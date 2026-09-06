package com.qify.fulfillment.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Import(QueueRequestCreationApiIntegrationTest.FixedClockConfiguration.class)
@Testcontainers
class QueueRequestCreationApiIntegrationTest {

    private static final String CUSTOMER_ID = "10000000-0000-0000-0000-000000000001";
    private static final String RUNNER_ID = "10000000-0000-0000-0000-000000000002";
    private static final String ADMIN_ID = "10000000-0000-0000-0000-000000000004";
    private static final String OFFERING_ID = "50000000-0000-0000-0000-000000000001";
    private static final Instant NOW = Instant.parse("2026-09-06T02:00:00Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Test
    void customerCreatesRequestAndTimelineEntry() throws Exception {
        int requestsBefore = count("queue_requests");
        int timelineBefore = count("request_timeline");
        String body = """
                {"serviceOfferingId":"%s","scheduledFor":"2026-09-07T10:00:00+08:00","expectedQueueMinutes":45,"arrivalNoticeMinutes":15,"customerId":"%s"}
                """.formatted(OFFERING_ID, RUNNER_ID);

        var result = mockMvc.perform(post("/api/v1/queue-requests")
                        .header("X-Actor-Id", CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();
        String id = result.getResponse().getContentAsString().replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        String location = result.getResponse().getHeader("Location");
        org.junit.jupiter.api.Assertions.assertEquals("/api/v1/queue-requests/" + id, location);
        org.junit.jupiter.api.Assertions.assertEquals(requestsBefore + 1, count("queue_requests"));
        org.junit.jupiter.api.Assertions.assertEquals(timelineBefore + 1, count("request_timeline"));
        org.junit.jupiter.api.Assertions.assertEquals(1, countById("queue_requests", id));
        org.junit.jupiter.api.Assertions.assertEquals(UUID.fromString(CUSTOMER_ID),
                uuidValue("queue_requests", "customer_id", id));
        org.junit.jupiter.api.Assertions.assertEquals(1, countByColumn("request_timeline", "request_id", id));
        org.junit.jupiter.api.Assertions.assertEquals(Instant.parse("2026-09-07T02:00:00Z"),
                instant("queue_requests", "scheduled_for", id));
        org.junit.jupiter.api.Assertions.assertEquals(NOW, instant("queue_requests", "created_at", id));
        org.junit.jupiter.api.Assertions.assertEquals(NOW, instant("request_timeline", "occurred_at", id));
    }

    @Test
    void validationAndMalformedTimestampReturnBadRequestWithoutRows() throws Exception {
        int requestsBefore = count("queue_requests");
        int timelineBefore = count("request_timeline");
        performCustomerPost("{\"scheduledFor\":\"2026-09-07T10:00:00+08:00\",\"expectedQueueMinutes\":45,\"arrivalNoticeMinutes\":15}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Request validation failed."));
        performCustomerPost("{" + "\"serviceOfferingId\":\"" + OFFERING_ID + "\",\"scheduledFor\":\"2026-09-07T10:00:00+08:00\",\"expectedQueueMinutes\":721,\"arrivalNoticeMinutes\":15}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Request validation failed."));
        performCustomerPost("{\"serviceOfferingId\":\"" + OFFERING_ID + "\",\"scheduledFor\":\"not-a-time\",\"expectedQueueMinutes\":45,\"arrivalNoticeMinutes\":15}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Request body is invalid."));
        org.junit.jupiter.api.Assertions.assertEquals(requestsBefore, count("queue_requests"));
        org.junit.jupiter.api.Assertions.assertEquals(timelineBefore, count("request_timeline"));
    }

    @Test
    void missingAndMalformedIdentityReturnUnauthorized() throws Exception {
        int requestsBefore = count("queue_requests");
        int timelineBefore = count("request_timeline");
        mockMvc.perform(post("/api/v1/queue-requests").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Actor identity is required."));
        mockMvc.perform(post("/api/v1/queue-requests").header("X-Actor-Id", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Actor identity is invalid."));
        org.junit.jupiter.api.Assertions.assertEquals(requestsBefore, count("queue_requests"));
        org.junit.jupiter.api.Assertions.assertEquals(timelineBefore, count("request_timeline"));
    }

    @Test
    void nonCustomersAreForbiddenWithoutRows() throws Exception {
        int requestsBefore = count("queue_requests");
        int timelineBefore = count("request_timeline");
        String body = "{\"serviceOfferingId\":\"" + OFFERING_ID + "\",\"scheduledFor\":\"2026-09-07T10:00:00+08:00\",\"expectedQueueMinutes\":45,\"arrivalNoticeMinutes\":15}";
        for (String actor : new String[] { RUNNER_ID, ADMIN_ID }) {
            mockMvc.perform(post("/api/v1/queue-requests").header("X-Actor-Id", actor)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("Only customers may create queue requests."));
        }
        org.junit.jupiter.api.Assertions.assertEquals(requestsBefore, count("queue_requests"));
        org.junit.jupiter.api.Assertions.assertEquals(timelineBefore, count("request_timeline"));
    }

    @Test
    void unknownOfferingReturnsNotFound() throws Exception {
        int requestsBefore = count("queue_requests");
        int timelineBefore = count("request_timeline");
        performCustomerPost("{\"serviceOfferingId\":\"50000000-0000-0000-0000-000000000099\",\"scheduledFor\":\"2026-09-07T10:00:00+08:00\",\"expectedQueueMinutes\":45,\"arrivalNoticeMinutes\":15}")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Service offering was not found."));
        org.junit.jupiter.api.Assertions.assertEquals(requestsBefore, count("queue_requests"));
        org.junit.jupiter.api.Assertions.assertEquals(timelineBefore, count("request_timeline"));
    }

    @Test
    void inactiveAndUnapprovedOfferingsReturnConflict() throws Exception {
        int requestsBefore = count("queue_requests");
        int timelineBefore = count("request_timeline");
        for (String offering : new String[] { "50000000-0000-0000-0000-000000000002", "50000000-0000-0000-0000-000000000003" }) {
            performCustomerPost("{\"serviceOfferingId\":\"" + offering + "\",\"scheduledFor\":\"2026-09-07T10:00:00+08:00\",\"expectedQueueMinutes\":45,\"arrivalNoticeMinutes\":15}")
                    .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").isString());
        }
        org.junit.jupiter.api.Assertions.assertEquals(requestsBefore, count("queue_requests"));
        org.junit.jupiter.api.Assertions.assertEquals(timelineBefore, count("request_timeline"));
    }

    private org.springframework.test.web.servlet.ResultActions performCustomerPost(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/queue-requests").header("X-Actor-Id", CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private int count(String table) {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getInt(1);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private int countById(String table, String id) {
        return countByColumn(table, "id", id);
    }

    private int countByColumn(String table, String column, String value) {
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?")) {
            statement.setObject(1, UUID.fromString(value));
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private UUID uuidValue(String table, String column, String id) {
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "SELECT " + column + " FROM " + table + " WHERE id = ?")) {
            statement.setObject(1, UUID.fromString(id));
            try (var result = statement.executeQuery()) {
                result.next();
                return (UUID) result.getObject(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Instant instant(String table, String column, String id) {
        String condition = table.equals("request_timeline")
                ? "request_id = (SELECT id FROM queue_requests WHERE id = ?)"
                : "id = ?";
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "SELECT " + column + " FROM " + table + " WHERE " + condition)) {
            statement.setObject(1, UUID.fromString(id));
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getTimestamp(1).toInstant();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
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
