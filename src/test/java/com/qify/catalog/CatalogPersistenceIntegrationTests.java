package com.qify.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import com.qify.catalog.domain.ServiceCategory;
import com.qify.catalog.domain.ServiceOffering;
import com.qify.catalog.domain.Venue;
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
class CatalogPersistenceIntegrationTests {

    private static final UUID VENUE_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID OFFERING_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID RETAIL_OFFERING_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID CUSTOMER_SERVICE_OFFERING_ID = UUID.fromString("50000000-0000-0000-0000-000000000003");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @Test
    void flywayCreatesCatalogTables() throws Exception {
        assertEquals(1, tableCount("venues"));
        assertEquals(1, tableCount("service_offerings"));
    }

    @Test
    void jpaReadsDeterministicSeedData() {
        Venue venue = entityManager.find(Venue.class, VENUE_ID);
        ServiceOffering offering = entityManager.find(ServiceOffering.class, OFFERING_ID);
        ServiceOffering retailOffering = entityManager.find(ServiceOffering.class, RETAIL_OFFERING_ID);
        ServiceOffering customerServiceOffering = entityManager.find(ServiceOffering.class, CUSTOMER_SERVICE_OFFERING_ID);

        assertNotNull(venue);
        assertEquals("Demo Central Market", venue.getName());
        assertNotNull(offering);
        assertEquals(VENUE_ID, offering.getVenue().getId());
        assertEquals(ServiceCategory.DINING, offering.getCategory());
        assertTrue(offering.isDelegationApproved());
        assertTrue(offering.isActive());
        assertNotNull(retailOffering);
        assertEquals(ServiceCategory.RETAIL, retailOffering.getCategory());
        assertFalse(retailOffering.isDelegationApproved());
        assertTrue(retailOffering.isActive());
        assertNotNull(customerServiceOffering);
        assertEquals(ServiceCategory.CUSTOMER_SERVICE, customerServiceOffering.getCategory());
        assertTrue(customerServiceOffering.isDelegationApproved());
        assertFalse(customerServiceOffering.isActive());
    }

    @Test
    void postgresRejectsMissingVenueAndInvalidRequiredValues() throws Exception {
        SQLException missingVenue = assertThrows(SQLException.class, () -> execute("INSERT INTO service_offerings (id, venue_id, category, delegation_approved, active) VALUES ('60000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000099', 'DINING', true, true)"));
        assertEquals("23503", missingVenue.getSQLState());

        SQLException blankVenue = assertThrows(SQLException.class, () -> execute("INSERT INTO venues (id, name) VALUES ('60000000-0000-0000-0000-000000000002', '   ')"));
        assertEquals("23514", blankVenue.getSQLState());

        SQLException invalidCategory = assertThrows(SQLException.class, () -> execute("INSERT INTO service_offerings (id, venue_id, category, delegation_approved, active) VALUES ('60000000-0000-0000-0000-000000000003', '" + VENUE_ID + "', 'INVALID', true, true)"));
        assertEquals("23514", invalidCategory.getSQLState());

        SQLException blankCategory = assertThrows(SQLException.class, () -> execute("INSERT INTO service_offerings (id, venue_id, category, delegation_approved, active) VALUES ('60000000-0000-0000-0000-000000000004', '" + VENUE_ID + "', '   ', true, true)"));
        assertEquals("23514", blankCategory.getSQLState());
    }

    @Test
    void postgresRejectsDuplicateCategoryAtVenue() throws Exception {
        SQLException duplicate = assertThrows(SQLException.class, () -> execute("INSERT INTO service_offerings (id, venue_id, category, delegation_approved, active) VALUES ('60000000-0000-0000-0000-000000000005', '" + VENUE_ID + "', 'DINING', true, true)"));
        assertEquals("23505", duplicate.getSQLState());
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
