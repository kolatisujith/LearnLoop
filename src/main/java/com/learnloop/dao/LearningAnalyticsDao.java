package com.learnloop.dao;

import com.learnloop.service.AnalyticsViews.RecentResult;
import com.learnloop.service.AnalyticsViews.StudentClassPerformance;
import com.learnloop.service.AnalyticsViews.TeacherClassPerformance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class LearningAnalyticsDao {

    public StudentMetrics loadStudentMetrics(Connection connection, long studentUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    COUNT(CASE WHEN aa.status = 'GRADED' THEN 1 END) AS graded_attempts,
                    COUNT(CASE WHEN aa.status = 'SUBMITTED' THEN 1 END) AS awaiting_grading,
                    AVG(CASE WHEN aa.status = 'GRADED' AND totals.total_points > 0
                        THEN aa.score / totals.total_points * 100 END) AS average_percentage
                FROM assessment_attempts aa
                JOIN assessments a ON a.id = aa.assessment_id
                LEFT JOIN (
                    SELECT assessment_id, SUM(points) AS total_points
                    FROM questions GROUP BY assessment_id
                ) totals ON totals.assessment_id = a.id
                WHERE aa.student_user_id = ?
                  AND EXISTS (
                    SELECT 1 FROM class_enrollments e
                    WHERE e.class_id = a.class_id
                      AND e.student_user_id = aa.student_user_id
                      AND e.status = 'ACTIVE'
                  )
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return new StudentMetrics(0, 0, null);
                }
                return new StudentMetrics(
                        resultSet.getLong("graded_attempts"),
                        resultSet.getLong("awaiting_grading"),
                        percentageText(resultSet.getBigDecimal("average_percentage")));
            }
        }
    }

    public List<StudentClassPerformance> findStudentClassPerformance(
            Connection connection, long studentUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    c.id AS class_id,
                    c.title AS class_title,
                    (SELECT COUNT(*) FROM assessments available
                     WHERE available.class_id = c.id
                       AND available.status = 'PUBLISHED'
                       AND (available.available_from IS NULL OR available.available_from <= CURRENT_TIMESTAMP(6))
                       AND (available.due_at IS NULL OR available.due_at >= CURRENT_TIMESTAMP(6))
                    ) AS available_assessments,
                    (SELECT COUNT(*) FROM assessment_attempts aa
                     JOIN assessments a ON a.id = aa.assessment_id
                     WHERE a.class_id = c.id AND aa.student_user_id = ?
                       AND aa.status = 'GRADED') AS graded_attempts,
                    (SELECT AVG(aa.score / totals.total_points * 100)
                     FROM assessment_attempts aa
                     JOIN assessments a ON a.id = aa.assessment_id
                     JOIN (
                        SELECT assessment_id, SUM(points) AS total_points
                        FROM questions GROUP BY assessment_id
                     ) totals ON totals.assessment_id = a.id
                     WHERE a.class_id = c.id AND aa.student_user_id = ?
                       AND aa.status = 'GRADED' AND totals.total_points > 0
                    ) AS average_percentage
                FROM class_enrollments e
                JOIN classes c ON c.id = e.class_id
                WHERE e.student_user_id = ? AND e.status = 'ACTIVE'
                ORDER BY c.title, c.id
                """;
        List<StudentClassPerformance> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, studentUserId);
            statement.setLong(3, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new StudentClassPerformance(
                            resultSet.getLong("class_id"),
                            resultSet.getString("class_title"),
                            resultSet.getLong("available_assessments"),
                            resultSet.getLong("graded_attempts"),
                            percentageText(resultSet.getBigDecimal("average_percentage"))));
                }
            }
        }
        return List.copyOf(rows);
    }

    public List<RecentResult> findRecentStudentResults(Connection connection, long studentUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT aa.id AS attempt_id, a.title AS assessment_title, c.title AS class_title,
                       aa.status, aa.score, aa.submitted_at, aa.graded_at,
                       a.passing_score_percent,
                       totals.total_points,
                       CASE WHEN aa.status = 'GRADED' AND totals.total_points > 0
                            THEN aa.score / totals.total_points * 100 ELSE NULL END AS percentage
                FROM assessment_attempts aa
                JOIN assessments a ON a.id = aa.assessment_id
                JOIN classes c ON c.id = a.class_id
                LEFT JOIN (
                    SELECT assessment_id, SUM(points) AS total_points
                    FROM questions GROUP BY assessment_id
                ) totals ON totals.assessment_id = a.id
                WHERE aa.student_user_id = ?
                  AND aa.status IN ('SUBMITTED', 'GRADED')
                  AND EXISTS (
                    SELECT 1 FROM class_enrollments e
                    WHERE e.class_id = a.class_id
                      AND e.student_user_id = aa.student_user_id
                      AND e.status = 'ACTIVE'
                  )
                ORDER BY COALESCE(aa.graded_at, aa.submitted_at) DESC, aa.id DESC
                LIMIT 10
                """;
        List<RecentResult> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String status = resultSet.getString("status");
                    BigDecimal percentage = resultSet.getBigDecimal("percentage");
                    BigDecimal passingScore = resultSet.getBigDecimal("passing_score_percent");
                    String outcome;
                    if (!"GRADED".equals(status)) {
                        outcome = "Awaiting grading";
                    } else if (percentage == null || passingScore == null) {
                        outcome = "Graded";
                    } else {
                        outcome = percentage.compareTo(passingScore) >= 0 ? "Pass mark met" : "Review recommended";
                    }
                    rows.add(new RecentResult(
                            resultSet.getLong("attempt_id"),
                            resultSet.getString("assessment_title"),
                            resultSet.getString("class_title"),
                            "GRADED".equals(status) ? "Graded" : "Submitted",
                            percentageText(percentage),
                            outcome,
                            JdbcSupport.getDate(resultSet, "submitted_at"),
                            JdbcSupport.getDate(resultSet, "graded_at")));
                }
            }
        }
        return List.copyOf(rows);
    }

    public List<TeacherClassPerformance> findTeacherClassPerformance(
            Connection connection, long teacherUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    c.id AS class_id,
                    c.title AS class_title,
                    (SELECT COUNT(*) FROM class_enrollments e
                     WHERE e.class_id = c.id AND e.status = 'ACTIVE') AS enrolled_students,
                    (SELECT COUNT(*) FROM assessments a
                     WHERE a.class_id = c.id AND a.status = 'PUBLISHED') AS published_assessments,
                    (SELECT COUNT(*) FROM assessment_attempts aa
                     JOIN assessments a ON a.id = aa.assessment_id
                     WHERE a.class_id = c.id AND aa.status = 'GRADED') AS graded_attempts,
                    (SELECT COUNT(*) FROM assessment_attempts aa
                     JOIN assessments a ON a.id = aa.assessment_id
                     WHERE a.class_id = c.id AND aa.status = 'SUBMITTED') AS awaiting_grading,
                    (SELECT AVG(aa.score / totals.total_points * 100)
                     FROM assessment_attempts aa
                     JOIN assessments a ON a.id = aa.assessment_id
                     JOIN (
                        SELECT assessment_id, SUM(points) AS total_points
                        FROM questions GROUP BY assessment_id
                     ) totals ON totals.assessment_id = a.id
                     WHERE a.class_id = c.id AND aa.status = 'GRADED'
                       AND totals.total_points > 0
                    ) AS average_percentage,
                    (SELECT COUNT(*) FROM assessment_attempts aa
                     JOIN assessments a ON a.id = aa.assessment_id
                     JOIN (
                        SELECT assessment_id, SUM(points) AS total_points
                        FROM questions GROUP BY assessment_id
                     ) totals ON totals.assessment_id = a.id
                     WHERE a.class_id = c.id AND aa.status = 'GRADED'
                       AND a.passing_score_percent IS NOT NULL
                       AND totals.total_points > 0
                       AND aa.score / totals.total_points * 100 >= a.passing_score_percent
                    ) AS passed_attempts,
                    (SELECT COUNT(*) FROM assessment_attempts aa
                     JOIN assessments a ON a.id = aa.assessment_id
                     JOIN (
                        SELECT assessment_id, SUM(points) AS total_points
                        FROM questions GROUP BY assessment_id
                     ) totals ON totals.assessment_id = a.id
                     WHERE a.class_id = c.id AND aa.status = 'GRADED'
                       AND a.passing_score_percent IS NOT NULL
                       AND totals.total_points > 0
                    ) AS pass_eligible_attempts
                FROM classes c
                WHERE c.teacher_user_id = ?
                ORDER BY c.title, c.id
                """;
        List<TeacherClassPerformance> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long passed = resultSet.getLong("passed_attempts");
                    long eligible = resultSet.getLong("pass_eligible_attempts");
                    String passRate = eligible == 0 ? null
                            : String.format(Locale.ROOT, "%.1f%%", (passed * 100.0) / eligible);
                    rows.add(new TeacherClassPerformance(
                            resultSet.getLong("class_id"),
                            resultSet.getString("class_title"),
                            resultSet.getLong("enrolled_students"),
                            resultSet.getLong("published_assessments"),
                            resultSet.getLong("graded_attempts"),
                            resultSet.getLong("awaiting_grading"),
                            percentageText(resultSet.getBigDecimal("average_percentage")),
                            passRate));
                }
            }
        }
        return List.copyOf(rows);
    }

    private static String percentageText(BigDecimal value) {
        return value == null ? null : value.setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    public record StudentMetrics(long gradedAttempts, long awaitingGrading, String averagePercentage) {
    }
}
