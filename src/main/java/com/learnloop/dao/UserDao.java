package com.learnloop.dao;

import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.NewUser;
import com.learnloop.model.UserAccount;
import com.learnloop.model.UserCredentials;
import com.learnloop.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class UserDao {

    private static final String SELECT_ACCOUNT_COLUMNS = """
            SELECT id, email, full_name, role, approval_status
            FROM users
            """;

    private static final String SELECT_CREDENTIAL_COLUMNS = """
            SELECT id, email, full_name, role, approval_status, password_hash
            FROM users
            """;

    public long insert(Connection connection, NewUser newUser) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(newUser, "newUser");
        String sql = """
                INSERT INTO users (
                    email, password_hash, full_name, role, approval_status,
                    approved_at, approved_by_user_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, newUser.email());
            statement.setString(2, newUser.passwordHash());
            statement.setString(3, newUser.fullName());
            statement.setString(4, newUser.role().name());
            statement.setString(5, newUser.approvalStatus().name());
            JdbcSupport.setInstant(statement, 6, newUser.approvedAt());
            JdbcSupport.setNullableLong(statement, 7, newUser.approvedByUserId());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("The database did not return a generated user id.");
    }

    public Optional<UserAccount> findByIdForUpdate(Connection connection, long userId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_ACCOUNT_COLUMNS + "WHERE id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapAccount(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<UserAccount> findById(Connection connection, long userId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_ACCOUNT_COLUMNS + "WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapAccount(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<UserAccount> findByEmail(Connection connection, String email) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_ACCOUNT_COLUMNS + "WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapAccount(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<UserCredentials> findCredentialsByEmail(Connection connection, String email) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_CREDENTIAL_COLUMNS + "WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapCredentials(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<UserAccount> findFirstAdminForUpdate(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_ACCOUNT_COLUMNS + """
                WHERE role = 'ADMIN'
                ORDER BY id
                LIMIT 1
                FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? Optional.of(mapAccount(resultSet)) : Optional.empty();
        }
    }

    public void markApproved(
            Connection connection,
            long userId,
            long approvedByUserId,
            Instant approvedAt) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE users
                SET approval_status = 'APPROVED',
                    approved_at = ?,
                    approved_by_user_id = ?,
                    rejection_reason = NULL
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setInstant(statement, 1, approvedAt);
            statement.setLong(2, approvedByUserId);
            statement.setLong(3, userId);
            statement.executeUpdate();
        }
    }

    public void markRejected(Connection connection, long userId, String rejectionReason) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE users
                SET approval_status = 'REJECTED',
                    approved_at = NULL,
                    approved_by_user_id = NULL,
                    rejection_reason = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, rejectionReason);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    private static UserAccount mapAccount(ResultSet resultSet) throws SQLException {
        return new UserAccount(
                resultSet.getLong("id"),
                resultSet.getString("email"),
                resultSet.getString("full_name"),
                UserRole.fromDatabase(resultSet.getString("role")),
                ApprovalStatus.fromDatabase(resultSet.getString("approval_status")));
    }

    private static UserCredentials mapCredentials(ResultSet resultSet) throws SQLException {
        return new UserCredentials(
                resultSet.getLong("id"),
                resultSet.getString("email"),
                resultSet.getString("full_name"),
                UserRole.fromDatabase(resultSet.getString("role")),
                ApprovalStatus.fromDatabase(resultSet.getString("approval_status")),
                resultSet.getString("password_hash"));
    }
}
