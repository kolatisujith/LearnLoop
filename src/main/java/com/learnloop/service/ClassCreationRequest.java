package com.learnloop.service;

import java.time.Instant;

public record ClassCreationRequest(
        long teacherUserId,
        long subjectId,
        String title,
        String description,
        Instant invitationExpiresAt,
        Integer maxStudents
) {
}
