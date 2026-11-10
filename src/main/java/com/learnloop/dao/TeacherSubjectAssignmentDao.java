package com.learnloop.dao;

import com.learnloop.model.TeacherSubjectAssignment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public final class TeacherSubjectAssignmentDao {

    public Optional<TeacherSubjectAssignment> findActiveForUpdate(
            Connection connection, long teacherUserId, long subjectId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT id, teacher_user_id, subject_id, is_active
                FROM teacher_subject_assignments
                WHERE teacher_user_id = ? AND subject_id = ? AND is_active = TRUE
                FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<TeacherSubjectAssignment> findForUpdate(
            Connection connection, long teacherUserId, long subjectId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT id, teacher_user_id, subject_id, is_active
                FROM teacher_subject_assignments
                WHERE teacher_user_id = ? AND subject_id = ?
                FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public void upsertActive(
            Connection connection,
            long teacherUserId,
            long subjectId,
            long assignedByUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                INSERT INTO teacher_subject_assignments (
                    teacher_user_id, subject_id, assigned_by_user_id, is_active
                )
                VALUES (?, ?, ?, TRUE)
                ON DUPLICATE KEY UPDATE
                    assigned_by_user_id = VALUES(assigned_by_user_id),
                    is_active = TRUE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, subjectId);
            statement.setLong(3, assignedByUserId);
            statement.executeUpdate();
        }
    }

    public void deactivate(Connection connection, long teacherUserId, long subjectId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE teacher_subject_assignments
                SET is_active = FALSE
                WHERE teacher_user_id = ? AND subject_id = ? AND is_active = TRUE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, subjectId);
            statement.executeUpdate();
        }
    }

    private static TeacherSubjectAssignment map(ResultSet resultSet) throws SQLException {
        return new TeacherSubjectAssignment(
                resultSet.getLong("id"),
                resultSet.getLong("teacher_user_id"),
                resultSet.getLong("subject_id"),
                resultSet.getBoolean("is_active"));
    }
}
