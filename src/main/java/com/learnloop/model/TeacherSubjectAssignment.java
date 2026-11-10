package com.learnloop.model;

public record TeacherSubjectAssignment(
        long id,
        long teacherUserId,
        long subjectId,
        boolean active
) {
}
