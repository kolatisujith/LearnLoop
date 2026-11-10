package com.learnloop.service;

import java.util.List;

public final class AnalyticsViews {

    private AnalyticsViews() {
    }

    public record StudentAnalytics(
            long gradedAttempts,
            long awaitingGrading,
            String averagePercentage,
            List<StudentClassPerformance> classes,
            List<RecentResult> recentResults,
            List<Recommendation> recommendations) {

        public StudentAnalytics {
            classes = List.copyOf(classes);
            recentResults = List.copyOf(recentResults);
            recommendations = List.copyOf(recommendations);
        }

        public long getGradedAttempts() { return gradedAttempts; }
        public long getAwaitingGrading() { return awaitingGrading; }
        public String getAveragePercentage() { return averagePercentage; }
        public List<StudentClassPerformance> getClasses() { return classes; }
        public List<RecentResult> getRecentResults() { return recentResults; }
        public List<Recommendation> getRecommendations() { return recommendations; }
    }

    public record StudentClassPerformance(
            long classId,
            String classTitle,
            long availableAssessments,
            long gradedAttempts,
            String averagePercentage) {

        public long getClassId() { return classId; }
        public String getClassTitle() { return classTitle; }
        public long getAvailableAssessments() { return availableAssessments; }
        public long getGradedAttempts() { return gradedAttempts; }
        public String getAveragePercentage() { return averagePercentage; }
    }

    public record TeacherAnalytics(List<TeacherClassPerformance> classes) {
        public TeacherAnalytics {
            classes = List.copyOf(classes);
        }

        public List<TeacherClassPerformance> getClasses() { return classes; }
        public long getClassCount() { return classes.size(); }
    }

    public record TeacherClassPerformance(
            long classId,
            String classTitle,
            long enrolledStudents,
            long publishedAssessments,
            long gradedAttempts,
            long awaitingGrading,
            String averagePercentage,
            String passRate) {

        public long getClassId() { return classId; }
        public String getClassTitle() { return classTitle; }
        public long getEnrolledStudents() { return enrolledStudents; }
        public long getPublishedAssessments() { return publishedAssessments; }
        public long getGradedAttempts() { return gradedAttempts; }
        public long getAwaitingGrading() { return awaitingGrading; }
        public String getAveragePercentage() { return averagePercentage; }
        public String getPassRate() { return passRate; }
        public boolean isHasGradedAttempts() { return gradedAttempts > 0; }
        public boolean isHasAwaitingGrading() { return awaitingGrading > 0; }
        public boolean isHasPassRate() { return passRate != null; }
    }

    public record RecentResult(
            long attemptId,
            String assessmentTitle,
            String classTitle,
            String statusLabel,
            String percentage,
            String outcomeLabel,
            java.util.Date submittedAt,
            java.util.Date gradedAt) {

        public long getAttemptId() { return attemptId; }
        public String getAssessmentTitle() { return assessmentTitle; }
        public String getClassTitle() { return classTitle; }
        public String getStatusLabel() { return statusLabel; }
        public String getPercentage() { return percentage; }
        public String getOutcomeLabel() { return outcomeLabel; }
        public java.util.Date getSubmittedAt() { return submittedAt; }
        public java.util.Date getGradedAt() { return gradedAt; }
        public boolean isHasPercentage() { return percentage != null; }
    }

    public record Recommendation(
            String title,
            String detail,
            String category,
            String href,
            String priorityLabel) {

        public String getTitle() { return title; }
        public String getDetail() { return detail; }
        public String getCategory() { return category; }
        public String getHref() { return href; }
        public String getPriorityLabel() { return priorityLabel; }
    }
}
