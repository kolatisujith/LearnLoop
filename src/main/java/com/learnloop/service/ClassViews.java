package com.learnloop.service;

import java.util.Date;

public final class ClassViews {

    private ClassViews() {
    }

    public record AssignedSubject(long id, String code, String name) {

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

    public record ClassSummary(
            long id,
            String title,
            String subjectCode,
            String status,
            Integer maxStudents,
            long enrolledCount,
            long pendingRequestCount,
            long materialCount,
            long assessmentCount,
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

        public long getMaterialCount() {
            return materialCount;
        }

        public long getAssessmentCount() {
            return assessmentCount;
        }

        public Date getCreatedAt() {
            return createdAt;
        }
    }

    public record ClassDetails(
            long id,
            long subjectId,
            String subjectCode,
            String subjectName,
            String title,
            String description,
            String status,
            Integer maxStudents,
            long enrolledCount,
            long pendingRequestCount,
            long joinRequestCount,
            long materialCount,
            long assessmentCount,
            Date createdAt,
            Date updatedAt
    ) {

        public long getId() {
            return id;
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

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
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

        public long getJoinRequestCount() {
            return joinRequestCount;
        }

        public long getMaterialCount() {
            return materialCount;
        }

        public long getAssessmentCount() {
            return assessmentCount;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public Date getUpdatedAt() {
            return updatedAt;
        }
    }
}
