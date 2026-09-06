package com.qify.catalog.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ServiceOfferingApiIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsOnlyActiveApprovedOfferingsWithoutIdentityHeader() throws Exception {
        mockMvc.perform(get("/api/v1/service-offerings"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("50000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$[0].venueId").value("40000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$[0].venueName").value("Demo Central Market"))
                .andExpect(jsonPath("$[0].category").value("DINING"))
                .andExpect(jsonPath("$[1]").doesNotExist())
                .andExpect(jsonPath("$[*].active").doesNotExist())
                .andExpect(jsonPath("$[*].delegationApproved").doesNotExist())
                .andExpect(jsonPath("$[*].venue").doesNotExist())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("50000000-0000-0000-0000-000000000002"))))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("50000000-0000-0000-0000-000000000003"))));
    }
}
