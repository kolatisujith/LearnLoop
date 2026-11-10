package com.learnloop.service;

import com.learnloop.util.ExternalResourceUrls;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ClassroomViews {

    private ClassroomViews() {
    }

    public record Classroom(
            long id,
            long subjectId,
            String subjectCode,
            String subjectName,
            String title,
            String description,
            String status,
            long teacherUserId,
            String teacherName,
            Integer maxStudents,
            long enrolledCount,
            String meetingUrl,
            Date createdAt,
            Date updatedAt,
            List<Material> materials,
            List<Assessment> assessments
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

        public long getTeacherUserId() {
            return teacherUserId;
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

        public String getMeetingUrl() {
            return meetingUrl;
        }

        public boolean isLiveSessionConfigured() {
            return meetingUrl != null && !meetingUrl.isBlank();
        }

        public boolean liveSessionConfigured() {
            return isLiveSessionConfigured();
        }

        public boolean isJitsiSession() {
            return ExternalResourceUrls.isJitsiMeetUrl(meetingUrl);
        }

        public boolean jitsiSession() {
            return isJitsiSession();
        }

        public boolean isGoogleMeetSession() {
            return ExternalResourceUrls.isGoogleMeetUrl(meetingUrl);
        }

        public boolean googleMeetSession() {
            return isGoogleMeetSession();
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public Date getUpdatedAt() {
            return updatedAt;
        }

        public List<Material> getMaterials() {
            return materials;
        }

        public List<Assessment> getAssessments() {
            return assessments;
        }

        public long getMaterialCount() {
            return materials == null ? 0 : materials.size();
        }

        public long materialCount() {
            return getMaterialCount();
        }

        public long getAssessmentCount() {
            return assessments == null ? 0 : assessments.size();
        }

        public long assessmentCount() {
            return getAssessmentCount();
        }

        public long getPublishedMaterialCount() {
            return materials == null ? 0 : materials.stream().filter(Material::isPublished).count();
        }

        public long publishedMaterialCount() {
            return getPublishedMaterialCount();
        }

        public boolean isEditable() {
            return "DRAFT".equals(status) || "ACTIVE".equals(status);
        }

        public boolean isDraft() {
            return "DRAFT".equals(status);
        }

        public boolean isArchived() {
            return "ARCHIVED".equals(status) || "CANCELLED".equals(status);
        }
    }

    public record Material(
            long id,
            long classId,
            String classTitle,
            String subjectCode,
            String teacherName,
            long teacherUserId,
            String classStatus,
            String title,
            String description,
            String materialType,
            String resourceUrl,
            String contentText,
            String fileName,
            String storedName,
            String mimeType,
            Long fileSizeBytes,
            int position,
            boolean published,
            Date publishedAt,
            Date createdAt,
            Date updatedAt
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

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public long getTeacherUserId() {
            return teacherUserId;
        }

        public String getClassStatus() {
            return classStatus;
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

        public String getResourceUrl() {
            return resourceUrl;
        }

        public String getSafeResourceUrl() {
            return ExternalResourceUrls.safeHttpUrl(resourceUrl);
        }

        public String safeResourceUrl() {
            return getSafeResourceUrl();
        }

        public String getContentText() {
            return contentText;
        }

        public String getFileName() {
            return fileName;
        }

        public String getStoredName() {
            return storedName;
        }

        public String getMimeType() {
            return mimeType;
        }

        public Long getFileSizeBytes() {
            return fileSizeBytes;
        }

        public String getFileSizeLabel() {
            return formatFileSize(fileSizeBytes);
        }

        public String fileSizeLabel() {
            return getFileSizeLabel();
        }

        public int getPosition() {
            return position;
        }

        public boolean isPublished() {
            return published;
        }

        public Date getPublishedAt() {
            return publishedAt;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public Date getUpdatedAt() {
            return updatedAt;
        }

        public boolean isLink() {
            return "LINK".equals(materialType);
        }

        public boolean link() {
            return isLink();
        }

        public boolean isFile() {
            return "FILE".equals(materialType);
        }

        public boolean file() {
            return isFile();
        }

        public boolean isText() {
            return "TEXT".equals(materialType);
        }

        public boolean text() {
            return isText();
        }

        public String getTypeLabel() {
            if (isLink()) {
                return "Link";
            }
            if (isFile()) {
                return "File";
            }
            if (isText()) {
                return "Reading";
            }
            return materialType;
        }

        public String typeLabel() {
            return getTypeLabel();
        }
    }

    public record Assessment(
            long id,
            long classId,
            String title,
            String instructions,
            String status,
            Date availableFrom,
            Date dueAt,
            Integer timeLimitMinutes,
            int maxAttempts,
            boolean published
    ) {

        public long getId() {
            return id;
        }

        public long getClassId() {
            return classId;
        }

        public String getTitle() {
            return title;
        }

        public String getInstructions() {
            return instructions;
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

        public Integer getTimeLimitMinutes() {
            return timeLimitMinutes;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public boolean isPublished() {
            return published;
        }
    }

    public record MaterialAccess(Classroom classroom, Material material) {

        public Classroom getClassroom() {
            return classroom;
        }

        public Material getMaterial() {
            return material;
        }
    }

    public static String formatFileSize(Long bytes) {
        if (bytes == null || bytes < 0) {
            return "Unknown size";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB"};
        for (String unit : units) {
            value /= 1024.0;
            if (value < 1024.0 || "GB".equals(unit)) {
                return String.format(Locale.ROOT, "%.1f %s", value, unit);
            }
        }
        return bytes + " B";
    }
}
