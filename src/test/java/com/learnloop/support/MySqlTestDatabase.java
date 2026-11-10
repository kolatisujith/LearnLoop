package com.learnloop.support;

import com.learnloop.config.DatabaseConfig;
import com.learnloop.dao.Database;
import com.learnloop.dao.DriverManagerConnectionProvider;
import org.junit.jupiter.api.Assumptions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public final class MySqlTestDatabase {

    private static final String URL = System.getenv("LEARNLOOP_TEST_DB_URL");
    private static final String USER = System.getenv("LEARNLOOP_TEST_DB_USER");
    private static final String PASSWORD = Objects.requireNonNullElse(System.getenv("LEARNLOOP_TEST_DB_PASSWORD"), "");

    private MySqlTestDatabase() {
    }

    public static Database database() {
        Assumptions.assumeTrue(URL != null && !URL.isBlank(),
                "Set LEARNLOOP_TEST_DB_URL and LEARNLOOP_TEST_DB_USER to run MySQL integration tests.");
        Assumptions.assumeTrue(USER != null && !USER.isBlank(),
                "Set LEARNLOOP_TEST_DB_USER to run MySQL integration tests.");
        return new Database(new DriverManagerConnectionProvider(
                new DatabaseConfig(URL, USER, PASSWORD, null)));
    }

    public static long queryLong(Database database, String sql, Object... parameters) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Query returned no rows: " + sql);
                    }
                    return resultSet.getLong(1);
                }
            }
        });
    }

    public static String queryString(Database database, String sql, Object... parameters) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Query returned no rows: " + sql);
                    }
                    return resultSet.getString(1);
                }
            }
        });
    }

    public static int execute(Database database, String sql, Object... parameters) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                return statement.executeUpdate();
            }
        });
    }

    public static void deleteClass(Database database, long classId) {
        execute(database, "DELETE FROM audit_logs WHERE entity_type = 'MATERIAL' AND entity_id IN "
                + "(SELECT id FROM class_materials WHERE class_id = ?)", classId);
        execute(database, "DELETE FROM audit_logs WHERE entity_type = 'CLASS' AND entity_id = ?", classId);
        execute(database, "DELETE FROM classes WHERE id = ?", classId);
    }

    private static void bind(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int index = 0; index < parameters.length; index++) {
            statement.setObject(index + 1, parameters[index]);
        }
    }
}
