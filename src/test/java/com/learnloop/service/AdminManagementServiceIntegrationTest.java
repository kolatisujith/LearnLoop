package com.learnloop.service;

import com.learnloop.dao.Database;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.NewUser;
import com.learnloop.model.Subject;
import com.learnloop.model.TeacherSubjectAssignment;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminManagementServiceIntegrationTest {

    private static final ClientContext CLIENT = new ClientContext("127.0.0.1", "JUnit");

    private Database database;
    private AdminManagementService service;
    private final Set<Long> userIds = new LinkedHashSet<>();
    private final Set<Long> subjectIds = new LinkedHashSet<>();
    private long adminId;

    @BeforeEach
    void setUp() {
        database = MySqlTestDatabase.database();
        service = new AdminManagementService(database);
        adminId = createUser(UserRole.ADMIN, ApprovalStatus.APPROVED);
    }

    @AfterEach
    void cleanUp() {
        for (long userId : userIds) {
            MySqlTestDatabase.execute(database,
                    "DELETE FROM audit_logs WHERE actor_user_id = ?", userId);
            MySqlTestDatabase.execute(database,
                    "DELETE FROM audit_logs WHERE entity_type = 'USER' AND entity_id = ?", userId);
            MySqlTestDatabase.execute(database, """
                    DELETE FROM audit_logs
                    WHERE entity_type = 'TEACHER_SUBJECT_ASSIGNMENT'
                      AND entity_id IN (
                          SELECT id FROM teacher_subject_assignments WHERE teacher_user_id = ?
                      )
                    """, userId);
        }
        for (long subjectId : subjectIds) {
            MySqlTestDatabase.execute(database, """
                    DELETE FROM audit_logs
                    WHERE entity_type = 'TEACHER_SUBJECT_ASSIGNMENT'
                      AND entity_id IN (
                          SELECT id FROM teacher_subject_assignments WHERE subject_id = ?
                      )
                    """, subjectId);
            MySqlTestDatabase.execute(database,
                    "DELETE FROM audit_logs WHERE entity_type = 'SUBJECT' AND entity_id = ?", subjectId);
        }
        for (long subjectId : subjectIds) {
            MySqlTestDatabase.execute(database,
                    "DELETE FROM teacher_subject_assignments WHERE subject_id = ?", subjectId);
            MySqlTestDatabase.execute(database, "DELETE FROM subjects WHERE id = ?", subjectId);
        }
        for (long userId : userIds) {
            MySqlTestDatabase.execute(database,
                    "DELETE FROM teacher_subject_assignments WHERE teacher_user_id = ?", userId);
            MySqlTestDatabase.execute(database, "DELETE FROM users WHERE id = ?", userId);
        }
    }

    @Test
    void approvesPendingTeacherAndAuditsTheDecision() {
        long teacherId = createUser(UserRole.TEACHER, ApprovalStatus.PENDING);

        service.approveTeacher(adminId, teacherId, CLIENT);

        assertEquals("APPROVED", userStatus(teacherId));
        assertEquals(adminId, MySqlTestDatabase.queryLong(database,
                "SELECT approved_by_user_id FROM users WHERE id = ?", teacherId));
        assertNotNull(MySqlTestDatabase.queryString(database,
                "SELECT approved_at FROM users WHERE id = ?", teacherId));
        assertEquals(1L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE actor_user_id = ? AND action = 'USER_APPROVED'
                  AND entity_type = 'USER' AND entity_id = ?
                """, adminId, teacherId));
    }

    @Test
    void rejectsPendingTeacherWithTrimmedReason() {
        long teacherId = createUser(UserRole.TEACHER, ApprovalStatus.PENDING);

        service.rejectTeacher(adminId, teacherId, "  Experience could not be verified.  ", CLIENT);

        assertEquals("REJECTED", userStatus(teacherId));
        assertEquals("Experience could not be verified.", MySqlTestDatabase.queryString(database,
                "SELECT rejection_reason FROM users WHERE id = ?", teacherId));
        assertNull(MySqlTestDatabase.queryString(database,
                "SELECT approved_at FROM users WHERE id = ?", teacherId));
        assertEquals(1L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE actor_user_id = ? AND action = 'USER_REJECTED'
                  AND entity_type = 'USER' AND entity_id = ?
                """, adminId, teacherId));
    }

    @Test
    void requiresARejectionReasonAndLeavesTeacherPending() {
        long teacherId = createUser(UserRole.TEACHER, ApprovalStatus.PENDING);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.rejectTeacher(adminId, teacherId, "   ", CLIENT));

        assertTrue(exception.fieldErrors().containsKey("reason"));
        assertEquals("PENDING", userStatus(teacherId));
        assertNull(MySqlTestDatabase.queryString(database,
                "SELECT rejection_reason FROM users WHERE id = ?", teacherId));
    }

    @Test
    void rejectsTeacherReviewWhenCallerIsNotAnAdmin() {
        long approvedTeacherId = createUser(UserRole.TEACHER, ApprovalStatus.APPROVED);
        long pendingTeacherId = createUser(UserRole.TEACHER, ApprovalStatus.PENDING);

        assertThrows(AdminAuthorizationException.class,
                () -> service.approveTeacher(approvedTeacherId, pendingTeacherId, CLIENT));

        assertEquals("PENDING", userStatus(pendingTeacherId));
        assertEquals(0L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE actor_user_id = ? AND action = 'USER_APPROVED'
                """, approvedTeacherId));
    }

    @Test
    void createsNormalizesUpdatesAndDeactivatesSubject() {
        String code = uniqueCode();
        String name = "Test Subject " + UUID.randomUUID();

        long subjectId = service.createSubject(
                adminId, "  " + code.toLowerCase() + "  ", "  " + name + "  ", "  Course details  ", CLIENT);
        subjectIds.add(subjectId);

        Subject created = findSubject(subjectId);
        assertEquals(code, created.code());
        assertEquals(name, created.name());
        assertEquals("Course details", created.description());
        assertTrue(created.active());
        assertEquals(1L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE actor_user_id = ? AND action = 'SUBJECT_CREATED'
                  AND entity_type = 'SUBJECT' AND entity_id = ?
                """, adminId, subjectId));

        ValidationException duplicate = assertThrows(ValidationException.class,
                () -> service.createSubject(adminId, code, name + " duplicate", null, CLIENT));
        assertTrue(duplicate.fieldErrors().containsKey("code"));

        String updatedName = name + " Updated";
        service.updateSubject(adminId, subjectId, code + "-X", updatedName, null, CLIENT);
        Subject updated = findSubject(subjectId);
        assertEquals(code + "-X", updated.code());
        assertEquals(updatedName, updated.name());
        assertNull(updated.description());

        service.setSubjectActive(adminId, subjectId, false, CLIENT);
        assertFalse(findSubject(subjectId).active());
        assertEquals(1L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE actor_user_id = ? AND action = 'SUBJECT_DEACTIVATED'
                  AND entity_type = 'SUBJECT' AND entity_id = ?
                """, adminId, subjectId));

        service.setSubjectActive(adminId, subjectId, true, CLIENT);
        assertTrue(findSubject(subjectId).active());
    }

    @Test
    void assignmentRequiresApprovedTeacherAndActiveSubject() {
        long approvedTeacherId = createUser(UserRole.TEACHER, ApprovalStatus.APPROVED);
        long pendingTeacherId = createUser(UserRole.TEACHER, ApprovalStatus.PENDING);
        long subjectId = createSubject();
        long inactiveSubjectId = createSubject();
        service.setSubjectActive(adminId, inactiveSubjectId, false, CLIENT);

        ValidationException pending = assertThrows(ValidationException.class,
                () -> service.assignTeacher(adminId, pendingTeacherId, subjectId, CLIENT));
        assertTrue(pending.fieldErrors().containsKey("teacherId"));

        ValidationException inactive = assertThrows(ValidationException.class,
                () -> service.assignTeacher(adminId, approvedTeacherId, inactiveSubjectId, CLIENT));
        assertTrue(inactive.fieldErrors().containsKey("subjectId"));

        service.assignTeacher(adminId, approvedTeacherId, subjectId, CLIENT);
        TeacherSubjectAssignment assignment = findAssignment(approvedTeacherId, subjectId);
        assertTrue(assignment.active());

        ValidationException duplicate = assertThrows(ValidationException.class,
                () -> service.assignTeacher(adminId, approvedTeacherId, subjectId, CLIENT));
        assertTrue(duplicate.fieldErrors().containsKey("teacherId"));
    }

    @Test
    void removalDeactivatesAssignmentAndReassignmentReusesTheRow() {
        long approvedTeacherId = createUser(UserRole.TEACHER, ApprovalStatus.APPROVED);
        long subjectId = createSubject();

        service.assignTeacher(adminId, approvedTeacherId, subjectId, CLIENT);
        TeacherSubjectAssignment first = findAssignment(approvedTeacherId, subjectId);
        assertTrue(first.active());

        service.removeAssignment(adminId, approvedTeacherId, subjectId, CLIENT);
        TeacherSubjectAssignment removed = findAssignment(approvedTeacherId, subjectId);
        assertEquals(first.id(), removed.id());
        assertFalse(removed.active());

        service.assignTeacher(adminId, approvedTeacherId, subjectId, CLIENT);
        TeacherSubjectAssignment restored = findAssignment(approvedTeacherId, subjectId);
        assertEquals(first.id(), restored.id());
        assertTrue(restored.active());

        assertEquals(1L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE actor_user_id = ? AND action = 'TEACHER_UNASSIGNED'
                  AND entity_type = 'TEACHER_SUBJECT_ASSIGNMENT' AND entity_id = ?
                """, adminId, first.id()));
        assertEquals(2L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE actor_user_id = ? AND action = 'TEACHER_ASSIGNED'
                  AND entity_type = 'TEACHER_SUBJECT_ASSIGNMENT' AND entity_id = ?
                """, adminId, first.id()));
    }

    @Test
    void pagedAdminListsSearchAndExposeFormOptions() {
        long approvedTeacherId = createUser(UserRole.TEACHER, ApprovalStatus.APPROVED);
        long subjectId = createSubject();
        DashboardService dashboardService = new DashboardService(database);
        String email = MySqlTestDatabase.queryString(database,
                "SELECT email FROM users WHERE id = ?", approvedTeacherId);
        String code = findSubject(subjectId).code();

        PagedResult<DashboardViews.AdminUserRow> teachers =
                dashboardService.adminTeachers(email, 1);
        assertEquals(1L, teachers.getTotalItems());
        assertEquals(approvedTeacherId, teachers.getItems().getFirst().getId());

        PagedResult<DashboardViews.AdminSubjectRow> subjects =
                dashboardService.adminSubjects(code, 99);
        assertEquals(1L, subjects.getTotalItems());
        assertEquals(1, subjects.getPage());
        assertTrue(subjects.getItems().getFirst().getDescription() != null);

        assertTrue(dashboardService.adminApprovedTeachers().stream()
                .anyMatch(teacher -> teacher.getId() == approvedTeacherId));
        assertTrue(dashboardService.adminActiveSubjects().stream()
                .anyMatch(subject -> subject.getId() == subjectId));
    }

    @Test
    void bootstrapValidatesInputBeforeTouchingTheDatabase() {
        AdminBootstrapService bootstrapService = new AdminBootstrapService(database);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> bootstrapService.bootstrap("not-an-email", "", "short"));

        assertTrue(exception.fieldErrors().containsKey("email"));
        assertTrue(exception.fieldErrors().containsKey("fullName"));
        assertTrue(exception.fieldErrors().containsKey("password"));
    }

    private long createUser(UserRole role, ApprovalStatus status) {
        String email = "phase5-" + UUID.randomUUID() + "@learnloop.test";
        Instant approvedAt = status == ApprovalStatus.APPROVED ? Instant.now() : null;
        long userId = database.inTransaction(connection -> new UserDao().insert(connection, new NewUser(
                email,
                "integration-test-hash",
                "Phase Five " + role.name().toLowerCase(),
                role,
                status,
                approvedAt,
                null)));
        userIds.add(userId);
        return userId;
    }

    private long createSubject() {
        long subjectId = service.createSubject(
                adminId,
                uniqueCode(),
                "Test Subject " + UUID.randomUUID(),
                "Integration subject",
                CLIENT);
        subjectIds.add(subjectId);
        return subjectId;
    }

    private Subject findSubject(long subjectId) {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT id, code, name, description, is_active FROM subjects WHERE id = ?
                    """)) {
                statement.setLong(1, subjectId);
                try (var resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next(), "Subject was not found.");
                    return new Subject(
                            resultSet.getLong("id"),
                            resultSet.getString("code"),
                            resultSet.getString("name"),
                            resultSet.getString("description"),
                            resultSet.getBoolean("is_active"));
                }
            }
        });
    }

    private TeacherSubjectAssignment findAssignment(long teacherId, long subjectId) {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT id, teacher_user_id, subject_id, is_active
                    FROM teacher_subject_assignments
                    WHERE teacher_user_id = ? AND subject_id = ?
                    """)) {
                statement.setLong(1, teacherId);
                statement.setLong(2, subjectId);
                try (var resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next(), "Assignment was not found.");
                    return new TeacherSubjectAssignment(
                            resultSet.getLong("id"),
                            resultSet.getLong("teacher_user_id"),
                            resultSet.getLong("subject_id"),
                            resultSet.getBoolean("is_active"));
                }
            }
        });
    }

    private String userStatus(long userId) {
        return MySqlTestDatabase.queryString(database,
                "SELECT approval_status FROM users WHERE id = ?", userId);
    }

    private static String uniqueCode() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
