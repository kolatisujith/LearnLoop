package com.learnloop.service;

import com.learnloop.config.PublicUrlConfig;
import com.learnloop.dao.Database;
import com.learnloop.dao.TeacherSubjectAssignmentDao;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.LearningClass;
import com.learnloop.model.NewUser;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;
import com.learnloop.service.InvitationAccessException.Reason;
import com.learnloop.service.InvitationViews.IssuedInvitation;
import com.learnloop.service.InvitationViews.PublicInvitation;
import com.learnloop.service.InvitationViews.TeacherInvitation;
import com.learnloop.support.MySqlTestDatabase;
import com.learnloop.util.InvitationTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationServiceIntegrationTest {

    private static final ClientContext CLIENT = new ClientContext("127.0.0.1", "JUnit");
    private static final String BASE_URL = "https://learnloop.example/app";

    private Database database;
    private ClassService classService;
    private InvitationService invitationService;
    private final Set<Long> classIds = new LinkedHashSet<>();
    private final Set<Long> userIds = new LinkedHashSet<>();
    private long teacherId;
    private long otherTeacherId;
    private long studentId;
    private long otherStudentId;
    private long pendingStudentId;
    private long mathSubjectId;

    @BeforeEach
    void setUp() {
        database = MySqlTestDatabase.database();
        classService = new ClassService(database);
        invitationService = new InvitationService(database, new PublicUrlConfig(BASE_URL));
        mathSubjectId = subjectId("MATH");
        teacherId = createUser(UserRole.TEACHER, ApprovalStatus.APPROVED);
        otherTeacherId = createUser(UserRole.TEACHER, ApprovalStatus.APPROVED);
        studentId = createUser(UserRole.STUDENT, ApprovalStatus.APPROVED);
        otherStudentId = createUser(UserRole.STUDENT, ApprovalStatus.APPROVED);
        pendingStudentId = createUser(UserRole.STUDENT, ApprovalStatus.PENDING);
        assign(teacherId, mathSubjectId);
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
    void issuesRegeneratesAndListsTeacherInvitationWithoutPersistingRawToken() {
        LearningClass learningClass = createActiveClass(20);

        TeacherInvitation initial = invitationService.findTeacherInvitation(teacherId, learningClass.id());
        assertEquals("NOT_GENERATED", initial.state());
        assertTrue(initial.canGenerate());
        assertFalse(initial.canRegenerate());

        IssuedInvitation issued = invitationService.issueInvitation(
                teacherId, learningClass.id(), false, CLIENT);

        assertTrue(InvitationTokens.isValidFormat(issued.token()));
        assertEquals(BASE_URL + "/invite/" + issued.token(), issued.url());
        assertNotNull(issued.expiresAt());
        assertTrue(issued.expiresAt().toInstant().isAfter(Instant.now()));

        TeacherInvitation active = invitationService.findTeacherInvitation(teacherId, learningClass.id());
        assertEquals("ACTIVE", active.state());
        assertNotNull(active.createdAt());
        assertNotNull(active.expiresAt());
        assertTrue(active.canRegenerate());
        assertTrue(active.canRevoke());
        assertTrue(active.canExpire());

        String storedHash = MySqlTestDatabase.queryString(
                database, "SELECT invitation_token_hash FROM classes WHERE id = ?", learningClass.id());
        assertEquals(InvitationTokens.sha256Hex(issued.token()), storedHash);
        assertNotEquals(issued.token(), storedHash);
        assertInvitationAuditWithoutToken(learningClass.id(), "INVITATION_ISSUED", issued.token());

        ValidationException duplicate = assertThrows(ValidationException.class,
                () -> invitationService.issueInvitation(teacherId, learningClass.id(), false, CLIENT));
        assertTrue(duplicate.fieldErrors().containsKey("invitation"));

        IssuedInvitation regenerated = invitationService.issueInvitation(
                teacherId, learningClass.id(), true, CLIENT);
        assertNotEquals(issued.token(), regenerated.token());
        assertThrows(InvitationAccessException.class,
                () -> invitationService.findPublicInvitation(issued.token(), null));
        assertEquals("SIGN_IN",
                invitationService.findPublicInvitation(regenerated.token(), null).viewerState());
        assertInvitationAuditWithoutToken(
                learningClass.id(), "INVITATION_REGENERATED", regenerated.token());
    }

    @Test
    void expiresInvitationAndBlocksPublicAccess() {
        LearningClass learningClass = createActiveClass(20);
        IssuedInvitation issued = invitationService.issueInvitation(
                teacherId, learningClass.id(), false, CLIENT);

        invitationService.expireInvitation(teacherId, learningClass.id(), CLIENT);

        TeacherInvitation expired = invitationService.findTeacherInvitation(teacherId, learningClass.id());
        assertEquals("EXPIRED", expired.state());
        assertTrue(expired.canGenerate());
        assertFalse(expired.canRegenerate());
        InvitationAccessException access = assertThrows(InvitationAccessException.class,
                () -> invitationService.findPublicInvitation(issued.token(), null));
        assertEquals(Reason.EXPIRED, access.reason());
        assertInvitationAuditWithoutToken(
                learningClass.id(), "INVITATION_EXPIRED", issued.token());
    }

    @Test
    void revokesInvitationAndBlocksPublicAccess() {
        LearningClass learningClass = createActiveClass(20);
        IssuedInvitation issued = invitationService.issueInvitation(
                teacherId, learningClass.id(), false, CLIENT);

        invitationService.revokeInvitation(teacherId, learningClass.id(), CLIENT);

        TeacherInvitation revoked = invitationService.findTeacherInvitation(teacherId, learningClass.id());
        assertEquals("REVOKED", revoked.state());
        assertNotNull(revoked.revokedAt());
        InvitationAccessException access = assertThrows(InvitationAccessException.class,
                () -> invitationService.findPublicInvitation(issued.token(), null));
        assertEquals(Reason.REVOKED, access.reason());
        assertInvitationAuditWithoutToken(
                learningClass.id(), "INVITATION_REVOKED", issued.token());

        assertThrows(ValidationException.class,
                () -> invitationService.expireInvitation(teacherId, learningClass.id(), CLIENT));
        assertThrows(ValidationException.class,
                () -> invitationService.revokeInvitation(teacherId, learningClass.id(), CLIENT));
    }

    @Test
    void rejectsMalformedUnknownUnissuedAndInactiveInvitations() {
        assertReason(Reason.MALFORMED, "not a valid token!");
        assertReason(Reason.MALFORMED, "short");
        assertReason(Reason.NOT_FOUND, "A".repeat(16));

        LearningClass unissuedClass = createActiveClass(20);
        String unissuedToken = "unissued-" + UUID.randomUUID();
        MySqlTestDatabase.execute(database, """
                UPDATE classes
                SET invitation_token_hash = ?,
                    invitation_token_created_at = NULL,
                    invitation_token_expires_at = NULL,
                    invitation_token_revoked_at = NULL
                WHERE id = ?
                """, InvitationTokens.sha256Hex(unissuedToken), unissuedClass.id());
        assertReason(Reason.NOT_ISSUED, unissuedToken);

        LearningClass inactiveClass = createActiveClass(20);
        IssuedInvitation issued = invitationService.issueInvitation(
                teacherId, inactiveClass.id(), false, CLIENT);
        MySqlTestDatabase.execute(database,
                "UPDATE classes SET status = 'CANCELLED' WHERE id = ?", inactiveClass.id());
        assertReason(Reason.CLASS_INACTIVE, issued.token());
        assertEquals("CLASS_INACTIVE",
                invitationService.findTeacherInvitation(teacherId, inactiveClass.id()).state());
    }

    @Test
    void autoRevokesInvitationWhenClassLeavesActiveOrIsArchived() {
        LearningClass learningClass = createActiveClass(20);
        IssuedInvitation issued = invitationService.issueInvitation(
                teacherId, learningClass.id(), false, CLIENT);

        classService.updateClass(new TeacherClassUpdateRequest(
                teacherId,
                learningClass.id(),
                mathSubjectId,
                learningClass.title(),
                learningClass.description(),
                20,
                "DRAFT"), CLIENT);

        assertEquals("CLASS_INACTIVE",
                invitationService.findTeacherInvitation(teacherId, learningClass.id()).state());
        assertReason(Reason.REVOKED, issued.token());

        activate(learningClass.id());
        assertEquals("REVOKED",
                invitationService.findTeacherInvitation(teacherId, learningClass.id()).state());

        IssuedInvitation replaced = invitationService.issueInvitation(
                teacherId, learningClass.id(), false, CLIENT);
        classService.archiveClass(teacherId, learningClass.id(), CLIENT);

        assertEquals("CLASS_INACTIVE",
                invitationService.findTeacherInvitation(teacherId, learningClass.id()).state());
        assertReason(Reason.REVOKED, replaced.token());
        assertThrows(ValidationException.class,
                () -> invitationService.issueInvitation(teacherId, learningClass.id(), true, CLIENT));
    }

    @Test
    void studentRequestsToJoinOnceAndTeacherInvitationRemainsActive() {
        LearningClass learningClass = createActiveClass(2);
        IssuedInvitation issued = invitationService.issueInvitation(
                teacherId, learningClass.id(), false, CLIENT);

        PublicInvitation before = invitationService.findPublicInvitation(issued.token(), studentId);
        assertEquals("AVAILABLE", before.viewerState());
        assertTrue(before.canRequest());
        assertEquals(0L, before.enrolledCount());
        assertEquals(2, before.seatsRemaining());

        InvitationViews.JoinRequestResult result = invitationService.requestToJoin(
                issued.token(), studentId, "  I would like to join.  ", CLIENT);

        assertEquals(learningClass.id(), result.classId());
        assertEquals(1L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM class_join_requests
                WHERE class_id = ? AND student_user_id = ? AND status = 'PENDING'
                """, learningClass.id(), studentId));
        assertEquals(1L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE actor_user_id = ? AND action = 'JOIN_REQUEST_CREATED'
                  AND entity_type = 'JOIN_REQUEST' AND entity_id = ?
                  AND JSON_EXTRACT(details, '$.classId') = ?
                """, studentId, result.requestId(), learningClass.id()));
        assertEquals("PENDING",
                invitationService.findPublicInvitation(issued.token(), studentId).viewerState());
        assertEquals("ACTIVE",
                invitationService.findTeacherInvitation(teacherId, learningClass.id()).state());

        InvitationJoinException duplicate = assertThrows(InvitationJoinException.class,
                () -> invitationService.requestToJoin(issued.token(), studentId, null, CLIENT));
        assertEquals(InvitationJoinException.Reason.ALREADY_REQUESTED, duplicate.reason());
        assertEquals(0L, countAuditDetailsContaining(result.requestId(), issued.token()));
    }

    @Test
    void rejectsIneligibleUnauthorizedAndUnknownCallers() {
        LearningClass learningClass = createActiveClass(20);
        IssuedInvitation issued = invitationService.issueInvitation(
                teacherId, learningClass.id(), false, CLIENT);

        assertEquals("NOT_ELIGIBLE",
                invitationService.findPublicInvitation(issued.token(), pendingStudentId).viewerState());
        InvitationJoinException pending = assertThrows(InvitationJoinException.class,
                () -> invitationService.requestToJoin(issued.token(), pendingStudentId, null, CLIENT));
        assertEquals(InvitationJoinException.Reason.NOT_ELIGIBLE, pending.reason());
        assertTrue(pending.isAuthorizationFailure());

        assertEquals("NOT_ELIGIBLE",
                invitationService.findPublicInvitation(issued.token(), teacherId).viewerState());
        InvitationJoinException teacher = assertThrows(InvitationJoinException.class,
                () -> invitationService.requestToJoin(issued.token(), teacherId, null, CLIENT));
        assertEquals(InvitationJoinException.Reason.NOT_ELIGIBLE, teacher.reason());

        long unknownUserId = 9_999_999_999L;
        assertThrows(InvitationJoinException.class,
                () -> invitationService.requestToJoin(issued.token(), unknownUserId, null, CLIENT));

        assertThrows(ClassNotFoundException.class,
                () -> invitationService.findTeacherInvitation(otherTeacherId, learningClass.id()));
        assertThrows(ClassNotFoundException.class,
                () -> invitationService.issueInvitation(
                        otherTeacherId, learningClass.id(), true, CLIENT));
        assertThrows(ClassNotFoundException.class,
                () -> invitationService.revokeInvitation(otherTeacherId, learningClass.id(), CLIENT));
        assertThrows(ClassNotFoundException.class,
                () -> invitationService.expireInvitation(otherTeacherId, learningClass.id(), CLIENT));
    }

    @Test
    void blocksJoinWhenFullAlreadyEnrolledOrReviewed() {
        LearningClass fullClass = createActiveClass(1);
        IssuedInvitation fullInvitation = invitationService.issueInvitation(
                teacherId, fullClass.id(), false, CLIENT);
        enroll(fullClass.id(), otherStudentId, "ACTIVE");

        PublicInvitation full = invitationService.findPublicInvitation(fullInvitation.token(), studentId);
        assertEquals("FULL", full.viewerState());
        assertFalse(full.canRequest());
        assertEquals(0, full.seatsRemaining());
        InvitationJoinException fullJoin = assertThrows(InvitationJoinException.class,
                () -> invitationService.requestToJoin(fullInvitation.token(), studentId, null, CLIENT));
        assertEquals(InvitationJoinException.Reason.FULL, fullJoin.reason());

        LearningClass enrolledClass = createActiveClass(5);
        IssuedInvitation enrolledInvitation = invitationService.issueInvitation(
                teacherId, enrolledClass.id(), false, CLIENT);
        enroll(enrolledClass.id(), studentId, "ACTIVE");
        assertEquals("ENROLLED",
                invitationService.findPublicInvitation(enrolledInvitation.token(), studentId).viewerState());
        InvitationJoinException enrolled = assertThrows(InvitationJoinException.class,
                () -> invitationService.requestToJoin(enrolledInvitation.token(), studentId, null, CLIENT));
        assertEquals(InvitationJoinException.Reason.ALREADY_ENROLLED, enrolled.reason());

        LearningClass reviewedClass = createActiveClass(5);
        IssuedInvitation reviewedInvitation = invitationService.issueInvitation(
                teacherId, reviewedClass.id(), false, CLIENT);
        MySqlTestDatabase.execute(database, """
                INSERT INTO class_join_requests (class_id, student_user_id, status, message)
                VALUES (?, ?, 'REJECTED', 'Previous request')
                """, reviewedClass.id(), studentId);
        assertEquals("REJECTED",
                invitationService.findPublicInvitation(reviewedInvitation.token(), studentId).viewerState());
        InvitationJoinException reviewed = assertThrows(InvitationJoinException.class,
                () -> invitationService.requestToJoin(reviewedInvitation.token(), studentId, null, CLIENT));
        assertEquals(InvitationJoinException.Reason.REQUEST_REVIEWED, reviewed.reason());
    }

    @Test
    void rejectsInvitationActionsForInactiveClassesAndLongMessages() {
        LearningClass draftClass = createClass(10);
        TeacherInvitation draft = invitationService.findTeacherInvitation(teacherId, draftClass.id());
        assertEquals("CLASS_INACTIVE", draft.state());
        assertFalse(draft.canGenerate());
        ValidationException inactive = assertThrows(ValidationException.class,
                () -> invitationService.issueInvitation(teacherId, draftClass.id(), false, CLIENT));
        assertTrue(inactive.fieldErrors().containsKey("invitation"));
        assertThrows(ValidationException.class,
                () -> invitationService.revokeInvitation(teacherId, draftClass.id(), CLIENT));
        assertThrows(ValidationException.class,
                () -> invitationService.expireInvitation(teacherId, draftClass.id(), CLIENT));

        LearningClass activeClass = createActiveClass(20);
        IssuedInvitation issued = invitationService.issueInvitation(
                teacherId, activeClass.id(), false, CLIENT);
        ValidationException message = assertThrows(ValidationException.class,
                () -> invitationService.requestToJoin(
                        issued.token(), studentId, "x".repeat(501), CLIENT));
        assertTrue(message.fieldErrors().containsKey("message"));
        assertEquals(0L, MySqlTestDatabase.queryLong(database,
                "SELECT COUNT(*) FROM class_join_requests WHERE class_id = ? AND student_user_id = ?",
                activeClass.id(), studentId));
    }

    private void assertReason(Reason expected, String token) {
        InvitationAccessException exception = assertThrows(InvitationAccessException.class,
                () -> invitationService.findPublicInvitation(token, null));
        assertEquals(expected, exception.reason());
    }

    private void assertInvitationAuditWithoutToken(long classId, String action, String rawToken) {
        assertEquals(1L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*)
                FROM audit_logs
                WHERE action = ? AND entity_type = 'CLASS' AND entity_id = ?
                """, action, classId));
        assertEquals(0L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*)
                FROM audit_logs
                WHERE action = ? AND entity_type = 'CLASS' AND entity_id = ?
                  AND CAST(details AS CHAR) LIKE CONCAT('%', ?, '%')
                """, action, classId, rawToken));
    }

    private long countAuditDetailsContaining(long entityId, String value) {
        return MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*)
                FROM audit_logs
                WHERE entity_type = 'JOIN_REQUEST' AND entity_id = ?
                  AND CAST(details AS CHAR) LIKE CONCAT('%', ?, '%')
                """, entityId, value);
    }

    private LearningClass createActiveClass(int maxStudents) {
        LearningClass created = createClass(maxStudents);
        activate(created.id());
        return created;
    }

    private LearningClass createClass(int maxStudents) {
        LearningClass created = classService.createManagedClass(new TeacherClassCreateRequest(
                teacherId,
                mathSubjectId,
                "Phase 7 class " + UUID.randomUUID(),
                "Invitation integration test class.",
                maxStudents), CLIENT);
        classIds.add(created.id());
        return created;
    }

    private void activate(long classId) {
        ClassViews.ClassDetails details = classService.findOwnedClass(teacherId, classId);
        classService.updateClass(new TeacherClassUpdateRequest(
                teacherId,
                classId,
                details.getSubjectId(),
                details.getTitle(),
                details.getDescription(),
                details.getMaxStudents(),
                "ACTIVE"), CLIENT);
    }

    private void enroll(long classId, long studentUserId, String status) {
        MySqlTestDatabase.execute(database, """
                INSERT INTO class_enrollments (class_id, student_user_id, enrolled_by_user_id, status)
                VALUES (?, ?, ?, ?)
                """, classId, studentUserId, teacherId, status);
    }

    private long createUser(UserRole role, ApprovalStatus approvalStatus) {
        String email = "phase7-" + UUID.randomUUID() + "@learnloop.test";
        long userId = database.inTransaction(connection -> new UserDao().insert(connection, new NewUser(
                email,
                "integration-test-hash",
                "Phase Seven " + role.name(),
                role,
                approvalStatus,
                approvalStatus == ApprovalStatus.APPROVED ? Instant.now() : null,
                null)));
        userIds.add(userId);
        return userId;
    }

    private void assign(long teacherUserId, long subjectId) {
        database.inTransaction(connection -> {
            new TeacherSubjectAssignmentDao().upsertActive(
                    connection, teacherUserId, subjectId, teacherUserId);
            return null;
        });
    }

    private long subjectId(String code) {
        return MySqlTestDatabase.queryLong(database, "SELECT id FROM subjects WHERE code = ?", code);
    }
}
