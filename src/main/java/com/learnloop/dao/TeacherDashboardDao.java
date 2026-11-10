package com.learnloop.dao;

import com.learnloop.service.DashboardViews.TeacherAssessmentRow;
import com.learnloop.service.DashboardViews.TeacherClassRow;
import com.learnloop.service.DashboardViews.TeacherJoinRequestRow;
import com.learnloop.service.DashboardViews.TeacherMaterialRow;
import com.learnloop.service.DashboardViews.TeacherOverview;
import com.learnloop.service.DashboardViews.TeacherProgressRow;
import com.learnloop.service.DashboardViews.TeacherSubjectRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TeacherDashboardDao {

    public TeacherOverview loadOverview(Connection connection, long teacherUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String approvalStatus = findApprovalStatus(connection, teacherUserId);
        List<TeacherSubjectRow> subjects = findAssignedSubjects(connection, teacherUserId);
        List<TeacherClassRow> classes = findClasses(connection, teacherUserId);
        List<TeacherJoinRequestRow> pendingRequests = findPendingRequests(connection, teacherUserId);
        long totalEnrolledStudents = classes.stream().mapToLong(TeacherClassRow::enrolledCount).sum();
        return new TeacherOverview(
                approvalStatus,
                subjects,
                classes,
                pendingRequests,
                totalEnrolledStudents);
    }

    public List<TeacherSubjectRow> findAssignedSubjects(Connection connection, long teacherUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT s.id, s.code, s.name
                FROM teacher_subject_assignments tsa
                JOIN subjects s ON s.id = tsa.subject_id
                WHERE tsa.teacher_user_id = ? AND tsa.is_active = TRUE AND s.is_active = TRUE
                ORDER BY s.name, s.code
                """;
        List<TeacherSubjectRow> subjects = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    subjects.add(new TeacherSubjectRow(
                            resultSet.getLong("id"),
                            resultSet.getString("code"),
                            resultSet.getString("name")));
                }
            }
        }
        return List.copyOf(subjects);
    }

    public List<TeacherClassRow> findClasses(Connection connection, long teacherUserId) throws SQLException {
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
                     WHERE jr.class_id = c.id AND jr.status = 'PENDING') AS pending_request_count
                FROM classes c
                JOIN subjects s ON s.id = c.subject_id
                WHERE c.teacher_user_id = ?
                ORDER BY c.created_at DESC, c.id DESC
                """;
        List<TeacherClassRow> classes = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    classes.add(new TeacherClassRow(
                            resultSet.getLong("id"),
                            resultSet.getString("title"),
                            resultSet.getString("subject_code"),
                            resultSet.getString("status"),
                            JdbcSupport.getNullableInteger(resultSet, "max_students"),
                            resultSet.getLong("enrolled_count"),
                            resultSet.getLong("pending_request_count"),
                            JdbcSupport.getDate(resultSet, "created_at")));
                }
            }
        }
        return List.copyOf(classes);
    }

    public List<TeacherJoinRequestRow> findPendingRequests(Connection connection, long teacherUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    jr.id,
                    c.id AS class_id,
                    c.title AS class_title,
                    u.full_name AS student_name,
                    u.email AS student_email,
                    jr.message,
                    jr.created_at
                FROM class_join_requests jr
                JOIN classes c ON c.id = jr.class_id
                JOIN users u ON u.id = jr.student_user_id
                WHERE c.teacher_user_id = ? AND jr.status = 'PENDING'
                ORDER BY jr.created_at ASC, jr.id ASC
                """;
        List<TeacherJoinRequestRow> requests = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(new TeacherJoinRequestRow(
                            resultSet.getLong("id"),
                            resultSet.getLong("class_id"),
                            resultSet.getString("class_title"),
                            resultSet.getString("student_name"),
                            resultSet.getString("student_email"),
                            resultSet.getString("message"),
                            JdbcSupport.getDate(resultSet, "created_at")));
                }
            }
        }
        return List.copyOf(requests);
    }

    public List<TeacherMaterialRow> findMaterials(Connection connection, long teacherUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    m.id,
                    c.title AS class_title,
                    s.code AS subject_code,
                    m.title,
                    m.material_type,
                    m.is_published,
                    m.created_at
                FROM class_materials m
                JOIN classes c ON c.id = m.class_id
                JOIN subjects s ON s.id = c.subject_id
                WHERE c.teacher_user_id = ?
                ORDER BY c.title, m.position, m.created_at
                """;
        List<TeacherMaterialRow> materials = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    materials.add(new TeacherMaterialRow(
                            resultSet.getLong("id"),
                            resultSet.getString("class_title"),
                            resultSet.getString("subject_code"),
                            resultSet.getString("title"),
                            resultSet.getString("material_type"),
                            resultSet.getBoolean("is_published"),
                            JdbcSupport.getDate(resultSet, "created_at")));
                }
            }
        }
        return List.copyOf(materials);
    }

    public List<TeacherAssessmentRow> findAssessments(Connection connection, long teacherUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    a.id,
                    c.title AS class_title,
                    s.code AS subject_code,
                    a.title,
                    a.status,
                    a.available_from,
                    a.due_at,
                    a.max_attempts,
                    (SELECT COUNT(*) FROM assessment_attempts aa
                     WHERE aa.assessment_id = a.id) AS attempt_count,
                    (SELECT COUNT(*) FROM assessment_attempts aa
                     WHERE aa.assessment_id = a.id AND aa.status = 'GRADED') AS graded_count
                FROM assessments a
                JOIN classes c ON c.id = a.class_id
                JOIN subjects s ON s.id = c.subject_id
                WHERE c.teacher_user_id = ?
                ORDER BY a.created_at DESC, a.id DESC
                """;
        List<TeacherAssessmentRow> assessments = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    assessments.add(new TeacherAssessmentRow(
                            resultSet.getLong("id"),
                            resultSet.getString("class_title"),
                            resultSet.getString("subject_code"),
                            resultSet.getString("title"),
                            resultSet.getString("status"),
                            JdbcSupport.getDate(resultSet, "available_from"),
                            JdbcSupport.getDate(resultSet, "due_at"),
                            resultSet.getInt("max_attempts"),
                            resultSet.getLong("attempt_count"),
                            resultSet.getLong("graded_count")));
                }
            }
        }
        return List.copyOf(assessments);
    }

    public List<TeacherProgressRow> findProgress(Connection connection, long teacherUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    c.id AS class_id,
                    c.title AS class_title,
                    (SELECT COUNT(*) FROM class_enrollments e
                     WHERE e.class_id = c.id AND e.status = 'ACTIVE') AS enrolled_count,
                    (SELECT COUNT(*) FROM assessment_attempts aa
                     JOIN assessments a ON a.id = aa.assessment_id
                     WHERE a.class_id = c.id AND aa.status = 'GRADED') AS graded_attempts,
                    (SELECT AVG(aa.score) FROM assessment_attempts aa
                     JOIN assessments a ON a.id = aa.assessment_id
                     WHERE a.class_id = c.id AND aa.status = 'GRADED') AS average_score
                FROM classes c
                WHERE c.teacher_user_id = ?
                ORDER BY c.title
                """;
        List<TeacherProgressRow> progress = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    progress.add(new TeacherProgressRow(
                            resultSet.getLong("class_id"),
                            resultSet.getString("class_title"),
                            resultSet.getLong("enrolled_count"),
                            resultSet.getLong("graded_attempts"),
                            decimalText(resultSet.getBigDecimal("average_score"))));
                }
            }
        }
        return List.copyOf(progress);
    }

    private static String findApprovalStatus(Connection connection, long teacherUserId) throws SQLException {
        String sql = "SELECT approval_status FROM users WHERE id = ? AND role = 'TEACHER'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("The teacher account could not be found.");
                }
                return resultSet.getString("approval_status");
            }
        }
    }

    private static String decimalText(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
