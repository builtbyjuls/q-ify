package com.qify.fulfillment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import com.qify.fulfillment.domain.QueueRequest;
import com.qify.fulfillment.domain.QueueRequestStatus;
import com.qify.fulfillment.domain.RequestTimelineEntry;
import com.qify.fulfillment.persistence.QueueRequestRepository;
import com.qify.fulfillment.persistence.RequestTimelineEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
@Import(QueueRequestCreationIntegrationTest.FixedClockConfiguration.class)
class QueueRequestCreationIntegrationTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OFFERING_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-06T02:00:00Z");
    private static final Instant SCHEDULED_FOR = Instant.parse("2026-09-07T02:00:00Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine");

    @Autowired
    private QueueRequestCreationService service;

    @Autowired
    private QueueRequestRepository queueRequestRepository;

    @Autowired
    private RequestTimelineEntryRepository requestTimelineEntryRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void createsRequestAndInitialTimelineEntryWithOneTimestamp() {
        UUID requestId = service.create(CUSTOMER_ID, OFFERING_ID, SCHEDULED_FOR, 45, 15);

        QueueRequest request = queueRequestRepository.findById(requestId).orElseThrow();
        RequestTimelineEntry timeline = requestTimelineEntryRepository.findAll().stream()
                .filter(entry -> entry.getRequest().getId().equals(requestId))
                .findFirst().orElseThrow();

        assertNotNull(requestId);
        assertEquals(requestId, request.getId());
        assertEquals(CUSTOMER_ID, request.getCustomer().getId());
        assertEquals(OFFERING_ID, request.getServiceOffering().getId());
        assertEquals(QueueRequestStatus.REQUESTED, request.getStatus());
        assertEquals(SCHEDULED_FOR, request.getScheduledFor());
        assertEquals(45, request.getExpectedQueueMinutes());
        assertEquals(15, request.getArrivalNoticeMinutes());
        assertEquals(NOW, request.getCreatedAt());
        assertEquals(QueueRequestStatus.REQUESTED, timeline.getStatus());
        assertEquals(CUSTOMER_ID, timeline.getPerformedByActor().getId());
        assertEquals(NOW, timeline.getOccurredAt());
        assertEquals(1, count("queue_requests", requestId));
        assertEquals(1, countByColumn("request_timeline", "request_id", requestId));
    }

    @Test
    void missingCustomerPersistsNeitherRow() {
        UUID missingCustomerId = UUID.fromString("10000000-0000-0000-0000-000000000099");
        int requestsBefore = countAllRows("queue_requests");
        int timelineBefore = countAllRows("request_timeline");

        assertThrows(IllegalArgumentException.class,
                () -> service.create(missingCustomerId, OFFERING_ID, SCHEDULED_FOR, 45, 15));

        assertEquals(requestsBefore, countAllRows("queue_requests"));
        assertEquals(timelineBefore, countAllRows("request_timeline"));
    }

    @Test
    void missingOfferingPersistsNeitherRow() {
        UUID missingOfferingId = UUID.fromString("50000000-0000-0000-0000-000000000099");
        int requestsBefore = countAllRows("queue_requests");
        int timelineBefore = countAllRows("request_timeline");

        assertThrows(IllegalArgumentException.class,
                () -> service.create(CUSTOMER_ID, missingOfferingId, SCHEDULED_FOR, 45, 15));

        assertEquals(requestsBefore, countAllRows("queue_requests"));
        assertEquals(timelineBefore, countAllRows("request_timeline"));
    }

    @Test
    void timelineInsertFailureRollsBackRequestInsert() throws Exception {
        int requestsBefore = countAllRows("queue_requests");
        int timelineBefore = countAllRows("request_timeline");
        installTimelineFailureTrigger();
        RuntimeException failure;
        try {
            failure = assertThrows(RuntimeException.class,
                    () -> service.create(CUSTOMER_ID, OFFERING_ID, SCHEDULED_FOR, 45, 15));
        } finally {
            dropTimelineFailureTrigger();
        }

        String postgresMessage = postgresMessage(failure);
        String requestMessagePrefix = "forced timeline insert failure for request ";
        assertNotNull(postgresMessage);
        UUID requestId = UUID.fromString(postgresMessage.substring(requestMessagePrefix.length()));
        assertEquals(requestMessagePrefix + requestId, postgresMessage);
        assertEquals(requestsBefore, countAllRows("queue_requests"));
        assertEquals(timelineBefore, countAllRows("request_timeline"));
        assertEquals(0, count("queue_requests", requestId));
        assertEquals(0, countByColumn("request_timeline", "request_id", requestId));
    }

    private int count(String table, UUID requestId) {
        return countByColumn(table, "id", requestId);
    }

    private int countByColumn(String table, String column, UUID value) {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private int countAllRows(String table) {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement();
                var resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void installTimelineFailureTrigger() throws SQLException {
        execute("CREATE OR REPLACE FUNCTION fail_request_timeline_insert() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF EXISTS (SELECT 1 FROM queue_requests WHERE id = NEW.request_id) THEN RAISE EXCEPTION 'forced timeline insert failure for request %', NEW.request_id; ELSE RAISE EXCEPTION 'request not visible during timeline trigger for request %', NEW.request_id; END IF; END; $$");
        execute("CREATE TRIGGER fail_request_timeline_insert BEFORE INSERT ON request_timeline FOR EACH ROW EXECUTE FUNCTION fail_request_timeline_insert()");
    }

    private String postgresMessage(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null) {
                for (String line : message.split("\\R")) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("ERROR: ")) {
                        return trimmed.substring("ERROR: ".length());
                    }
                }
            }
        }
        return null;
    }

    private void dropTimelineFailureTrigger() throws SQLException {
        execute("DROP TRIGGER IF EXISTS fail_request_timeline_insert ON request_timeline");
        execute("DROP FUNCTION IF EXISTS fail_request_timeline_insert()");
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
