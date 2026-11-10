package com.learnloop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JoinRequestDao {

    private static final String SELECT_JOIN_REQUESTS = """
            SELECT
                jr.id,
                jr.class_id,
                c.title AS class_title,
                c.status AS class_status,
                s.code AS subject_code,
                s.name AS subject_name,
                teacher.full_name AS teacher_name,
                student.full_name AS student_name,
                student.email AS student_email,
                jr.status,
                jr.message,
                jr.created_at,
                jr.reviewed_at
            FROM class_join_requests jr
            JOIN classes c ON c.id = jr.class_id
            JOIN subjects s ON s.id = c.subject_id
            JOIN users teacher ON teacher.id = c.teacher_user_id
            JOIN users student ON student.id = jr.student_user_id
            """;

    public List<StoredJoinRequest> findByStudent(
            Connection connection, long studentUserId, boolean pendingOnly) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_JOIN_REQUESTS + """
                WHERE jr.student_user_id = ?
                """ + (pendingOnly ? " AND jr.status = 'PENDING'\n" : "") + """
                ORDER BY jr.created_at DESC, jr.id DESC
                """;
        return findMany(connection, sql, studentUserId);
    }

    public List<StoredJoinRequest> findPendingForTeacher(Connection connection, long teacherUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_JOIN_REQUESTS + """
                WHERE c.teacher_user_id = ? AND jr.status = 'PENDING'
                ORDER BY jr.created_at ASC, jr.id ASC
                """;
        return findMany(connection, sql, teacherUserId);
    }

    private static List<StoredJoinRequest> findMany(
            Connection connection, String sql, long userId) throws SQLException {
        List<StoredJoinRequest> requests = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(map(resultSet));
                }
            }
        }
        return List.copyOf(requests);
    }

    private static StoredJoinRequest map(ResultSet resultSet) throws SQLException {
        return new StoredJoinRequest(
                resultSet.getLong("id"),
                resultSet.getLong("class_id"),
                resultSet.getString("class_title"),
                resultSet.getString("class_status"),
                resultSet.getString("subject_code"),
                resultSet.getString("subject_name"),
                resultSet.getString("teacher_name"),
                resultSet.getString("student_name"),
                resultSet.getString("student_email"),
                resultSet.getString("status"),
                resultSet.getString("message"),
                JdbcSupport.getInstant(resultSet, "created_at"),
                JdbcSupport.getInstant(resultSet, "reviewed_at"));
    }

    public Optional<StoredJoinRequest> findByIdForUpdate(Connection connection, long requestId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_JOIN_REQUESTS + " WHERE jr.id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public int updateStatus(Connection connection, long requestId, String status, long reviewedByUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = "UPDATE class_join_requests SET status = ?, reviewed_by_user_id = ?, reviewed_at = CURRENT_TIMESTAMP(6), updated_at = CURRENT_TIMESTAMP(6) WHERE id = ? AND status = 'PENDING'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, reviewedByUserId);
            statement.setLong(3, requestId);
            return statement.executeUpdate();
        }
    }

    public record StoredJoinRequest(
            long id,
            long classId,
            String classTitle,
            String classStatus,
            String subjectCode,
            String subjectName,
            String teacherName,
            String studentName,
            String studentEmail,
            String status,
            String message,
            Instant createdAt,
            Instant reviewedAt
    ) {
    }
}
