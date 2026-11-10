package com.learnloop.service;

import com.learnloop.service.AssessmentViews.Answer;
import com.learnloop.service.AssessmentViews.AssessmentDetail;
import com.learnloop.service.AssessmentViews.AssessmentSummary;
import com.learnloop.service.AssessmentViews.AttemptDetail;
import com.learnloop.service.AssessmentViews.Question;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentViewsTest {

    private AssessmentSummary assessment() {
        Date now = new Date();
        return new AssessmentSummary(
                11L, 7L, 3L, "Intro to Programming", "ACTIVE", "CS", "Maya Teacher",
                "Demo assessment", "Answer every question.", "PUBLISHED",
                null, null, null, 1, new BigDecimal("60.00"), true,
                2L, new BigDecimal("10.00"), now, now, true);
    }

    @Test
    void assessmentSummaryExposesRecordStylePropertiesUsedByJspViews() {
        AssessmentSummary assessment = assessment();

        assertEquals("Published", assessment.statusLabel());
        assertEquals("ll-state-approved", assessment.statusCssClass());
        assertFalse(assessment.draft());
        assertTrue(assessment.published());
        assertFalse(assessment.timeLimited());
        assertTrue(assessment.passingConfigured());
        assertEquals("60", assessment.passingScoreText());
        assertEquals("10", assessment.totalPointsText());
    }

    @Test
    void questionAndAnswerExposeRecordStylePropertiesUsedByJspViews() {
        Question choice = new Question(
                1L, 11L, "SINGLE_CHOICE", "What is 3 + 4?",
                new BigDecimal("5.00"), 1, true,
                List.of(new AssessmentViews.Option(1L, "7", true, 1)));
        Question written = new Question(
                2L, 11L, "SHORT_TEXT", "Explain a variable.",
                new BigDecimal("5.00"), 2, true, List.of());

        assertEquals("Single choice", choice.typeLabel());
        assertEquals("5", choice.pointsText());
        assertFalse(choice.shortText());
        assertTrue(written.shortText());

        Answer answer = new Answer(
                8L, 2L, "Explain a variable.", "SHORT_TEXT", new BigDecimal("5.00"),
                "A variable stores a value.", false, new BigDecimal("4.00"), List.of());
        assertTrue(answer.shortText());
        assertTrue(answer.incorrectAnswer());
        assertTrue(answer.answered());
        assertEquals("5", answer.maxPointsText());
        assertEquals("4", answer.pointsAwardedText());
    }

    @Test
    void attemptAndAssessmentDetailsExposeRecordStylePropertiesUsedByJspViews() {
        Date now = new Date();
        AssessmentSummary assessment = assessment();
        AttemptDetail graded = new AttemptDetail(
                20L, 11L, 21L, "Workflow Test Student", "workflow.student@example.test",
                1, "GRADED", now, now, now,
                new BigDecimal("9.00"), new BigDecimal("10.00"),
                new BigDecimal("90.00"), true, List.of());
        AttemptDetail submitted = new AttemptDetail(
                21L, 11L, 21L, "Workflow Test Student", "workflow.student@example.test",
                1, "SUBMITTED", now, now, null,
                null, new BigDecimal("10.00"), null, null, List.of());

        assertEquals("Graded", graded.statusLabel());
        assertEquals("ll-state-approved", graded.statusCssClass());
        assertEquals("9", graded.scoreText());
        assertEquals("10", graded.totalPointsText());
        assertEquals("90", graded.percentageText());
        assertEquals("Passed", graded.passLabel());
        assertTrue(graded.graded());
        assertTrue(submitted.awaitingGrading());

        AssessmentDetail teacherView = new AssessmentDetail(assessment, List.of(), List.of(submitted));
        assertEquals(1, teacherView.attemptCount());
        assertEquals(1L, teacherView.awaitingGradingCount());

        AssessmentViews.StudentAssessmentDetail studentView =
                new AssessmentViews.StudentAssessmentDetail(assessment, null, List.of(graded), true);
        assertEquals(1L, studentView.attemptsUsed());
        assertEquals(0L, studentView.remainingAttempts());
        assertFalse(studentView.hasInProgress());
    }
}
