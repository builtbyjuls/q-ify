package com.qify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class QifyApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void contextLoadsWithPostgreSql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();

            assertEquals("PostgreSQL", metadata.getDatabaseProductName());
            assertEquals(18, metadata.getDatabaseMajorVersion());
            assertTrue(entityManagerFactory.isOpen());

            try (var statement = connection.createStatement();
                    var resultSet = statement.executeQuery("""
                            SELECT COUNT(*)
                            FROM information_schema.tables
                            WHERE table_schema = 'public'
                              AND table_type = 'BASE TABLE'
                              AND table_name <> 'flyway_schema_history'
                            """)) {
                resultSet.next();
                assertEquals(2, resultSet.getInt(1));
            }
        }
    }
}
