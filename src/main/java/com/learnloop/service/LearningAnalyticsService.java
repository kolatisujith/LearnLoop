package com.learnloop.service;

import com.learnloop.dao.Database;
import com.learnloop.dao.LearningAnalyticsDao;
import com.learnloop.service.AnalyticsViews.Recommendation;
import com.learnloop.service.AnalyticsViews.StudentAnalytics;
import com.learnloop.service.AnalyticsViews.StudentClassPerformance;
import com.learnloop.service.AnalyticsViews.TeacherAnalytics;
import com.learnloop.service.AnalyticsViews.TeacherClassPerformance;
import com.learnloop.service.DashboardViews.StudentAssessmentRow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LearningAnalyticsService {

    private static final int MAX_RECOMMENDATIONS = 5;
    private final Database database;
    private final LearningAnalyticsDao analyticsDao;

    public LearningAnalyticsService(Database database) {
        this(database, new LearningAnalyticsDao());
    }

    public LearningAnalyticsService(Database database, LearningAnalyticsDao analyticsDao) {
        this.database = Objects.requireNonNull(database, "database");
        this.analyticsDao = Objects.requireNonNull(analyticsDao, "analyticsDao");
    }

    public StudentAnalytics studentAnalytics(long studentUserId, List<StudentAssessmentRow> availableAssessments) {
        if (studentUserId <= 0) {
            throw new IllegalArgumentException("studentUserId must be positive");
        }
        List<StudentAssessmentRow> assessments = List.copyOf(
                Objects.requireNonNull(availableAssessments, "availableAssessments"));
        StudentData data = database.withConnection(connection -> {
            LearningAnalyticsDao.StudentMetrics metrics =
                    analyticsDao.loadStudentMetrics(connection, studentUserId);
            List<StudentClassPerformance> classes =
                    analyticsDao.findStudentClassPerformance(connection, studentUserId);
            List<AnalyticsViews.RecentResult> recent =
                    analyticsDao.findRecentStudentResults(connection, studentUserId);
            return new StudentData(metrics, classes, recent);
        });
        List<Recommendation> recommendations = buildRecommendations(data.classes(), assessments);
        return new StudentAnalytics(
                data.metrics().gradedAttempts(),
                data.metrics().awaitingGrading(),
                data.metrics().averagePercentage(),
                data.classes(),
                data.recentResults(),
                recommendations);
    }

    public TeacherAnalytics teacherAnalytics(long teacherUserId) {
        if (teacherUserId <= 0) {
            throw new IllegalArgumentException("teacherUserId must be positive");
        }
        List<TeacherClassPerformance> classes =
                database.withConnection(connection -> analyticsDao.findTeacherClassPerformance(
                        connection, teacherUserId));
        return new TeacherAnalytics(classes);
    }

    public static List<Recommendation> buildRecommendations(
            List<StudentClassPerformance> classes,
            List<StudentAssessmentRow> availableAssessments) {
        Objects.requireNonNull(classes, "classes");
        Objects.requireNonNull(availableAssessments, "availableAssessments");
        List<Recommendation> result = new ArrayList<>();

        for (StudentAssessmentRow assessment : availableAssessments) {
            if (assessment.getAttemptsUsed() >= assessment.getMaxAttempts()) {
                continue;
            }
            boolean previouslyAttempted = assessment.getAttemptsUsed() > 0;
            result.add(new Recommendation(
                    (previouslyAttempted ? "Continue or review " : "Start ") + assessment.getTitle(),
                    "Available in " + assessment.getClassTitle()
                            + ". Open the assessment page to check instructions and your available attempts.",
                    "Assessment",
                    "/student/assessments/" + assessment.getId(),
                    assessment.getDueAt() == null ? "Available" : "Due date set"));
            if (result.size() >= 3) {
                break;
            }
        }

        for (StudentClassPerformance item : classes) {
            BigDecimal average = parsePercentage(item.getAveragePercentage());
            if (average == null || average.compareTo(BigDecimal.valueOf(70)) >= 0) {
                continue;
            }
            result.add(new Recommendation(
                    "Review materials for " + item.getClassTitle(),
                    "Your graded-assessment average in this class is below 70%. Revisit the published materials before your next attempt.",
                    "Study focus",
                    "/student/classes/" + item.getClassId() + "?tab=materials",
                    "Practice suggested"));
            if (result.size() >= MAX_RECOMMENDATIONS) {
                break;
            }
        }

        if (result.isEmpty()) {
            result.add(new Recommendation(
                    "Build your next study step",
                    "Open your learning area to review published materials, or check back when your teacher makes an assessment available.",
                    "Study plan",
                    "/student/learning",
                    "Suggested"));
        }
        return List.copyOf(result);
    }

    private static BigDecimal parsePercentage(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace("%", "");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record StudentData(
            LearningAnalyticsDao.StudentMetrics metrics,
            List<StudentClassPerformance> classes,
            List<AnalyticsViews.RecentResult> recentResults) {
    }
}
