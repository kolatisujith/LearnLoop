package com.learnloop.service;

import java.util.Date;
import java.util.List;

public final class DashboardViews {

    private DashboardViews() {
    }

    public record AdminOverview(
            long userCount,
            long pendingTeacherCount,
            long subjectCount,
            long classCount,
            long joinRequestCount,
            long pendingJoinRequestCount,
            List<AdminUserRow> pendingTeachers
    ) {

        public AdminOverview {
            pendingTeachers = List.copyOf(pendingTeachers);
        }

        public long getUserCount() {
            return userCount;
        }

        public long getPendingTeacherCount() {
            return pendingTeacherCount;
        }

        public long getSubjectCount() {
            return subjectCount;
        }

        public long getClassCount() {
            return classCount;
        }

        public long getJoinRequestCount() {
            return joinRequestCount;
        }

        public long getPendingJoinRequestCount() {
            return pendingJoinRequestCount;
        }

        public List<AdminUserRow> getPendingTeachers() {
            return pendingTeachers;
        }
    }

    public record AdminUserRow(
            long id,
            String fullName,
            String email,
            String role,
            String approvalStatus,
            String rejectionReason,
            Date createdAt
    ) {

        public long getId() {
            return id;
        }

        public String getFullName() {
            return fullName;
        }

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
        }

        public String getApprovalStatus() {
            return approvalStatus;
        }

        public String getRejectionReason() {
            return rejectionReason;
        }

