package com.qify.identity.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qify.identity.domain.ActorRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DemoActorApiIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsSeededActorsInStableAliasOrderWithoutIdentityHeader() throws Exception {
        mockMvc.perform(get("/api/v1/demo/actors"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("10000000-0000-0000-0000-000000000004"))
                .andExpect(jsonPath("$[0].alias").value("demo-admin"))
                .andExpect(jsonPath("$[0].role").value(ActorRole.ADMIN.name()))
                .andExpect(jsonPath("$[1].id").value("10000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$[1].alias").value("demo-customer"))
                .andExpect(jsonPath("$[1].role").value(ActorRole.CUSTOMER.name()))
                .andExpect(jsonPath("$[2].id").value("10000000-0000-0000-0000-000000000002"))
                .andExpect(jsonPath("$[2].alias").value("demo-runner-available"))
                .andExpect(jsonPath("$[2].role").value(ActorRole.RUNNER.name()))
                .andExpect(jsonPath("$[3].id").value("10000000-0000-0000-0000-000000000003"))
                .andExpect(jsonPath("$[3].alias").value("demo-runner-unavailable"))
                .andExpect(jsonPath("$[3].role").value(ActorRole.RUNNER.name()))
                .andExpect(jsonPath("$[4]").doesNotExist())
                .andExpect(jsonPath("$[*].runnerProfile").doesNotExist())
                .andExpect(jsonPath("$[*].availability").doesNotExist())
                .andExpect(jsonPath("$[*].verified").doesNotExist());
    }
}
