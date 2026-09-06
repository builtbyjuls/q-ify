package com.qify.fulfillment.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

import com.qify.testsupport.QifyDatabaseSnapshot;

@SpringBootTest
@AutoConfigureMockMvc
@Import(QueueRequestDetailApiIntegrationTest.FixedClockConfiguration.class)
@Testcontainers
class QueueRequestDetailApiIntegrationTest {

    private static final String CUSTOMER_ID = "10000000-0000-0000-0000-000000000001";
    private static final String RUNNER_ID = "10000000-0000-0000-0000-000000000002";
    private static final String ADMIN_ID = "10000000-0000-0000-0000-000000000004";
    private static final String OTHER_CUSTOMER_ID = "10000000-0000-0000-0000-000000000099";
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
    void ownerGetsExactRequestDetailWithoutCreatingRows() throws Exception {
        String id = createRequest(CUSTOMER_ID);
        var databaseBefore = QifyDatabaseSnapshot.capture(dataSource);

        mockMvc.perform(get("/api/v1/queue-requests/" + id).header("X-Actor-Id", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.serviceOfferingId").value(OFFERING_ID))
                .andExpect(jsonPath("$.venueId").value(VENUE_ID))
                .andExpect(jsonPath("$.venueName").value("Demo Central Market"))
                .andExpect(jsonPath("$.serviceCategory").value("DINING"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.scheduledFor").value("2026-09-07T02:00:00Z"))
                .andExpect(jsonPath("$.expectedQueueMinutes").value(45))
                .andExpect(jsonPath("$.arrivalNoticeMinutes").value(15))
                .andExpect(jsonPath("$.createdAt").value(NOW.toString()));

        assertEquals(databaseBefore, QifyDatabaseSnapshot.capture(dataSource));
    }

    @Test
    void nonexistentRequestReturnsSafeNotFoundWithoutRows() throws Exception {
        var databaseBefore = QifyDatabaseSnapshot.capture(dataSource);
        mockMvc.perform(get("/api/v1/queue-requests/" + UUID.randomUUID()).header("X-Actor-Id", CUSTOMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Queue request was not found."));
        assertEquals(databaseBefore, QifyDatabaseSnapshot.capture(dataSource));
    }

    @Test
    void otherCustomersRequestReturnsIdenticalNotFound() throws Exception {
        insertOtherCustomer();
        String id = createRequest(OTHER_CUSTOMER_ID);
        var databaseBefore = QifyDatabaseSnapshot.capture(dataSource);
        mockMvc.perform(get("/api/v1/queue-requests/" + id).header("X-Actor-Id", CUSTOMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Queue request was not found."));
        assertEquals(databaseBefore, QifyDatabaseSnapshot.capture(dataSource));
    }

    @Test
    void runnersAndAdminsAreForbiddenWithoutRows() throws Exception {
        String id = createRequest(CUSTOMER_ID);
        for (String actor : new String[] { RUNNER_ID, ADMIN_ID }) {
            var databaseBefore = QifyDatabaseSnapshot.capture(dataSource);
            mockMvc.perform(get("/api/v1/queue-requests/" + id).header("X-Actor-Id", actor))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("Only customers may view queue requests."));
            assertEquals(databaseBefore, QifyDatabaseSnapshot.capture(dataSource));
        }
    }

    @Test
    void missingAndMalformedActorAreUnauthorizedWithoutRows() throws Exception {
        String id = UUID.randomUUID().toString();
        var databaseBefore = QifyDatabaseSnapshot.capture(dataSource);
        mockMvc.perform(get("/api/v1/queue-requests/" + id))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Actor identity is required."));
        assertEquals(databaseBefore, QifyDatabaseSnapshot.capture(dataSource));

        databaseBefore = QifyDatabaseSnapshot.capture(dataSource);
        mockMvc.perform(get("/api/v1/queue-requests/" + id).header("X-Actor-Id", "not-a-uuid"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Actor identity is invalid."));
        assertEquals(databaseBefore, QifyDatabaseSnapshot.capture(dataSource));
    }

    private String createRequest(String actorId) throws Exception {
        String body = """
                {"serviceOfferingId":"%s","scheduledFor":"2026-09-07T10:00:00+08:00","expectedQueueMinutes":45,"arrivalNoticeMinutes":15}
                """.formatted(OFFERING_ID);
        String response = mockMvc.perform(post("/api/v1/queue-requests").header("X-Actor-Id", actorId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private void insertOtherCustomer() {
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "INSERT INTO actors (id, alias, role) VALUES (?, ?, 'CUSTOMER') ON CONFLICT (id) DO NOTHING")) {
            statement.setObject(1, UUID.fromString(OTHER_CUSTOMER_ID));
            statement.setString(2, "detail-test-customer");
            statement.executeUpdate();
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
