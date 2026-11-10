package com.learnloop.service;

public record TeacherClassCreateRequest(
        long teacherUserId,
        long subjectId,
        String title,
        String description,
        Integer maxStudents
) {
}
