package com.learnloop.service;

import com.learnloop.dao.Database;
import com.learnloop.dao.TeacherSubjectAssignmentDao;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.LearningClass;
import com.learnloop.model.NewUser;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;
import com.learnloop.support.MySqlTestDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassManagementServiceIntegrationTest {

    private static final ClientContext CLIENT = new ClientContext("127.0.0.1", "JUnit");

    private Database database;
    private ClassService service;
    private final Set<Long> classIds = new LinkedHashSet<>();
    private final Set<Long> userIds = new LinkedHashSet<>();
    private long approvedTeacherId;
    private long pendingTeacherId;
    private long studentId;
    private long secondStudentId;
    private long mathSubjectId;
    private long csSubjectId;
    private long physicsSubjectId;

    @BeforeEach
    void setUp() {
        database = MySqlTestDatabase.database();
        service = new ClassService(database);
        approvedTeacherId = userId("teacher@learnloop.local");
        pendingTeacherId = userId("pending.teacher@learnloop.local");
        studentId = userId("alice.student@learnloop.local");
        secondStudentId = userId("bob.student@learnloop.local");
        mathSubjectId = subjectId("MATH");
        csSubjectId = subjectId("CS");
        physicsSubjectId = subjectId("PHYS");
    }

    @AfterEach
    void cleanUp() {
        for (long classId : classIds) {
            MySqlTestDatabase.deleteClass(database, classId);
        }
        for (long userId : userIds) {
            MySqlTestDatabase.execute(database, "DELETE FROM audit_logs WHERE actor_user_id = ?", userId);
            MySqlTestDatabase.execute(database,
                    "DELETE FROM teacher_subject_assignments WHERE teacher_user_id = ?", userId);
            MySqlTestDatabase.execute(database, "DELETE FROM users WHERE id = ?", userId);
        }
    }

    @Test
    void createsDraftAndListsOwnedClassWithLiveCounts() {
        String title = "Managed Class " + UUID.randomUUID();

        LearningClass created = service.createManagedClass(new TeacherClassCreateRequest(
                approvedTeacherId,
                mathSubjectId,
                "  " + title + "  ",
                "  Managed description  ",
                20), CLIENT);
        classIds.add(created.id());

        assertEquals("DRAFT", created.status());
        assertEquals(approvedTeacherId, created.teacherUserId());
        assertEquals(mathSubjectId, created.subjectId());
        assertEquals(title, created.title());
        assertEquals("Managed description", created.description());
        assertEquals(20, created.maxStudents());

        ClassViews.ClassSummary summary = service.findOwnedClasses(approvedTeacherId).stream()
                .filter(item -> item.getId() == created.id())
                .findFirst()
                .orElseThrow();
        assertEquals(0L, summary.getEnrolledCount());
        assertEquals(0L, summary.getPendingRequestCount());
        assertEquals(0L, summary.getMaterialCount());
        assertEquals(0L, summary.getAssessmentCount());

        ClassViews.ClassDetails details = service.findOwnedClass(approvedTeacherId, created.id());
        assertEquals("MATH", details.getSubjectCode());
        assertEquals("Mathematics", details.getSubjectName());
        assertEquals(0L, details.getJoinRequestCount());
        assertEquals(20, details.getMaxStudents());
        assertEquals(1L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE actor_user_id = ? AND action = 'CLASS_CREATED'
                  AND entity_type = 'CLASS' AND entity_id = ?
                """, approvedTeacherId, created.id()));
    }

    @Test
    void rejectsPendingTeachersUnassignedSubjectsAndInvalidCapacity() {
        assertThrows(ClassAccessException.class,
                () -> service.createManagedClass(new TeacherClassCreateRequest(
                        pendingTeacherId, physicsSubjectId, "Pending teacher class", null, 10), CLIENT));
        assertThrows(ClassAccessException.class,
                () -> service.findOwnedClasses(pendingTeacherId));
        assertThrows(ClassAccessException.class,
                () -> service.createManagedClass(new TeacherClassCreateRequest(
                        studentId, mathSubjectId, "Student class", null, 10), CLIENT));

        ValidationException unassigned = assertThrows(ValidationException.class,
                () -> service.createManagedClass(new TeacherClassCreateRequest(
                        approvedTeacherId, physicsSubjectId, "Unassigned subject", null, 10), CLIENT));
        assertTrue(unassigned.fieldErrors().containsKey("subjectId"));

        ValidationException invalidSubject = assertThrows(ValidationException.class,
                () -> service.createManagedClass(new TeacherClassCreateRequest(
                        approvedTeacherId, 0, "Invalid subject", null, 10), CLIENT));
        assertTrue(invalidSubject.fieldErrors().containsKey("subjectId"));

        ValidationException invalidCapacity = assertThrows(ValidationException.class,
                () -> service.createManagedClass(new TeacherClassCreateRequest(
                        approvedTeacherId, mathSubjectId, "Invalid capacity", null, 0), CLIENT));
        assertTrue(invalidCapacity.fieldErrors().containsKey("maxStudents"));
        assertEquals(0L, MySqlTestDatabase.queryLong(
                database, "SELECT COUNT(*) FROM classes WHERE title IN (?, ?, ?, ?)",
                "Pending teacher class", "Student class", "Unassigned subject", "Invalid capacity"));
    }

    @Test
    void anotherApprovedTeacherCannotReadUpdateArchiveOrDeleteOwnedClass() {
        long otherTeacherId = createApprovedTeacher();
        assign(otherTeacherId, mathSubjectId);
        String otherTitle = "Other teacher class " + UUID.randomUUID();
        LearningClass otherClass = service.createManagedClass(new TeacherClassCreateRequest(
                otherTeacherId, mathSubjectId, otherTitle, null, 10), CLIENT);
        classIds.add(otherClass.id());

        assertTrue(service.findOwnedClasses(approvedTeacherId).stream()
                .noneMatch(item -> item.getId() == otherClass.id()));
        assertThrows(ClassNotFoundException.class,
                () -> service.findOwnedClass(approvedTeacherId, otherClass.id()));
        assertThrows(ClassNotFoundException.class,
                () -> service.updateClass(new TeacherClassUpdateRequest(
                        approvedTeacherId,
                        otherClass.id(),
                        mathSubjectId,
                        "Hijacked class",
                        null,
                        5,
                        "ACTIVE"), CLIENT));
        assertThrows(ClassNotFoundException.class,
                () -> service.archiveClass(approvedTeacherId, otherClass.id(), CLIENT));
        assertThrows(ClassNotFoundException.class,
                () -> service.deleteClass(approvedTeacherId, otherClass.id(), CLIENT));

        assertEquals(otherTitle,
                MySqlTestDatabase.queryString(database,
                        "SELECT title FROM classes WHERE id = ?", otherClass.id()));
        assertEquals("DRAFT",
                MySqlTestDatabase.queryString(database, "SELECT status FROM classes WHERE id = ?", otherClass.id()));
        assertEquals(otherTeacherId,
                MySqlTestDatabase.queryLong(database,
                        "SELECT teacher_user_id FROM classes WHERE id = ?", otherClass.id()));
    }

    @Test
    void updatesOwnedClassButCapacityCannotDropBelowEnrollment() {
        LearningClass created = service.createManagedClass(new TeacherClassCreateRequest(
                approvedTeacherId, mathSubjectId, "Capacity class " + UUID.randomUUID(), null, 10), CLIENT);
        classIds.add(created.id());
        MySqlTestDatabase.execute(database, """
                INSERT INTO class_enrollments (class_id, student_user_id, enrolled_by_user_id, status)
                VALUES (?, ?, ?, 'ACTIVE')
                """, created.id(), studentId, approvedTeacherId);
        MySqlTestDatabase.execute(database, """
                INSERT INTO class_enrollments (class_id, student_user_id, enrolled_by_user_id, status)
                VALUES (?, ?, ?, 'ACTIVE')
                """, created.id(), secondStudentId, approvedTeacherId);

        ValidationException capacity = assertThrows(ValidationException.class,
                () -> service.updateClass(new TeacherClassUpdateRequest(
                        approvedTeacherId,
                        created.id(),
                        mathSubjectId,
                        created.title(),
                        null,
                        1,
                        "ACTIVE"), CLIENT));
        assertTrue(capacity.fieldErrors().containsKey("maxStudents"));
        assertEquals("DRAFT",
                MySqlTestDatabase.queryString(database, "SELECT status FROM classes WHERE id = ?", created.id()));

        service.updateClass(new TeacherClassUpdateRequest(
                approvedTeacherId,
                created.id(),
                csSubjectId,
                "  Updated capacity class  ",
                "  Updated description  ",
                5,
                "active"), CLIENT);

        ClassViews.ClassDetails updated = service.findOwnedClass(approvedTeacherId, created.id());
        assertEquals("Updated capacity class", updated.getTitle());
        assertEquals("Updated description", updated.getDescription());
        assertEquals("CS", updated.getSubjectCode());
        assertEquals("ACTIVE", updated.getStatus());
        assertEquals(5, updated.getMaxStudents());
        assertEquals(2L, updated.getEnrolledCount());
        assertEquals(1L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE actor_user_id = ? AND action = 'CLASS_UPDATED'
                  AND entity_type = 'CLASS' AND entity_id = ?
                """, approvedTeacherId, created.id()));

        service.archiveClass(approvedTeacherId, created.id(), CLIENT);
        assertEquals("ARCHIVED", service.findOwnedClass(approvedTeacherId, created.id()).getStatus());
        assertThrows(ValidationException.class,
                () -> service.updateClass(new TeacherClassUpdateRequest(
                        approvedTeacherId,
                        created.id(),
                        csSubjectId,
                        "Archived edit",
                        null,
                        5,
                        "ACTIVE"), CLIENT));
        assertThrows(ValidationException.class,
                () -> service.deleteClass(approvedTeacherId, created.id(), CLIENT));
    }

    @Test
    void deletesOnlyEmptyDraftsAndArchivesClassesWithHistory() {
        LearningClass emptyDraft = service.createManagedClass(new TeacherClassCreateRequest(
                approvedTeacherId, mathSubjectId, "Empty draft " + UUID.randomUUID(), null, 10), CLIENT);
        classIds.add(emptyDraft.id());

        service.deleteClass(approvedTeacherId, emptyDraft.id(), CLIENT);
        assertThrows(ClassNotFoundException.class,
                () -> service.findOwnedClass(approvedTeacherId, emptyDraft.id()));
        assertEquals(0L, MySqlTestDatabase.queryLong(
                database, "SELECT COUNT(*) FROM classes WHERE id = ?", emptyDraft.id()));

        LearningClass draftWithStudent = service.createManagedClass(new TeacherClassCreateRequest(
                approvedTeacherId, mathSubjectId, "Draft with student " + UUID.randomUUID(), null, 10), CLIENT);
        classIds.add(draftWithStudent.id());
        MySqlTestDatabase.execute(database, """
                INSERT INTO class_enrollments (class_id, student_user_id, enrolled_by_user_id, status)
                VALUES (?, ?, ?, 'ACTIVE')
                """, draftWithStudent.id(), studentId, approvedTeacherId);

        ValidationException delete = assertThrows(ValidationException.class,
                () -> service.deleteClass(approvedTeacherId, draftWithStudent.id(), CLIENT));
        assertTrue(delete.fieldErrors().containsKey("classId"));
        assertEquals("DRAFT", MySqlTestDatabase.queryString(
                database, "SELECT status FROM classes WHERE id = ?", draftWithStudent.id()));

        service.archiveClass(approvedTeacherId, draftWithStudent.id(), CLIENT);
        assertEquals("ARCHIVED", MySqlTestDatabase.queryString(
                database, "SELECT status FROM classes WHERE id = ?", draftWithStudent.id()));
        assertEquals(1L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE actor_user_id = ? AND action = 'CLASS_ARCHIVED'
                  AND entity_type = 'CLASS' AND entity_id = ?
                """, approvedTeacherId, draftWithStudent.id()));
    }

    private long createApprovedTeacher() {
        String email = "phase6-" + UUID.randomUUID() + "@learnloop.test";
        long userId = database.inTransaction(connection -> new UserDao().insert(connection, new NewUser(
                email,
                "integration-test-hash",
                "Phase Six Teacher",
                UserRole.TEACHER,
                ApprovalStatus.APPROVED,
                Instant.now(),
                null)));
        userIds.add(userId);
        return userId;
    }

    private void assign(long teacherId, long subjectId) {
        database.inTransaction(connection -> {
            new TeacherSubjectAssignmentDao().upsertActive(connection, teacherId, subjectId, approvedTeacherId);
            return null;
        });
    }

    private long userId(String email) {
        return MySqlTestDatabase.queryLong(database, "SELECT id FROM users WHERE email = ?", email);
    }

    private long subjectId(String code) {
        return MySqlTestDatabase.queryLong(database, "SELECT id FROM subjects WHERE code = ?", code);
    }
}
