package com.learnloop.dao;

import com.learnloop.service.DashboardViews.DiscoveryClassRow;
import com.learnloop.service.DashboardViews.StudentAssessmentRow;
import com.learnloop.service.DashboardViews.StudentClassRow;
import com.learnloop.service.DashboardViews.StudentJoinRequestRow;
import com.learnloop.service.DashboardViews.StudentMaterialRow;
import com.learnloop.service.DashboardViews.StudentOverview;
import com.learnloop.service.DashboardViews.StudentResultRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class StudentDashboardDao {

    public StudentOverview loadOverview(Connection connection, long studentUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        List<StudentClassRow> classes = findEnrolledClasses(connection, studentUserId);
        List<StudentJoinRequestRow> pendingRequests = findPendingRequests(connection, studentUserId);
        List<StudentAssessmentRow> availableAssessments = findAvailableAssessments(connection, studentUserId);
        List<StudentResultRow> recentResults = findRecentResults(connection, studentUserId);
        return new StudentOverview(
                classes.size(),
                pendingRequests.size(),
                availableAssessments.size(),
                recentResults.size(),
                classes,
                pendingRequests,
                availableAssessments,
                recentResults);
    }

    public List<StudentClassRow> findEnrolledClasses(Connection connection, long studentUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    c.id,
                    c.title,
                    c.description,
                    s.code AS subject_code,
                    s.name AS subject_name,
                    u.full_name AS teacher_name,
                    c.status AS class_status,
                    e.status AS enrollment_status,
                    c.max_students,
                    e.created_at AS enrolled_at,
                    (SELECT COUNT(*) FROM class_enrollments enrolled
                     WHERE enrolled.class_id = c.id AND enrolled.status = 'ACTIVE') AS enrolled_count,
                    (SELECT COUNT(*) FROM class_materials m
                     WHERE m.class_id = c.id AND m.is_published = TRUE) AS material_count,
                    (SELECT COUNT(*) FROM assessments a
                     WHERE a.class_id = c.id AND a.status = 'PUBLISHED') AS assessment_count
                FROM class_enrollments e
                JOIN classes c ON c.id = e.class_id
                JOIN subjects s ON s.id = c.subject_id
                JOIN users u ON u.id = c.teacher_user_id
                WHERE e.student_user_id = ? AND e.status = 'ACTIVE'
                ORDER BY c.title, c.id
                """;
        List<StudentClassRow> classes = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    classes.add(new StudentClassRow(
                            resultSet.getLong("id"),
                            resultSet.getString("title"),
                            resultSet.getString("description"),
                            resultSet.getString("subject_code"),
                            resultSet.getString("subject_name"),
                            resultSet.getString("teacher_name"),
                            resultSet.getString("class_status"),
                            resultSet.getString("enrollment_status"),
                            JdbcSupport.getNullableInteger(resultSet, "max_students"),
                            resultSet.getLong("enrolled_count"),
                            resultSet.getLong("material_count"),
                            resultSet.getLong("assessment_count"),
                            JdbcSupport.getDate(resultSet, "enrolled_at")));
                }
            }
        }
        return List.copyOf(classes);
    }

    public List<StudentJoinRequestRow> findPendingRequests(Connection connection, long studentUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    jr.id,
                    c.id AS class_id,
                    c.title AS class_title,
                    c.status AS class_status,
                    s.code AS subject_code,
                    s.name AS subject_name,
                    teacher.full_name AS teacher_name,
                    jr.status,
                    jr.message,
                    jr.created_at,
                    jr.reviewed_at
                FROM class_join_requests jr
                JOIN classes c ON c.id = jr.class_id
                JOIN subjects s ON s.id = c.subject_id
                JOIN users teacher ON teacher.id = c.teacher_user_id
                WHERE jr.student_user_id = ? AND jr.status = 'PENDING'
                ORDER BY jr.created_at ASC, jr.id ASC
                """;
        List<StudentJoinRequestRow> requests = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(new StudentJoinRequestRow(
                            resultSet.getLong("id"),
                            resultSet.getString("class_title"),
                            resultSet.getString("subject_code"),
                            resultSet.getString("status"),
                            resultSet.getString("message"),
                            JdbcSupport.getDate(resultSet, "created_at"),
                            resultSet.getLong("class_id"),
                            resultSet.getString("class_status"),
                            resultSet.getString("subject_name"),
                            resultSet.getString("teacher_name"),
                            JdbcSupport.getDate(resultSet, "reviewed_at")));
                }
            }
        }
        return List.copyOf(requests);
    }

    public List<StudentAssessmentRow> findAvailableAssessments(Connection connection, long studentUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    a.id,
                    c.title AS class_title,
                    s.code AS subject_code,
                    a.title,
                    a.available_from,
                    a.due_at,
                    a.time_limit_minutes,
                    a.max_attempts,
                    (SELECT COUNT(*) FROM assessment_attempts aa
                     WHERE aa.assessment_id = a.id AND aa.student_user_id = ?) AS attempts_used
                FROM assessments a
                JOIN classes c ON c.id = a.class_id
                JOIN subjects s ON s.id = c.subject_id
                JOIN class_enrollments e ON e.class_id = a.class_id
                    AND e.student_user_id = ? AND e.status = 'ACTIVE'
                WHERE a.status = 'PUBLISHED'
                  AND (a.available_from IS NULL OR a.available_from <= CURRENT_TIMESTAMP(6))
                  AND (a.due_at IS NULL OR a.due_at >= CURRENT_TIMESTAMP(6))
                ORDER BY a.due_at IS NULL, a.due_at, a.title
                """;
        List<StudentAssessmentRow> assessments = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    assessments.add(new StudentAssessmentRow(
                            resultSet.getLong("id"),
                            resultSet.getString("class_title"),
                            resultSet.getString("subject_code"),
                            resultSet.getString("title"),
                            JdbcSupport.getDate(resultSet, "available_from"),
                            JdbcSupport.getDate(resultSet, "due_at"),
                            JdbcSupport.getNullableInteger(resultSet, "time_limit_minutes"),
                            resultSet.getInt("max_attempts"),
                            resultSet.getLong("attempts_used")));
                }
            }
        }
        return List.copyOf(assessments);
    }

    public List<StudentResultRow> findRecentResults(Connection connection, long studentUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    aa.id AS attempt_id,
                    a.title AS assessment_title,
                    c.title AS class_title,
                    aa.status,
                    aa.score,
                    aa.submitted_at,
                    aa.graded_at,
                    a.passing_score_percent
                FROM assessment_attempts aa
                JOIN assessments a ON a.id = aa.assessment_id
                JOIN classes c ON c.id = a.class_id
                WHERE aa.student_user_id = ?
                  AND aa.status IN ('SUBMITTED', 'GRADED')
                ORDER BY COALESCE(aa.graded_at, aa.submitted_at, aa.started_at) DESC, aa.id DESC
                LIMIT 8
                """;
        List<StudentResultRow> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(new StudentResultRow(
                            resultSet.getLong("attempt_id"),
                            resultSet.getString("assessment_title"),
                            resultSet.getString("class_title"),
                            resultSet.getString("status"),
                            decimalText(resultSet.getBigDecimal("score")),
                            JdbcSupport.getDate(resultSet, "submitted_at"),
                            JdbcSupport.getDate(resultSet, "graded_at"),
                            decimalText(resultSet.getBigDecimal("passing_score_percent"))));
                }
            }
        }
        return List.copyOf(results);
    }

    public List<DiscoveryClassRow> findDiscoverableClasses(Connection connection, long studentUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    c.id,
                    c.title,
                    c.description,
                    s.code AS subject_code,
                    s.name AS subject_name,
                    u.full_name AS teacher_name,
                    c.max_students,
                    (SELECT COUNT(*) FROM class_enrollments enrolled
                     WHERE enrolled.class_id = c.id AND enrolled.status = 'ACTIVE') AS enrolled_count,
                    (SELECT jr.status FROM class_join_requests jr
                     WHERE jr.class_id = c.id AND jr.student_user_id = ?
                     ORDER BY jr.created_at DESC, jr.id DESC LIMIT 1) AS request_status
                FROM classes c
                JOIN subjects s ON s.id = c.subject_id
                JOIN users u ON u.id = c.teacher_user_id
                WHERE c.status = 'ACTIVE'
                  AND NOT EXISTS (
                      SELECT 1 FROM class_enrollments e
                      WHERE e.class_id = c.id AND e.student_user_id = ? AND e.status = 'ACTIVE'
                  )
                ORDER BY s.name, c.title
                """;
        List<DiscoveryClassRow> classes = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    classes.add(new DiscoveryClassRow(
                            resultSet.getLong("id"),
                            resultSet.getString("title"),
                            resultSet.getString("description"),
                            resultSet.getString("subject_code"),
                            resultSet.getString("subject_name"),
                            resultSet.getString("teacher_name"),
                            JdbcSupport.getNullableInteger(resultSet, "max_students"),
                            resultSet.getLong("enrolled_count"),
                            resultSet.getString("request_status")));
                }
            }
        }
        return List.copyOf(classes);
    }

    public List<StudentMaterialRow> findEnrolledMaterials(Connection connection, long studentUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    m.id,
                    c.title AS class_title,
                    s.code AS subject_code,
                    m.title,
                    m.description,
                    m.material_type,
                    m.content_text,
                    m.resource_url,
                    m.created_at
                FROM class_materials m
                JOIN classes c ON c.id = m.class_id
                JOIN subjects s ON s.id = c.subject_id
                JOIN class_enrollments e ON e.class_id = c.id
                    AND e.student_user_id = ? AND e.status = 'ACTIVE'
                WHERE m.is_published = TRUE
                ORDER BY c.title, m.position, m.created_at
                """;
        List<StudentMaterialRow> materials = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    materials.add(new StudentMaterialRow(
                            resultSet.getLong("id"),
                            resultSet.getString("class_title"),
                            resultSet.getString("subject_code"),
                            resultSet.getString("title"),
                            resultSet.getString("description"),
                            resultSet.getString("material_type"),
                            resultSet.getString("content_text"),
                            resultSet.getString("resource_url"),
                            JdbcSupport.getDate(resultSet, "created_at")));
                }
            }
        }
        return List.copyOf(materials);
    }

    private static String decimalText(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
