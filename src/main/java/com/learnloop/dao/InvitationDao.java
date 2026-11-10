package com.learnloop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class InvitationDao {

    private static final String SELECT_INVITATION = """
            SELECT c.id, c.teacher_user_id, c.status, c.title, c.description,
                   c.invitation_token_created_at, c.invitation_token_expires_at,
                   c.invitation_token_revoked_at, c.max_students,
                   s.code AS subject_code, s.name AS subject_name,
                   u.full_name AS teacher_name
            FROM classes c
            JOIN subjects s ON s.id = c.subject_id
            JOIN users u ON u.id = c.teacher_user_id
            """;

    public Optional<StoredInvitation> findByTokenHash(Connection connection, String tokenHash)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        return findOne(connection, SELECT_INVITATION + "WHERE c.invitation_token_hash = ?", false, tokenHash);
    }

    public Optional<StoredInvitation> findByTokenHashForUpdate(Connection connection, String tokenHash)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        return findOne(connection, SELECT_INVITATION + "WHERE c.invitation_token_hash = ?", true, tokenHash);
    }

    public Optional<StoredInvitation> findOwned(Connection connection, long classId, long teacherUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        return findOne(connection, SELECT_INVITATION + "WHERE c.id = ? AND c.teacher_user_id = ?",
                false, classId, teacherUserId);
    }

    public Optional<StoredInvitation> findOwnedForUpdate(Connection connection, long classId, long teacherUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        return findOne(connection, SELECT_INVITATION + "WHERE c.id = ? AND c.teacher_user_id = ?",
                true, classId, teacherUserId);
    }

    public int issue(
            Connection connection,
            long classId,
            long teacherUserId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE classes
                SET invitation_token_hash = ?,
                    invitation_token_created_at = ?,
                    invitation_token_expires_at = ?,
                    invitation_token_revoked_at = NULL
                WHERE id = ? AND teacher_user_id = ? AND status = 'ACTIVE'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            JdbcSupport.setInstant(statement, 2, createdAt);
            JdbcSupport.setInstant(statement, 3, expiresAt);
            statement.setLong(4, classId);
            statement.setLong(5, teacherUserId);
            return statement.executeUpdate();
        }
    }

    public int revoke(Connection connection, long classId, long teacherUserId, Instant revokedAt)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE classes
                SET invitation_token_revoked_at = ?
                WHERE id = ? AND teacher_user_id = ?
                  AND invitation_token_created_at IS NOT NULL
                  AND invitation_token_revoked_at IS NULL
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setInstant(statement, 1, revokedAt);
            statement.setLong(2, classId);
            statement.setLong(3, teacherUserId);
            return statement.executeUpdate();
        }
    }

    public int expire(Connection connection, long classId, long teacherUserId, Instant expiredAt)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE classes
                SET invitation_token_expires_at = ?
                WHERE id = ? AND teacher_user_id = ?
                  AND invitation_token_created_at IS NOT NULL
                  AND invitation_token_revoked_at IS NULL
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setInstant(statement, 1, expiredAt);
            statement.setLong(2, classId);
            statement.setLong(3, teacherUserId);
            return statement.executeUpdate();
        }
    }

    public Optional<String> findEnrollmentStatus(Connection connection, long classId, long studentUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT status FROM class_enrollments
                WHERE class_id = ? AND student_user_id = ?
                """;
        return findStatus(connection, sql, classId, studentUserId);
    }

    public Optional<String> findJoinRequestStatus(Connection connection, long classId, long studentUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT status FROM class_join_requests
                WHERE class_id = ? AND student_user_id = ?
                """;
        return findStatus(connection, sql, classId, studentUserId);
    }

    public long insertJoinRequest(
            Connection connection, long classId, long studentUserId, String message) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                INSERT INTO class_join_requests (class_id, student_user_id, status, message)
                VALUES (?, ?, 'PENDING', ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, classId);
            statement.setLong(2, studentUserId);
            statement.setString(3, message);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("The database did not return a generated join request id.");
    }

    private static Optional<StoredInvitation> findOne(
            Connection connection, String sql, boolean forUpdate, Object... parameters) throws SQLException {
        String statementSql = forUpdate ? sql + " FOR UPDATE" : sql;
        try (PreparedStatement statement = connection.prepareStatement(statementSql)) {
            for (int index = 0; index < parameters.length; index++) {
                Object parameter = parameters[index];
                if (parameter instanceof Long value) {
                    statement.setLong(index + 1, value);
                } else {
                    statement.setString(index + 1, String.valueOf(parameter));
                }
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    private static Optional<String> findStatus(
            Connection connection, String sql, long classId, long studentUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.ofNullable(resultSet.getString(1)) : Optional.empty();
            }
        }
    }

    private static StoredInvitation map(ResultSet resultSet) throws SQLException {
        return new StoredInvitation(
                resultSet.getLong("id"),
                resultSet.getLong("teacher_user_id"),
                resultSet.getString("status"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("subject_code"),
                resultSet.getString("subject_name"),
                resultSet.getString("teacher_name"),
                JdbcSupport.getNullableInteger(resultSet, "max_students"),
                JdbcSupport.getInstant(resultSet, "invitation_token_created_at"),
                JdbcSupport.getInstant(resultSet, "invitation_token_expires_at"),
                JdbcSupport.getInstant(resultSet, "invitation_token_revoked_at"));
    }

    public record StoredInvitation(
            long classId,
            long teacherUserId,
            String classStatus,
            String title,
            String description,
            String subjectCode,
            String subjectName,
            String teacherName,
            Integer maxStudents,
            Instant invitationCreatedAt,
            Instant invitationExpiresAt,
            Instant invitationRevokedAt
    ) {
    }
}