        public Date getCreatedAt() {
            return createdAt;
        }
    }

    public record AdminSubjectRow(
            long id,
            String code,
            String name,
            String description,
            boolean active,
            long teacherCount,
            long classCount
    ) {

        public long getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean isActive() {
            return active;
        }

        public long getTeacherCount() {
            return teacherCount;
        }

        public long getClassCount() {
            return classCount;
        }
    }

    public record AdminAssignmentRow(
            long id,
            long teacherUserId,
            String teacherName,
            String teacherEmail,
            long subjectId,
            String subjectCode,
            String subjectName,
            boolean active,
            Date assignedAt
    ) {

        public long getId() {
            return id;
        }

        public long getTeacherUserId() {
            return teacherUserId;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public String getTeacherEmail() {
            return teacherEmail;
        }

        public long getSubjectId() {
            return subjectId;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public boolean isActive() {
            return active;
        }

        public Date getAssignedAt() {
            return assignedAt;
        }
    }

    public record AdminClassRow(
            long id,
            String title,
            String subjectCode,
            String teacherName,
            String status,
            Integer maxStudents,
            long enrolledCount,
            long pendingRequestCount,
            Date createdAt
    ) {

        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public String getStatus() {
            return status;
        }

        public Integer getMaxStudents() {
            return maxStudents;
        }

        public long getEnrolledCount() {
            return enrolledCount;
        }

        public long getPendingRequestCount() {
            return pendingRequestCount;
        }

        public Date getCreatedAt() {
            return createdAt;
        }
    }

    public record TeacherOverview(
            String approvalStatus,
            List<TeacherSubjectRow> subjects,
            List<TeacherClassRow> classes,
            List<TeacherJoinRequestRow> pendingRequests,
            long totalEnrolledStudents
    ) {

        public TeacherOverview {
            subjects = List.copyOf(subjects);
            classes = List.copyOf(classes);
            pendingRequests = List.copyOf(pendingRequests);
        }

        public String getApprovalStatus() {
            return approvalStatus;
        }

        public List<TeacherSubjectRow> getSubjects() {
            return subjects;
        }

        public List<TeacherClassRow> getClasses() {
            return classes;
        }

        public List<TeacherJoinRequestRow> getPendingRequests() {
            return pendingRequests;
        }

        public long getTotalEnrolledStudents() {
            return totalEnrolledStudents;
        }

        public int getSubjectCount() {
            return subjects.size();
        }

        public int subjectCount() {
            return getSubjectCount();
        }

        public int getClassCount() {
            return classes.size();
        }

        public int classCount() {
            return getClassCount();
        }

        public int getPendingRequestCount() {
            return pendingRequests.size();
        }

        public int pendingRequestCount() {
            return getPendingRequestCount();
        }
    }

    public record TeacherSubjectRow(long id, String code, String name) {

        public long getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
    }

    public record TeacherClassRow(
            long id,
            String title,
            String subjectCode,
            String status,
            Integer maxStudents,
            long enrolledCount,
            long pendingRequestCount,
            Date createdAt
    ) {

        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getStatus() {
            return status;
        }

        public Integer getMaxStudents() {
            return maxStudents;
        }

        public long getEnrolledCount() {
            return enrolledCount;
        }

        public long getPendingRequestCount() {
            return pendingRequestCount;
        }

        public Date getCreatedAt() {
            return createdAt;
        }
    }

    public record TeacherJoinRequestRow(
            long id,
            long classId,
            String classTitle,
            String studentName,
            String studentEmail,
            String message,
            Date createdAt
    ) {

        public long getId() {
            return id;
        }

        public long getClassId() {
            return classId;
        }

        public String getClassTitle() {
            return classTitle;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getStudentEmail() {
            return studentEmail;
        }

        public String getMessage() {
            return message;
        }

        public Date getCreatedAt() {
            return createdAt;
        }
    }

    public record TeacherMaterialRow(
            long id,
            String classTitle,
            String subjectCode,
            String title,
            String materialType,
            boolean published,
            Date createdAt
    ) {

        public long getId() {
            return id;
        }

        public String getClassTitle() {
            return classTitle;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getTitle() {
            return title;
        }

        public String getMaterialType() {
            return materialType;
        }

        public boolean isPublished() {
            return published;
        }

        public Date getCreatedAt() {
            return createdAt;
        }
    }

    public record TeacherAssessmentRow(
            long id,
            String classTitle,
            String subjectCode,
            String title,
            String status,
            Date availableFrom,
            Date dueAt,
            int maxAttempts,
            long attemptCount,
            long gradedCount
    ) {

        public long getId() {
            return id;
        }

        public String getClassTitle() {
            return classTitle;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getTitle() {
            return title;
        }

        public String getStatus() {
            return status;
        }

        public Date getAvailableFrom() {
            return availableFrom;
        }

        public Date getDueAt() {
            return dueAt;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public long getAttemptCount() {
            return attemptCount;
        }

        public long getGradedCount() {
            return gradedCount;
        }
    }

    public record TeacherProgressRow(
            long classId,
            String classTitle,
            long enrolledCount,
            long gradedAttempts,
            String averageScore
    ) {

        public long getClassId() {
            return classId;
        }

        public String getClassTitle() {
            return classTitle;
        }

        public long getEnrolledCount() {
            return enrolledCount;
        }

        public long getGradedAttempts() {
            return gradedAttempts;
        }

        public String getAverageScore() {
            return averageScore;
        }
    }

    public record StudentOverview(
            long enrolledClassCount,
            long pendingRequestCount,
            long availableAssessmentCount,
            long recentResultCount,
            List<StudentClassRow> classes,
            List<StudentJoinRequestRow> pendingRequests,
            List<StudentAssessmentRow> availableAssessments,
            List<StudentResultRow> recentResults
    ) {

        public StudentOverview {
            classes = List.copyOf(classes);
            pendingRequests = List.copyOf(pendingRequests);
            availableAssessments = List.copyOf(availableAssessments);
            recentResults = List.copyOf(recentResults);
        }

        public long getEnrolledClassCount() {
            return enrolledClassCount;
        }

        public long getPendingRequestCount() {
            return pendingRequestCount;
        }

        public long getAvailableAssessmentCount() {
            return availableAssessmentCount;
        }

        public long getRecentResultCount() {
            return recentResultCount;
        }

        public List<StudentClassRow> getClasses() {
            return classes;
        }

        public List<StudentJoinRequestRow> getPendingRequests() {
            return pendingRequests;
        }

        public List<StudentAssessmentRow> getAvailableAssessments() {
            return availableAssessments;
        }

        public List<StudentResultRow> getRecentResults() {
            return recentResults;
        }
    }

    public record StudentClassRow(
            long id,
            String title,
            String description,
            String subjectCode,
            String subjectName,
            String teacherName,
            String classStatus,
            String enrollmentStatus,
            Integer maxStudents,
            long enrolledCount,
            long materialCount,
            long assessmentCount,
            Date enrolledAt
    ) {

        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public String getClassStatus() {
            return classStatus;
        }

        public String getEnrollmentStatus() {
            return enrollmentStatus;
        }

        public Integer getMaxStudents() {
            return maxStudents;
        }

        public long getEnrolledCount() {
            return enrolledCount;
        }

        public long getMaterialCount() {
            return materialCount;
        }

        public long getAssessmentCount() {
            return assessmentCount;
        }

        public Date getEnrolledAt() {
            return enrolledAt;
        }
    }

    public record StudentJoinRequestRow(
            long id,
            String classTitle,
            String subjectCode,
            String status,
            String message,
            Date createdAt,
            long classId,
            String classStatus,
            String subjectName,
            String teacherName,
            Date reviewedAt
    ) {

        public StudentJoinRequestRow(
                long id,
                String classTitle,
                String subjectCode,
                String status,
                String message,
                Date createdAt) {
            this(id, classTitle, subjectCode, status, message, createdAt, 0L, null, null, null, null);
        }

        public long getId() {
            return id;
        }

        public String getClassTitle() {
            return classTitle;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public long getClassId() {
            return classId;
        }

        public String getClassStatus() {
            return classStatus;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public Date getReviewedAt() {
            return reviewedAt;
        }

        public String getStatusLabel() {
            return switch (status) {
                case "PENDING" -> "Pending";
                case "APPROVED", "ACCEPTED" -> "Accepted";
                case "REJECTED" -> "Rejected";
                case "CANCELLED" -> "Cancelled";
                default -> "Unknown";
            };
        }

        public String getStatusCssClass() {
            return switch (status) {
                case "PENDING" -> "ll-state-pending";
                case "APPROVED", "ACCEPTED" -> "ll-state-approved";
                case "REJECTED" -> "ll-state-rejected";
                default -> "ll-state-neutral";
            };
        }

        public boolean isPending() {
            return "PENDING".equals(status);
        }
    }

    public record StudentAssessmentRow(
            long id,
            String classTitle,
            String subjectCode,
            String title,
            Date availableFrom,
            Date dueAt,
            Integer timeLimitMinutes,
            int maxAttempts,
            long attemptsUsed
    ) {

        public long getId() {
            return id;
        }

        public String getClassTitle() {
            return classTitle;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getTitle() {
            return title;
        }

        public Date getAvailableFrom() {
            return availableFrom;
        }

        public Date getDueAt() {
            return dueAt;
        }

        public Integer getTimeLimitMinutes() {
            return timeLimitMinutes;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public long getAttemptsUsed() {
            return attemptsUsed;
        }
    }

    public record StudentResultRow(
            long attemptId,
            String assessmentTitle,
            String classTitle,
            String status,
            String score,
            Date submittedAt,
            Date gradedAt,
            String passingScore
    ) {

        public long getAttemptId() {
            return attemptId;
        }

        public String getAssessmentTitle() {
            return assessmentTitle;
        }

        public String getClassTitle() {
            return classTitle;
        }

        public String getStatus() {
            return status;
        }

        public String getScore() {
            return score;
        }

        public Date getSubmittedAt() {
            return submittedAt;
        }

        public Date getGradedAt() {
            return gradedAt;
        }

        public String getPassingScore() {
            return passingScore;
        }
    }

    public record StudentMaterialRow(
            long id,
            String classTitle,
            String subjectCode,
            String title,
            String description,
            String materialType,
            String contentText,
            String resourceUrl,
            Date createdAt
    ) {

        public long getId() {
            return id;
        }

        public String getClassTitle() {
            return classTitle;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getMaterialType() {
            return materialType;
        }

        public String getContentText() {
            return contentText;
        }

        public String getResourceUrl() {
            return resourceUrl;
        }

        public String getSafeResourceUrl() {
            if (resourceUrl == null) {
                return null;
            }
            String trimmed = resourceUrl.trim();
            if (trimmed.regionMatches(true, 0, "https://", 0, 8)
                    || trimmed.regionMatches(true, 0, "http://", 0, 7)) {
                return trimmed;
            }
            return null;
        }

        public Date getCreatedAt() {
            return createdAt;
        }
    }

    public record DiscoveryClassRow(
            long id,
            String title,
            String description,
            String subjectCode,
            String subjectName,
            String teacherName,
            Integer maxStudents,
            long enrolledCount,
            String requestStatus
    ) {

        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public Integer getMaxStudents() {
            return maxStudents;
        }

        public long getEnrolledCount() {
            return enrolledCount;
        }

        public String getRequestStatus() {
            return requestStatus;
        }
    }
}
