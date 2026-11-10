package com.learnloop.service;

import com.learnloop.dao.ClassDao;
import com.learnloop.dao.Database;
import com.learnloop.model.LearningClass;
import com.learnloop.support.MySqlTestDatabase;
import com.learnloop.util.InvitationTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassServiceIntegrationTest {

    private Database database;
    private ClassService classService;
    private long approvedTeacherId;
    private long pendingTeacherId;
    private long studentId;
    private long mathSubjectId;
    private long physicsSubjectId;

    @BeforeEach
    void setUp() {
        database = MySqlTestDatabase.database();
        classService = new ClassService(database);
        approvedTeacherId = userId("teacher@learnloop.local");
        pendingTeacherId = userId("pending.teacher@learnloop.local");
        studentId = userId("alice.student@learnloop.local");
        mathSubjectId = subjectId("MATH");
        physicsSubjectId = subjectId("PHYS");
    }

    @Test
    void createsDraftClassForApprovedAssignedTeacher() {
        String title = "Integration Class " + UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        long classId = -1;
        try {
            ClassCreationResult result = classService.createClass(new ClassCreationRequest(
                    approvedTeacherId,
                    mathSubjectId,
                    "  " + title + "  ",
                    "  Integration description  ",
                    expiresAt,
                    18));

            LearningClass created = result.learningClass();
            classId = created.id();
            assertTrue(classId > 0);
            assertEquals(approvedTeacherId, created.teacherUserId());
            assertEquals(mathSubjectId, created.subjectId());
            assertEquals(title, created.title());
            assertEquals("Integration description", created.description());
            assertEquals("DRAFT", created.status());
            assertEquals(18, created.maxStudents());
            assertNotNull(created.createdAt());
            assertNotNull(created.invitationTokenExpiresAt());
            assertTrue(Duration.between(expiresAt, created.invitationTokenExpiresAt()).abs().getSeconds() <= 1);

            String token = result.invitationToken();
            assertFalse(token.isBlank());
            assertEquals(InvitationTokens.sha256Hex(token), created.invitationTokenHash());
            assertFalse(created.invitationTokenHash().contains(token));

            long foundId = database.withConnection(connection ->
                    new ClassDao().findByInvitationTokenHash(connection, created.invitationTokenHash())
                            .map(LearningClass::id)
                            .orElse(-1L));

            assertEquals(classId, foundId);
            assertEquals(1L, MySqlTestDatabase.queryLong(database,
                    "SELECT COUNT(*) FROM audit_logs WHERE action = 'CLASS_CREATED' "
                            + "AND entity_type = 'CLASS' AND entity_id = ?",
                    classId));
        } finally {
            if (classId > 0) {
                MySqlTestDatabase.deleteClass(database, classId);
            }
        }
    }

    @Test
    void rejectsPendingTeacherEvenWhenAssignedToSubject() {
        String title = "Pending Teacher Class " + UUID.randomUUID();

        ClassCreationException exception = assertThrows(ClassCreationException.class,
                () -> classService.createClass(new ClassCreationRequest(
                        pendingTeacherId,
                        physicsSubjectId,
                        title,
                        null,
                        Instant.now().plus(1, ChronoUnit.DAYS),
                        10)));

        assertTrue(exception.getMessage().contains("approved"));
        assertEquals(0L, classCountByTitle(title));
    }

    @Test
    void rejectsTeacherWhoIsNotAssignedToSubject() {
        String title = "Unassigned Subject Class " + UUID.randomUUID();

        ClassCreationException exception = assertThrows(ClassCreationException.class,
                () -> classService.createClass(new ClassCreationRequest(
                        approvedTeacherId,
                        physicsSubjectId,
                        title,
                        null,
                        Instant.now().plus(1, ChronoUnit.DAYS),
                        10)));

        assertTrue(exception.getMessage().contains("assigned"));
        assertEquals(0L, classCountByTitle(title));
    }

    @Test
    void rejectsNonTeacherAccount() {
        String title = "Student Class " + UUID.randomUUID();

        ClassCreationException exception = assertThrows(ClassCreationException.class,
                () -> classService.createClass(new ClassCreationRequest(
                        studentId,
                        mathSubjectId,
                        title,
                        null,
                        null,
                        10)));

        assertTrue(exception.getMessage().contains("teacher"));
        assertEquals(0L, classCountByTitle(title));
    }

    private long userId(String email) {
        return MySqlTestDatabase.queryLong(database, "SELECT id FROM users WHERE email = ?", email);
    }

    private long subjectId(String code) {
        return MySqlTestDatabase.queryLong(database, "SELECT id FROM subjects WHERE code = ?", code);
    }

    private long classCountByTitle(String title) {
        return MySqlTestDatabase.queryLong(database, "SELECT COUNT(*) FROM classes WHERE title = ?", title);
    }
}
