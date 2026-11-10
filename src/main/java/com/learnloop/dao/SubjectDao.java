package com.learnloop.dao;

import com.learnloop.model.Subject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;

public final class SubjectDao {

    private static final String SELECT_COLUMNS = """
            SELECT id, code, name, description, is_active
            FROM subjects
            """;

    public Optional<Subject> findByIdForUpdate(Connection connection, long subjectId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_COLUMNS + "WHERE id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<Subject> findByCode(Connection connection, String code) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_COLUMNS + "WHERE code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<Subject> findByName(Connection connection, String name) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_COLUMNS + "WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public long insert(
            Connection connection,
            String code,
            String name,
            String description,
            boolean active) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                INSERT INTO subjects (code, name, description, is_active)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, code);
            statement.setString(2, name);
            statement.setString(3, description);
            statement.setBoolean(4, active);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("The database did not return a generated subject id.");
    }

    public void updateDetails(
            Connection connection,
            long subjectId,
            String code,
            String name,
            String description) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE subjects
                SET code = ?, name = ?, description = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            statement.setString(2, name);
            statement.setString(3, description);
            statement.setLong(4, subjectId);
            statement.executeUpdate();
        }
    }

    public void setActive(Connection connection, long subjectId, boolean active) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = "UPDATE subjects SET is_active = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, active);
            statement.setLong(2, subjectId);
            statement.executeUpdate();
        }
    }

    private static Subject map(ResultSet resultSet) throws SQLException {
        return new Subject(
                resultSet.getLong("id"),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getBoolean("is_active"));
    }
}
