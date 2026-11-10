package com.learnloop.model;

import java.time.Instant;

public record LearningClass(
        long id,
        long subjectId,
        long teacherUserId,
        String title,
        String description,
        String invitationTokenHash,
        Instant invitationTokenCreatedAt,
        Instant invitationTokenExpiresAt,
        Instant invitationTokenRevokedAt,
        String status,
        Integer maxStudents,
        Instant createdAt,
        Instant updatedAt
) {
}
