package com.qify;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BackendRiskProbeIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:18.6-alpine";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE));

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServerProperties serverProperties;

    @Test
    void flywayMigratesRealPostgreSql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
        }

        assertEquals("backend risk probe",
                jdbcTemplate.queryForObject("select description from qify_probe where id = 1", String.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success", Integer.class));
    }

    @Test
    void hikariPoolUsesBoundedSettings() {
        HikariDataSource hikari = (HikariDataSource) dataSource;

        assertEquals(3, hikari.getMaximumPoolSize());
        assertEquals(5_000, hikari.getConnectionTimeout());
        assertEquals(3_000, hikari.getValidationTimeout());
    }

    @Test
    void healthAndReadinessAreUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void openApiDescribesTheProbe() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(containsString("3.")))
                .andExpect(jsonPath("$.paths['/api/probe']").exists())
                .andExpect(jsonPath("$.paths['/api/events/heartbeat']").exists());
    }

    @Test
    void probeKeepsServerSideSessionState() throws Exception {
        org.springframework.boot.web.server.Cookie sessionCookie = serverProperties.getServlet()
                .getSession()
                .getCookie();
        assertEquals(Boolean.TRUE, sessionCookie.getHttpOnly());
        assertEquals(Boolean.TRUE, sessionCookie.getSecure());
        assertEquals(SameSite.LAX, sessionCookie.getSameSite());

        mockMvc.perform(get("/api/probe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready"))
                .andExpect(jsonPath("$.*", hasSize(1)));

        MvcResult first = mockMvc.perform(get("/api/probe/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visits").value(1))
                .andExpect(request().sessionAttribute("qify.probe.visits", 1))
                .andReturn();
        MockHttpSession session = (MockHttpSession) first.getRequest().getSession(false);

        assertNotNull(session);
        mockMvc.perform(get("/api/probe/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visits").value(2))
                .andExpect(request().sessionAttribute("qify.probe.visits", 2));
    }

    @Test
    void csrfRejectsMissingHeaderAndAcceptsCookieTokenInHeader() throws Exception {
        MvcResult bootstrap = mockMvc.perform(get("/api/probe"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();
        Cookie tokenCookie = bootstrap.getResponse().getCookie("XSRF-TOKEN");

        assertNotNull(tokenCookie);
        assertFalse(tokenCookie.isHttpOnly());
        assertTrue(tokenCookie.getSecure());
        assertEquals("Lax", tokenCookie.getAttribute("SameSite"));

        mockMvc.perform(post("/api/probe").cookie(tokenCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/probe")
                        .cookie(tokenCookie)
                        .header("X-XSRF-TOKEN", tokenCookie.getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    void sseEndpointEmitsHeartbeat() throws Exception {
        MvcResult stream = mockMvc.perform(get("/api/events/heartbeat").accept("text/event-stream"))
                .andExpect(request().asyncStarted())
                .andReturn();

        assertEquals(200, stream.getResponse().getStatus());
        assertTrue(MediaType.TEXT_EVENT_STREAM.isCompatibleWith(
                MediaType.parseMediaType(stream.getResponse().getContentType())));
        assertTrue(stream.getResponse().getContentAsString().contains("event:heartbeat"));
        assertTrue(stream.getResponse().getContentAsString().contains("data:{\"status\":\"alive\"}"));
    }
}
