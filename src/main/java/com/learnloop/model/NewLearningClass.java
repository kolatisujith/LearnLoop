package com.learnloop.model;

import java.time.Instant;

public record NewLearningClass(
        long subjectId,
        long teacherUserId,
        String title,
        String description,
        String invitationTokenHash,
        Instant invitationTokenCreatedAt,
        Instant invitationTokenExpiresAt,
        Instant invitationTokenRevokedAt,
        String status,
        Integer maxStudents
) {
}
