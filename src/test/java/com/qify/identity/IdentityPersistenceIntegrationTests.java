package com.qify.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import com.qify.identity.domain.Actor;
import com.qify.identity.domain.ActorRole;
import com.qify.identity.domain.RunnerAvailability;
import com.qify.identity.domain.RunnerProfile;
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
class IdentityPersistenceIntegrationTests {

    private static final UUID AVAILABLE_RUNNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID AVAILABLE_PROFILE_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID UNAVAILABLE_RUNNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @Test
    void flywayCreatesIdentityTables() throws Exception {
        assertEquals(1, tableCount("actors"));
        assertEquals(1, tableCount("runner_profiles"));
    }

    @Test
    void jpaReadsDeterministicSeedData() {
        Actor actor = entityManager.find(Actor.class, AVAILABLE_RUNNER_ID);
        RunnerProfile profile = entityManager.find(RunnerProfile.class, AVAILABLE_PROFILE_ID);

        assertNotNull(actor);
        assertEquals("demo-runner-available", actor.getAlias());
        assertEquals(ActorRole.RUNNER, actor.getRole());
        assertNotNull(profile);
        assertEquals(RunnerAvailability.AVAILABLE, profile.getAvailability());
        assertEquals(true, profile.isVerified());
    }

    @Test
    void postgresRejectsInvalidRoleAndAvailability() throws Exception {
        SQLException invalidRole = assertThrows(SQLException.class, () -> execute("INSERT INTO actors (id, alias, role) VALUES ('20000000-0000-0000-0000-000000000001', 'invalid-role', 'NOT_A_ROLE')"));
        assertEquals("23514", invalidRole.getSQLState());

        execute("INSERT INTO actors (id, alias, role) VALUES ('20000000-0000-0000-0000-000000000003', 'constraint-runner', 'RUNNER')");
        SQLException invalidAvailability = assertThrows(SQLException.class, () -> execute("INSERT INTO runner_profiles (id, actor_id, verified, availability) VALUES ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', false, 'NOT_AVAILABLE')"));
        assertEquals("23514", invalidAvailability.getSQLState());
    }

    @Test
    void postgresEnforcesAliasUniquenessAndOneProfilePerRunner() throws Exception {
        SQLException duplicateAlias = assertThrows(SQLException.class, () -> execute("INSERT INTO actors (id, alias, role) VALUES ('20000000-0000-0000-0000-000000000002', 'demo-runner-available', 'CUSTOMER')"));
        assertEquals("23505", duplicateAlias.getSQLState());

        SQLException duplicateProfile = assertThrows(SQLException.class, () -> execute("INSERT INTO runner_profiles (id, actor_id, verified, availability) VALUES ('30000000-0000-0000-0000-000000000004', '" + AVAILABLE_RUNNER_ID + "', false, 'AVAILABLE')"));
        assertEquals("23505", duplicateProfile.getSQLState());
    }

    private int tableCount(String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?")) {
            statement.setString(1, tableName);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
