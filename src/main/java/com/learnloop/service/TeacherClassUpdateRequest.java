package com.learnloop.service;

public record TeacherClassUpdateRequest(
        long teacherUserId,
        long classId,
        long subjectId,
        String title,
        String description,
        Integer maxStudents,
        String status
) {
}
