package com.learnloop.dao;

import com.learnloop.model.LearningClass;
import com.learnloop.model.NewLearningClass;
import com.learnloop.service.ClassViews.AssignedSubject;
import com.learnloop.service.ClassViews.ClassDetails;
import com.learnloop.service.ClassViews.ClassSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ClassDao {

    private static final String SELECT_COLUMNS = """
            SELECT id, subject_id, teacher_user_id, title, description,
                   invitation_token_hash, invitation_token_created_at,
                   invitation_token_expires_at, invitation_token_revoked_at,
                   status, max_students, created_at, updated_at
            FROM classes
            """;

    public long insert(Connection connection, NewLearningClass newClass) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(newClass, "newClass");
        String sql = """
                INSERT INTO classes (
                    subject_id, teacher_user_id, title, description,
                    invitation_token_hash, invitation_token_created_at,
                    invitation_token_expires_at, invitation_token_revoked_at,
                    status, max_students
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, newClass.subjectId());
            statement.setLong(2, newClass.teacherUserId());
            statement.setString(3, newClass.title());
            statement.setString(4, newClass.description());
            statement.setString(5, newClass.invitationTokenHash());
            JdbcSupport.setInstant(statement, 6, newClass.invitationTokenCreatedAt());
            JdbcSupport.setInstant(statement, 7, newClass.invitationTokenExpiresAt());
            JdbcSupport.setInstant(statement, 8, newClass.invitationTokenRevokedAt());
            statement.setString(9, newClass.status());
            JdbcSupport.setNullableInteger(statement, 10, newClass.maxStudents());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("The database did not return a generated class id.");
    }

    public Optional<LearningClass> findById(Connection connection, long classId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_COLUMNS + "WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<LearningClass> findByIdAndTeacherForUpdate(
            Connection connection, long classId, long teacherUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_COLUMNS + "WHERE id = ? AND teacher_user_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classId);
            statement.setLong(2, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<LearningClass> findByInvitationTokenHash(Connection connection, String tokenHash)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SELECT_COLUMNS + "WHERE invitation_token_hash = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public List<AssignedSubject> findAssignedSubjects(Connection connection, long teacherUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT s.id, s.code, s.name
                FROM teacher_subject_assignments tsa
                JOIN subjects s ON s.id = tsa.subject_id
                WHERE tsa.teacher_user_id = ? AND tsa.is_active = TRUE AND s.is_active = TRUE
                ORDER BY s.name, s.code
                """;
        List<AssignedSubject> subjects = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    subjects.add(new AssignedSubject(
                            resultSet.getLong("id"),
                            resultSet.getString("code"),
                            resultSet.getString("name")));
                }
            }
        }
        return List.copyOf(subjects);
    }

    public List<ClassSummary> findOwnedClasses(Connection connection, long teacherUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    c.id,
                    c.title,
                    s.code AS subject_code,
                    c.status,
                    c.max_students,
                    c.created_at,
                    (SELECT COUNT(*) FROM class_enrollments e
                     WHERE e.class_id = c.id AND e.status = 'ACTIVE') AS enrolled_count,
                    (SELECT COUNT(*) FROM class_join_requests jr
                     WHERE jr.class_id = c.id AND jr.status = 'PENDING') AS pending_request_count,
                    (SELECT COUNT(*) FROM class_materials m
                     WHERE m.class_id = c.id) AS material_count,
                    (SELECT COUNT(*) FROM assessments a
                     WHERE a.class_id = c.id) AS assessment_count
                FROM classes c
                JOIN subjects s ON s.id = c.subject_id
                WHERE c.teacher_user_id = ?
                ORDER BY c.created_at DESC, c.id DESC
                """;
        List<ClassSummary> classes = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    classes.add(new ClassSummary(
                            resultSet.getLong("id"),
                            resultSet.getString("title"),
                            resultSet.getString("subject_code"),
                            resultSet.getString("status"),
                            JdbcSupport.getNullableInteger(resultSet, "max_students"),
                            resultSet.getLong("enrolled_count"),
                            resultSet.getLong("pending_request_count"),
                            resultSet.getLong("material_count"),
                            resultSet.getLong("assessment_count"),
                            JdbcSupport.getDate(resultSet, "created_at")));
                }
            }
        }
        return List.copyOf(classes);
    }

    public Optional<ClassDetails> findOwnedDetails(Connection connection, long classId, long teacherUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    c.id,
                    c.subject_id,
                    s.code AS subject_code,
                    s.name AS subject_name,
                    c.title,
                    c.description,
                    c.status,
                    c.max_students,
                    c.created_at,
                    c.updated_at,
                    (SELECT COUNT(*) FROM class_enrollments e
                     WHERE e.class_id = c.id AND e.status = 'ACTIVE') AS enrolled_count,
                    (SELECT COUNT(*) FROM class_join_requests jr
                     WHERE jr.class_id = c.id AND jr.status = 'PENDING') AS pending_request_count,
                    (SELECT COUNT(*) FROM class_join_requests jr
                     WHERE jr.class_id = c.id) AS join_request_count,
                    (SELECT COUNT(*) FROM class_materials m
                     WHERE m.class_id = c.id) AS material_count,
                    (SELECT COUNT(*) FROM assessments a
                     WHERE a.class_id = c.id) AS assessment_count
                FROM classes c
                JOIN subjects s ON s.id = c.subject_id
                WHERE c.id = ? AND c.teacher_user_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classId);
            statement.setLong(2, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapDetails(resultSet)) : Optional.empty();
            }
        }
    }

    public long countActiveEnrollments(Connection connection, long classId) throws SQLException {
        return count(connection, """
                SELECT COUNT(*) FROM class_enrollments
                WHERE class_id = ? AND status = 'ACTIVE'
                """, classId);
    }

    public long countEnrollments(Connection connection, long classId) throws SQLException {
        return count(connection, "SELECT COUNT(*) FROM class_enrollments WHERE class_id = ?", classId);
    }

    public long countJoinRequests(Connection connection, long classId) throws SQLException {
        return count(connection, "SELECT COUNT(*) FROM class_join_requests WHERE class_id = ?", classId);
    }

    public long countMaterials(Connection connection, long classId) throws SQLException {
        return count(connection, "SELECT COUNT(*) FROM class_materials WHERE class_id = ?", classId);
    }

    public long countAssessments(Connection connection, long classId) throws SQLException {
        return count(connection, "SELECT COUNT(*) FROM assessments WHERE class_id = ?", classId);
    }

    public int updateOwned(
            Connection connection,
            long classId,
            long teacherUserId,
            long subjectId,
            String title,
            String description,
            Integer maxStudents,
            String status) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE classes
                SET subject_id = ?, title = ?, description = ?, max_students = ?, status = ?
                WHERE id = ? AND teacher_user_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            statement.setString(2, title);
            statement.setString(3, description);
            JdbcSupport.setNullableInteger(statement, 4, maxStudents);
            statement.setString(5, status);
            statement.setLong(6, classId);
            statement.setLong(7, teacherUserId);
            return statement.executeUpdate();
        }
    }

    public int revokeInvitationIfClassInactive(
            Connection connection, long classId, Instant revokedAt) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE classes
                SET invitation_token_revoked_at = ?
                WHERE id = ?
                  AND status <> 'ACTIVE'
                  AND invitation_token_created_at IS NOT NULL
                  AND invitation_token_revoked_at IS NULL
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setInstant(statement, 1, revokedAt);
            statement.setLong(2, classId);
            return statement.executeUpdate();
        }
    }

    public int archiveOwned(Connection connection, long classId, long teacherUserId, String archivedStatus)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE classes
                SET status = ?
                WHERE id = ? AND teacher_user_id = ? AND status <> ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, archivedStatus);
            statement.setLong(2, classId);
            statement.setLong(3, teacherUserId);
            statement.setString(4, archivedStatus);
            return statement.executeUpdate();
        }
    }

    public int deleteOwnedDraft(Connection connection, long classId, long teacherUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = "DELETE FROM classes WHERE id = ? AND teacher_user_id = ? AND status = 'DRAFT'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classId);
            statement.setLong(2, teacherUserId);
            return statement.executeUpdate();
        }
    }

    private static long count(Connection connection, String sql, long classId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    private static LearningClass map(ResultSet resultSet) throws SQLException {
        return new LearningClass(
                resultSet.getLong("id"),
                resultSet.getLong("subject_id"),
                resultSet.getLong("teacher_user_id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("invitation_token_hash"),
                JdbcSupport.getInstant(resultSet, "invitation_token_created_at"),
                JdbcSupport.getInstant(resultSet, "invitation_token_expires_at"),
                JdbcSupport.getInstant(resultSet, "invitation_token_revoked_at"),
                resultSet.getString("status"),
                JdbcSupport.getNullableInteger(resultSet, "max_students"),
                JdbcSupport.getInstant(resultSet, "created_at"),
                JdbcSupport.getInstant(resultSet, "updated_at"));
    }

    private static ClassDetails mapDetails(ResultSet resultSet) throws SQLException {
        return new ClassDetails(
                resultSet.getLong("id"),
                resultSet.getLong("subject_id"),
                resultSet.getString("subject_code"),
                resultSet.getString("subject_name"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("status"),
                JdbcSupport.getNullableInteger(resultSet, "max_students"),
                resultSet.getLong("enrolled_count"),
                resultSet.getLong("pending_request_count"),
                resultSet.getLong("join_request_count"),
                resultSet.getLong("material_count"),
                resultSet.getLong("assessment_count"),
                JdbcSupport.getDate(resultSet, "created_at"),
                JdbcSupport.getDate(resultSet, "updated_at"));
    }
}
