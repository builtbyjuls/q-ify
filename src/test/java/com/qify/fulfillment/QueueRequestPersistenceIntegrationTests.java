package com.qify.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import javax.sql.DataSource;

import com.qify.fulfillment.domain.QueueRequest;
import com.qify.fulfillment.domain.QueueRequestStatus;
import com.qify.fulfillment.domain.RequestTimelineEntry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class QueueRequestPersistenceIntegrationTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OFFERING_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @Test
    void flywayCreatesQueueRequestTables() throws Exception {
        assertEquals(1, tableCount("queue_requests"));
        assertEquals(1, tableCount("request_timeline"));
    }

    @Test
    void jpaReadsRequestAndTimelineRows() throws Exception {
        UUID requestId = UUID.fromString("70000000-0000-0000-0000-000000000001");
        UUID performerId = UUID.fromString("10000000-0000-0000-0000-000000000004");
        Instant scheduledFor = Instant.parse("2026-09-07T02:00:00Z");
        Instant createdAt = Instant.parse("2026-09-06T02:00:00Z");
        Instant occurredAt = Instant.parse("2026-09-06T02:30:00Z");
        insert("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES ('" + requestId + "', '" + CUSTOMER_ID + "', '" + OFFERING_ID + "', 'REQUESTED', '" + scheduledFor + "', 45, 15, '" + createdAt + "')");
        insert("INSERT INTO request_timeline (request_id, status, performed_by_actor_id, occurred_at) VALUES ('" + requestId + "', 'REQUESTED', '" + performerId + "', '" + occurredAt + "')");

        QueueRequest request = entityManager.find(QueueRequest.class, requestId);
        RequestTimelineEntry timeline = entityManager.createQuery("select t from RequestTimelineEntry t where t.request.id = :requestId", RequestTimelineEntry.class)
                .setParameter("requestId", requestId)
                .getSingleResult();

        assertNotNull(request);
        assertEquals(CUSTOMER_ID, request.getCustomer().getId());
        assertEquals(OFFERING_ID, request.getServiceOffering().getId());
        assertEquals(QueueRequestStatus.REQUESTED, request.getStatus());
        assertEquals(scheduledFor, request.getScheduledFor());
        assertEquals(45, request.getExpectedQueueMinutes());
        assertEquals(15, request.getArrivalNoticeMinutes());
        assertEquals(createdAt, request.getCreatedAt());
        assertNotNull(timeline);
        assertEquals(requestId, timeline.getRequest().getId());
        assertEquals(QueueRequestStatus.REQUESTED, timeline.getStatus());
        assertEquals(performerId, timeline.getPerformedByActor().getId());
        assertEquals(occurredAt, timeline.getOccurredAt());
    }

    @Test
    void postgresRejectsInvalidStatuses() throws Exception {
        UUID requestId = UUID.fromString("70000000-0000-0000-0000-000000000002");
        SQLException requestStatus = assertThrows(SQLException.class, () -> insert("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES ('" + requestId + "', '" + CUSTOMER_ID + "', '" + OFFERING_ID + "', 'INVALID', '2026-09-07T02:00:00Z', 1, 0, '2026-09-06T02:00:00Z')"));
        assertEquals("23514", requestStatus.getSQLState());

        UUID timelineRequestId = UUID.fromString("70000000-0000-0000-0000-000000000013");
        insert("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES ('" + timelineRequestId + "', '" + CUSTOMER_ID + "', '" + OFFERING_ID + "', 'REQUESTED', '2026-09-07T02:00:00Z', 1, 0, '2026-09-06T02:00:00Z')");
        SQLException timelineStatus = assertThrows(SQLException.class, () -> insert("INSERT INTO request_timeline (request_id, status, performed_by_actor_id, occurred_at) VALUES ('" + timelineRequestId + "', 'INVALID', '" + CUSTOMER_ID + "', '2026-09-06T02:00:00Z')"));
        assertEquals("23514", timelineStatus.getSQLState());
    }

    @Test
    void postgresEnforcesQueueAndNoticeBounds() throws Exception {
        UUID requestId = UUID.fromString("70000000-0000-0000-0000-000000000003");
        insert("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES ('" + requestId + "', '" + CUSTOMER_ID + "', '" + OFFERING_ID + "', 'REQUESTED', '2026-09-07T02:00:00Z', 1, 0, '2026-09-06T02:00:00Z')");
        UUID upperRequestId = UUID.fromString("70000000-0000-0000-0000-000000000004");
        insert("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES ('" + upperRequestId + "', '" + CUSTOMER_ID + "', '" + OFFERING_ID + "', 'REQUESTED', '2026-09-07T02:00:00Z', 720, 120, '2026-09-06T02:00:00Z')");

        SQLException lowQueue = assertThrows(SQLException.class, () -> insert("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES ('70000000-0000-0000-0000-000000000005', '" + CUSTOMER_ID + "', '" + OFFERING_ID + "', 'REQUESTED', '2026-09-07T02:00:00Z', 0, 0, '2026-09-06T02:00:00Z')"));
        assertEquals("23514", lowQueue.getSQLState());
        SQLException highQueue = assertThrows(SQLException.class, () -> insert("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES ('70000000-0000-0000-0000-000000000006', '" + CUSTOMER_ID + "', '" + OFFERING_ID + "', 'REQUESTED', '2026-09-07T02:00:00Z', 721, 0, '2026-09-06T02:00:00Z')"));
        assertEquals("23514", highQueue.getSQLState());
        SQLException lowNotice = assertThrows(SQLException.class, () -> insert("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES ('70000000-0000-0000-0000-000000000007', '" + CUSTOMER_ID + "', '" + OFFERING_ID + "', 'REQUESTED', '2026-09-07T02:00:00Z', 1, -1, '2026-09-06T02:00:00Z')"));
        assertEquals("23514", lowNotice.getSQLState());
        SQLException highNotice = assertThrows(SQLException.class, () -> insert("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES ('70000000-0000-0000-0000-000000000008', '" + CUSTOMER_ID + "', '" + OFFERING_ID + "', 'REQUESTED', '2026-09-07T02:00:00Z', 1, 121, '2026-09-06T02:00:00Z')"));
        assertEquals("23514", highNotice.getSQLState());
    }

    @Test
    void postgresRejectsMissingReferences() throws Exception {
        SQLException customer = assertThrows(SQLException.class, () -> insert("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES ('70000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000099', '" + OFFERING_ID + "', 'REQUESTED', '2026-09-07T02:00:00Z', 1, 0, '2026-09-06T02:00:00Z')"));
        assertEquals("23503", customer.getSQLState());
        SQLException offering = assertThrows(SQLException.class, () -> insert("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES ('70000000-0000-0000-0000-000000000010', '" + CUSTOMER_ID + "', '50000000-0000-0000-0000-000000000099', 'REQUESTED', '2026-09-07T02:00:00Z', 1, 0, '2026-09-06T02:00:00Z')"));
        assertEquals("23503", offering.getSQLState());
        SQLException request = assertThrows(SQLException.class, () -> insert("INSERT INTO request_timeline (request_id, status, performed_by_actor_id, occurred_at) VALUES ('70000000-0000-0000-0000-000000000099', 'REQUESTED', '" + CUSTOMER_ID + "', '2026-09-06T02:00:00Z')"));
        assertEquals("23503", request.getSQLState());
        UUID requestId = UUID.fromString("70000000-0000-0000-0000-000000000011");
        insert("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES ('" + requestId + "', '" + CUSTOMER_ID + "', '" + OFFERING_ID + "', 'REQUESTED', '2026-09-07T02:00:00Z', 1, 0, '2026-09-06T02:00:00Z')");
        SQLException performer = assertThrows(SQLException.class, () -> insert("INSERT INTO request_timeline (request_id, status, performed_by_actor_id, occurred_at) VALUES ('" + requestId + "', 'REQUESTED', '10000000-0000-0000-0000-000000000099', '2026-09-06T02:00:00Z')"));
        assertEquals("23503", performer.getSQLState());
    }

    @Test
    void timelineIdentityIdsIncreaseForEqualOccurrenceTimes() throws Exception {
        UUID requestId = UUID.fromString("70000000-0000-0000-0000-000000000012");
        Instant occurredAt = Instant.parse("2026-09-06T03:00:00Z");
        insert("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES ('" + requestId + "', '" + CUSTOMER_ID + "', '" + OFFERING_ID + "', 'REQUESTED', '2026-09-07T02:00:00Z', 1, 0, '2026-09-06T02:00:00Z')");
        long firstId = insertReturningId("INSERT INTO request_timeline (request_id, status, performed_by_actor_id, occurred_at) VALUES ('" + requestId + "', 'REQUESTED', '" + CUSTOMER_ID + "', '" + occurredAt + "') RETURNING id");
        long secondId = insertReturningId("INSERT INTO request_timeline (request_id, status, performed_by_actor_id, occurred_at) VALUES ('" + requestId + "', 'REQUESTED', '" + CUSTOMER_ID + "', '" + occurredAt + "') RETURNING id");
        assertTrue(secondId > firstId);
    }

    private int tableCount(String tableName) throws SQLException {
        return (int) queryLongs("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '" + tableName + "'");
    }

    private void insert(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long insertReturningId(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement(); var resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private long queryLongs(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement(); var resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
