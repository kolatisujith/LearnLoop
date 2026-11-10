package com.learnloop.service;

import java.util.Date;

public final class InvitationViews {

    private InvitationViews() {
    }

    public record TeacherInvitation(
            long classId,
            String classTitle,
            String classStatus,
            String state,
            Date createdAt,
            Date expiresAt,
            Date revokedAt,
            boolean canGenerate,
            boolean canRegenerate,
            boolean canRevoke,
            boolean canExpire
    ) {

        public long getClassId() {
            return classId;
        }

        public String getClassTitle() {
            return classTitle;
        }

        public String getClassStatus() {
            return classStatus;
        }

        public String getState() {
            return state;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public Date getExpiresAt() {
            return expiresAt;
        }

        public Date getRevokedAt() {
            return revokedAt;
        }

        public boolean isCanGenerate() {
            return canGenerate;
        }

        public boolean isCanRegenerate() {
            return canRegenerate;
        }

        public boolean isCanRevoke() {
            return canRevoke;
        }

        public boolean isCanExpire() {
            return canExpire;
        }
    }

    public record PublicInvitation(
            long classId,
            String title,
            String description,
            String subjectCode,
            String subjectName,
            String teacherName,
            Integer maxStudents,
            long enrolledCount,
            Integer seatsRemaining,
            Date expiresAt,
            String viewerState,
            String viewerMessage,
            boolean canRequest
    ) {

        public long getClassId() {
            return classId;
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

        public Integer getSeatsRemaining() {
            return seatsRemaining;
        }

        public Date getExpiresAt() {
            return expiresAt;
        }

        public String getViewerState() {
            return viewerState;
        }

        public String getViewerMessage() {
            return viewerMessage;
        }

        public boolean isCanRequest() {
            return canRequest;
        }
    }

    public record IssuedInvitation(
            long classId,
            String classTitle,
            String token,
            String url,
            Date expiresAt
    ) {

        public long getClassId() {
            return classId;
        }

        public String getClassTitle() {
            return classTitle;
        }

        public String getToken() {
            return token;
        }

        public String getUrl() {
            return url;
        }

        public Date getExpiresAt() {
            return expiresAt;
        }
    }

    public record JoinRequestResult(long requestId, long classId, String classTitle) {

        public long getRequestId() {
            return requestId;
        }

        public long getClassId() {
            return classId;
        }

        public String getClassTitle() {
            return classTitle;
        }
    }
}
