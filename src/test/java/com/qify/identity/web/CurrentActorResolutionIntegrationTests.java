package com.qify.identity.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.qify.identity.application.CurrentActor;
import com.qify.identity.domain.ActorRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@org.springframework.context.annotation.Import(CurrentActorResolutionIntegrationTests.TestWebConfiguration.class)
class CurrentActorResolutionIntegrationTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Value("${spring.jpa.open-in-view}")
    private boolean openEntityManagerInView;

    @Test
    void knownActorResolvesDatabaseOwnedIdentity() throws Exception {
        mockMvc.perform(get("/test/current-actor")
                        .header("X-Actor-Id", CUSTOMER_ID)
                        .header("X-Actor-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.alias").value("demo-customer"))
                .andExpect(jsonPath("$.role").value(ActorRole.CUSTOMER.name()));
    }

    @Test
    void missingActorHeaderReturnsUnauthorizedProblemDetail() throws Exception {
        mockMvc.perform(get("/test/current-actor"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Actor identity is required."));
    }

    @Test
    void blankActorHeaderReturnsUnauthorizedProblemDetail() throws Exception {
        mockMvc.perform(get("/test/current-actor").header("X-Actor-Id", "  "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void malformedActorHeaderReturnsUnauthorizedProblemDetail() throws Exception {
        mockMvc.perform(get("/test/current-actor").header("X-Actor-Id", "not-a-uuid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Actor identity is invalid."));
    }

    @Test
    void shortenedUuidHeaderReturnsUnauthorizedProblemDetail() throws Exception {
        mockMvc.perform(get("/test/current-actor").header("X-Actor-Id", "10000000-0-0-0-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Actor identity is invalid."));
    }

    @Test
    void unknownActorHeaderReturnsUnauthorizedProblemDetail() throws Exception {
        mockMvc.perform(get("/test/current-actor").header("X-Actor-Id", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Actor identity is invalid."));
    }

    @Test
    void openEntityManagerInViewIsDisabled() {
        assertFalse(openEntityManagerInView);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestWebConfiguration {

        @Bean
        TestActorController testActorController() {
            return new TestActorController();
        }
    }

    @RestController
    static class TestActorController {

        @GetMapping("/test/current-actor")
        CurrentActor currentActor(CurrentActor actor) {
            return actor;
        }
    }
}
