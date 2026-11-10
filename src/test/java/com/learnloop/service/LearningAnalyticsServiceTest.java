package com.learnloop.service;

import com.learnloop.service.AnalyticsViews.Recommendation;
import com.learnloop.service.AnalyticsViews.StudentClassPerformance;
import com.learnloop.service.DashboardViews.StudentAssessmentRow;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningAnalyticsServiceTest {

    @Test
    void recommendsAvailableAssessmentAndLowPerformingClassReview() {
        StudentAssessmentRow assessment = new StudentAssessmentRow(
                18, "Java Basics", "JAVA", "Variables quiz", null, new Date(), 20, 2, 0);
        List<StudentClassPerformance> classes = List.of(
                new StudentClassPerformance(7, "Java Basics", 1, 3, "62.5%"),
                new StudentClassPerformance(8, "Database Fundamentals", 0, 2, "86.0%"));

        List<Recommendation> recommendations =
                LearningAnalyticsService.buildRecommendations(classes, List.of(assessment));

        assertEquals(2, recommendations.size());
        assertTrue(recommendations.get(0).getTitle().contains("Variables quiz"));
        assertEquals("/student/assessments/18", recommendations.get(0).getHref());
        assertTrue(recommendations.get(1).getTitle().contains("Java Basics"));
        assertTrue(recommendations.get(1).getDetail().contains("below 70%"));
        assertEquals("/student/classes/7?tab=materials", recommendations.get(1).getHref());
    }

    @Test
    void doesNotRecommendAssessmentWhenAttemptLimitIsUsed() {
        StudentAssessmentRow exhausted = new StudentAssessmentRow(
                22, "Networks", "CN", "Network quiz", null, null, null, 1, 1);

        List<Recommendation> recommendations =
                LearningAnalyticsService.buildRecommendations(List.of(), List.of(exhausted));

        assertEquals(1, recommendations.size());
        assertEquals("Study plan", recommendations.get(0).getCategory());
        assertEquals("/student/learning", recommendations.get(0).getHref());
        assertFalse(recommendations.get(0).getTitle().contains("Network quiz"));
    }

    @Test
    void ignoresMissingOrHealthyAveragesWhenCreatingPracticeRecommendations() {
        List<StudentClassPerformance> classes = List.of(
                new StudentClassPerformance(2, "No results", 1, 0, null),
                new StudentClassPerformance(3, "Strong results", 0, 4, "70.0%"));

        List<Recommendation> recommendations =
                LearningAnalyticsService.buildRecommendations(classes, List.of());

        assertEquals(1, recommendations.size());
        assertEquals("Study plan", recommendations.get(0).getCategory());
    }
}
