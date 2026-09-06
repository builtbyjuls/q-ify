package com.qify.testsupport;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

public final class QifyDatabaseSnapshot {

    private static final List<TableQuery> TABLE_QUERIES = List.of(
            new TableQuery("actors", "SELECT id, alias, role FROM actors ORDER BY id"),
            new TableQuery("runner_profiles", "SELECT id, actor_id, verified, availability FROM runner_profiles ORDER BY id"),
            new TableQuery("venues", "SELECT id, name FROM venues ORDER BY id"),
            new TableQuery("service_offerings", "SELECT id, venue_id, category, delegation_approved, active FROM service_offerings ORDER BY id"),
            new TableQuery("queue_requests", "SELECT id, customer_id, service_offering_id, status, scheduled_for, expected_queue_minutes, arrival_notice_minutes, created_at FROM queue_requests ORDER BY id"),
            new TableQuery("request_timeline", "SELECT id, request_id, status, performed_by_actor_id, occurred_at FROM request_timeline ORDER BY id"));

    private final List<TableSnapshot> tables;

    private QifyDatabaseSnapshot(List<TableSnapshot> tables) {
        this.tables = List.copyOf(tables);
    }

    public static QifyDatabaseSnapshot capture(DataSource dataSource) {
        List<TableSnapshot> tables = new ArrayList<>();
        for (TableQuery tableQuery : TABLE_QUERIES) {
            tables.add(captureTable(dataSource, tableQuery));
        }
        return new QifyDatabaseSnapshot(tables);
    }

    private static TableSnapshot captureTable(DataSource dataSource, TableQuery tableQuery) {
        List<List<String>> rows = new ArrayList<>();
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
                var result = statement.executeQuery(tableQuery.sql())) {
            while (result.next()) {
                List<String> row = new ArrayList<>();
                for (int column = 1; column <= result.getMetaData().getColumnCount(); column++) {
                    row.add(String.valueOf(result.getObject(column)));
                }
                rows.add(List.copyOf(row));
            }
            return new TableSnapshot(tableQuery.table(), List.copyOf(rows));
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not capture Q-ify database snapshot.", exception);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof QifyDatabaseSnapshot snapshot && tables.equals(snapshot.tables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tables);
    }

    @Override
    public String toString() {
        return "QifyDatabaseSnapshot{tables=" + tables + '}';
    }

    private record TableQuery(String table, String sql) {
    }

    private record TableSnapshot(String table, List<List<String>> rows) {
    }
}
