package com.qify.fulfillment.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Import(QueueRequestListApiIntegrationTest.FixedClockConfiguration.class)
@Testcontainers
class QueueRequestListApiIntegrationTest {

    private static final String RUNNER_ID = "10000000-0000-0000-0000-000000000002";
    private static final String ADMIN_ID = "10000000-0000-0000-0000-000000000004";
    private static final String OFFERING_ID = "50000000-0000-0000-0000-000000000001";
    private static final String VENUE_ID = "40000000-0000-0000-0000-000000000001";
    private static final Instant NOW = Instant.parse("2026-09-06T02:00:00Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Test
    void ownerGetsOnlyOwnedRequestsWithStableOrderingAndExactFields() throws Exception {
        String owner = "10000000-0000-0000-0000-000000000101";
        String other = "10000000-0000-0000-0000-000000000102";
        insertCustomer(owner, "list-owner-101");
        insertCustomer(other, "list-other-102");
        insertRequest("20000000-0000-0000-0000-000000000003", owner, Instant.parse("2026-09-08T02:00:00Z"));
        insertRequest("20000000-0000-0000-0000-000000000001", owner, Instant.parse("2026-09-07T02:00:00Z"));
        insertRequest("20000000-0000-0000-0000-000000000002", owner, Instant.parse("2026-09-07T02:00:00Z"));
        insertRequest("20000000-0000-0000-0000-000000000004", other, Instant.parse("2026-09-09T02:00:00Z"));

        var databaseBefore = snapshot();
        mockMvc.perform(get("/api/v1/queue-requests").header("X-Actor-Id", owner))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].length()").value(10))
                .andExpect(jsonPath("$[1].length()").value(10))
                .andExpect(jsonPath("$[2].length()").value(10))
                .andExpect(jsonPath("$[0].id").value("20000000-0000-0000-0000-000000000003"))
                .andExpect(jsonPath("$[0].serviceOfferingId").value(OFFERING_ID))
                .andExpect(jsonPath("$[0].venueId").value(VENUE_ID))
                .andExpect(jsonPath("$[0].venueName").value("Demo Central Market"))
                .andExpect(jsonPath("$[0].serviceCategory").value("DINING"))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$[0].scheduledFor").value("2026-09-09T02:00:00Z"))
                .andExpect(jsonPath("$[0].expectedQueueMinutes").value(45))
                .andExpect(jsonPath("$[0].arrivalNoticeMinutes").value(15))
                .andExpect(jsonPath("$[0].createdAt").value("2026-09-08T02:00:00Z"))
                .andExpect(jsonPath("$[1].id").value("20000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$[2].id").value("20000000-0000-0000-0000-000000000002"));
        assertEquals(databaseBefore, snapshot());
    }

    @Test
    void customerWithNoRequestsGetsEmptyArrayWithoutRows() throws Exception {
        String owner = "10000000-0000-0000-0000-000000000103";
        insertCustomer(owner, "list-empty-103");
        var databaseBefore = snapshot();
        mockMvc.perform(get("/api/v1/queue-requests").header("X-Actor-Id", owner))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
        assertEquals(databaseBefore, snapshot());
    }

    @Test
    void runnerAndAdminAreForbiddenWithoutRows() throws Exception {
        for (String actor : new String[] { RUNNER_ID, ADMIN_ID }) {
            var databaseBefore = snapshot();
            mockMvc.perform(get("/api/v1/queue-requests").header("X-Actor-Id", actor))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("Only customers may view queue requests."));
            assertEquals(databaseBefore, snapshot());
        }
    }

    @Test
    void missingAndMalformedActorAreUnauthorizedWithoutRows() throws Exception {
        for (RequestCase requestCase : new RequestCase[] {
                new RequestCase(get("/api/v1/queue-requests"), "Actor identity is required."),
                new RequestCase(get("/api/v1/queue-requests").header("X-Actor-Id", "not-a-uuid"),
                        "Actor identity is invalid.") }) {
            var databaseBefore = snapshot();
            var result = mockMvc.perform(requestCase.request()).andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
            result.andExpect(jsonPath("$.detail").value(requestCase.detail()));
            assertEquals(databaseBefore, snapshot());
        }
    }

    private void insertCustomer(String id, String alias) {
        execute("INSERT INTO actors (id, alias, role) VALUES (?, ?, 'CUSTOMER') ON CONFLICT (id) DO NOTHING",
                statement -> {
                    statement.setObject(1, UUID.fromString(id));
                    statement.setString(2, alias);
                });
    }

    private void insertRequest(String id, String customerId, Instant createdAt) {
        execute("INSERT INTO queue_requests (id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at) VALUES (?, ?, ?, 'REQUESTED', ?, 45, 15, ?)",
                statement -> {
                    statement.setObject(1, UUID.fromString(id));
                    statement.setObject(2, UUID.fromString(customerId));
                    statement.setObject(3, UUID.fromString(OFFERING_ID));
                    statement.setTimestamp(4, Timestamp.from(createdAt.plusSeconds(86400)));
                    statement.setTimestamp(5, Timestamp.from(createdAt));
                });
        execute("INSERT INTO request_timeline (request_id, status, performed_by_actor_id, occurred_at) VALUES (?, 'REQUESTED', ?, ?)",
                statement -> {
                    statement.setObject(1, UUID.fromString(id));
                    statement.setObject(2, UUID.fromString(customerId));
                    statement.setTimestamp(3, Timestamp.from(createdAt));
                });
    }

    private void execute(String sql, SqlBinder binder) {
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private DatabaseSnapshot snapshot() {
        List<TableSnapshot> tables = new ArrayList<>();
        tables.add(snapshotTable("actors",
                "SELECT id, alias, role FROM actors ORDER BY id"));
        tables.add(snapshotTable("runner_profiles",
                "SELECT id, actor_id, verified, availability FROM runner_profiles ORDER BY id"));
        tables.add(snapshotTable("venues",
                "SELECT id, name FROM venues ORDER BY id"));
        tables.add(snapshotTable("service_offerings",
                "SELECT id, venue_id, category, delegation_approved, active FROM service_offerings ORDER BY id"));
        tables.add(snapshotTable("queue_requests",
                "SELECT id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at FROM queue_requests ORDER BY id"));
        tables.add(snapshotTable("request_timeline",
                "SELECT id, request_id, status, performed_by_actor_id, occurred_at FROM request_timeline ORDER BY id"));
        return new DatabaseSnapshot(List.copyOf(tables));
    }

    private TableSnapshot snapshotTable(String table, String sql) {
        List<List<String>> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement();
                var result = statement.executeQuery(sql)) {
            while (result.next()) {
                List<String> row = new ArrayList<>();
                for (int column = 1; column <= result.getMetaData().getColumnCount(); column++) {
                    row.add(String.valueOf(result.getObject(column)));
                }
                rows.add(List.copyOf(row));
            }
            return new TableSnapshot(table, List.copyOf(rows));
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private record TableSnapshot(String table, List<List<String>> rows) {
    }

    private record DatabaseSnapshot(List<TableSnapshot> tables) {
    }

    private record RequestCase(MockHttpServletRequestBuilder request, String detail) {
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
