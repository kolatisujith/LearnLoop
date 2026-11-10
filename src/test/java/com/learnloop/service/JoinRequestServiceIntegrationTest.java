package com.learnloop.service;

import com.learnloop.config.PublicUrlConfig;
import com.learnloop.dao.DataAccessException;
import com.learnloop.dao.Database;
import com.learnloop.dao.TeacherSubjectAssignmentDao;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.LearningClass;
import com.learnloop.model.NewUser;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;
import com.learnloop.service.DashboardViews.StudentJoinRequestRow;
import com.learnloop.service.InvitationViews.IssuedInvitation;
import com.learnloop.service.InvitationViews.JoinRequestResult;
import com.learnloop.support.MySqlTestDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoinRequestServiceIntegrationTest {

    private static final ClientContext CLIENT = new ClientContext("127.0.0.1", "JUnit");
    private static final String BASE_URL = "https://learnloop.example/app";

    private Database database;
    private ClassService classService;
    private InvitationService invitationService;
    private JoinRequestService requestService;
    private final Set<Long> classIds = new LinkedHashSet<>();
    private final Set<Long> userIds = new LinkedHashSet<>();
    private long teacherId;
    private long otherTeacherId;
    private long studentId;
    private long otherStudentId;
    private long pendingTeacherId;
    private long pendingStudentId;
    private long mathSubjectId;

    @BeforeEach
    void setUp() {
        database = MySqlTestDatabase.database();
        classService = new ClassService(database);
        invitationService = new InvitationService(database, new PublicUrlConfig(BASE_URL));
        requestService = new JoinRequestService(database);
        mathSubjectId = subjectId("MATH");
        teacherId = createUser(UserRole.TEACHER, ApprovalStatus.APPROVED);
        otherTeacherId = createUser(UserRole.TEACHER, ApprovalStatus.APPROVED);
        studentId = createUser(UserRole.STUDENT, ApprovalStatus.APPROVED);
        otherStudentId = createUser(UserRole.STUDENT, ApprovalStatus.APPROVED);
        pendingTeacherId = createUser(UserRole.TEACHER, ApprovalStatus.PENDING);
        pendingStudentId = createUser(UserRole.STUDENT, ApprovalStatus.PENDING);
        assign(teacherId);
        assign(otherTeacherId);
    }

    @AfterEach
    void cleanUp() {
        for (long classId : classIds) {
            MySqlTestDatabase.execute(database, """
                    DELETE FROM audit_logs
                    WHERE entity_type = 'JOIN_REQUEST'
                      AND entity_id IN (SELECT id FROM class_join_requests WHERE class_id = ?)
                    """, classId);
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
    void studentHistoryIncludesPendingAcceptedAndRejectedRequests() {
        long pendingClassId = createActiveClass(teacherId, 10);
        long acceptedClassId = createActiveClass(teacherId, 10);
        long rejectedClassId = createActiveClass(otherTeacherId, 10);
        long otherStudentClassId = createActiveClass(teacherId, 10);

        long pendingRequestId = request(studentId, pendingClassId, "Please add me.");
        long acceptedRequestId = request(studentId, acceptedClassId, "Accepted request.");
        long rejectedRequestId = request(studentId, rejectedClassId, "Rejected request.");
        long otherStudentRequestId = request(otherStudentId, otherStudentClassId, null);

        review(acceptedRequestId, "APPROVED");
        review(rejectedRequestId, "REJECTED");

        List<StudentJoinRequestRow> history = requestService.findStudentRequests(studentId);
        Map<Long, StudentJoinRequestRow> rows = history.stream()
                .collect(Collectors.toMap(StudentJoinRequestRow::getId, Function.identity()));

        assertEquals(3, history.size());
        assertEquals(Set.of(pendingRequestId, acceptedRequestId, rejectedRequestId), rows.keySet());
        assertFalse(rows.containsKey(otherStudentRequestId));
        assertEquals(1, requestService.findStudentPendingRequests(studentId).size());
        assertEquals(pendingRequestId, requestService.findStudentPendingRequests(studentId).get(0).getId());

        StudentJoinRequestRow pending = rows.get(pendingRequestId);
        assertEquals("PENDING", pending.getStatus());
        assertEquals("Pending", pending.getStatusLabel());
        assertEquals("ll-state-pending", pending.getStatusCssClass());
        assertTrue(pending.isPending());
        assertNull(pending.getReviewedAt());
        assertEquals("Please add me.", pending.getMessage());
        assertEquals("ACTIVE", pending.getClassStatus());
        assertNotNull(pending.getTeacherName());

        StudentJoinRequestRow accepted = rows.get(acceptedRequestId);
        assertEquals("APPROVED", accepted.getStatus());
        assertEquals("Accepted", accepted.getStatusLabel());
        assertEquals("ll-state-approved", accepted.getStatusCssClass());
        assertFalse(accepted.isPending());
        assertNotNull(accepted.getReviewedAt());

        StudentJoinRequestRow rejected = rows.get(rejectedRequestId);
        assertEquals("REJECTED", rejected.getStatus());
        assertEquals("Rejected", rejected.getStatusLabel());
        assertEquals("ll-state-rejected", rejected.getStatusCssClass());
        assertFalse(rejected.isPending());
        assertNotNull(rejected.getReviewedAt());

        assertEquals("ACCEPTED",
                invitationService.findPublicInvitation(tokenForClass(acceptedClassId), studentId).viewerState());
        assertEquals("REJECTED",
                invitationService.findPublicInvitation(tokenForClass(rejectedClassId), studentId).viewerState());
        assertEquals(0L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM class_enrollments WHERE student_user_id = ? AND class_id = ?
                """, studentId, acceptedClassId));
    }

    @Test
    void teacherQueueOnlyContainsPendingRequestsForOwnedClasses() {
        long ownPendingClassId = createActiveClass(teacherId, 10);
        long ownReviewedClassId = createActiveClass(teacherId, 10);
        long otherPendingClassId = createActiveClass(otherTeacherId, 10);

        long ownPendingRequestId = request(studentId, ownPendingClassId, null);
        long ownReviewedRequestId = request(otherStudentId, ownReviewedClassId, null);
        long otherPendingRequestId = request(otherStudentId, otherPendingClassId, null);
        review(ownReviewedRequestId, "REJECTED");

        var teacherQueue = requestService.findTeacherPendingRequests(teacherId);
        assertEquals(1, teacherQueue.size());
        assertEquals(ownPendingRequestId, teacherQueue.get(0).getId());
        assertEquals(ownPendingClassId, teacherQueue.get(0).getClassId());
        assertEquals("Phase 8 STUDENT", teacherQueue.get(0).getStudentName());

        var otherQueue = requestService.findTeacherPendingRequests(otherTeacherId);
        assertEquals(1, otherQueue.size());
        assertEquals(otherPendingRequestId, otherQueue.get(0).getId());
    }

    @Test
    void rejectsUnapprovedMismatchedAndUnknownCallers() {
        assertThrows(JoinRequestAccessException.class,
                () -> requestService.findStudentRequests(teacherId));
        assertThrows(JoinRequestAccessException.class,
                () -> requestService.findStudentRequests(pendingStudentId));
        assertThrows(JoinRequestAccessException.class,
                () -> requestService.findTeacherPendingRequests(studentId));
        assertThrows(JoinRequestAccessException.class,
                () -> requestService.findTeacherPendingRequests(pendingTeacherId));
        assertThrows(JoinRequestAccessException.class,
                () -> requestService.findStudentRequests(9_999_999_999L));
        assertThrows(JoinRequestAccessException.class,
                () -> requestService.findTeacherPendingRequests(-1L));
    }

    @Test
    void duplicateRequestsAreRejectedWithoutEnrollment() {
        long classId = createActiveClass(teacherId, 10);
        String token = tokenForClass(classId);

        JoinRequestResult first = invitationService.requestToJoin(token, studentId, null, CLIENT);
        assertNotNull(first);
        InvitationJoinException duplicate = assertThrows(InvitationJoinException.class,
                () -> invitationService.requestToJoin(token, studentId, null, CLIENT));
        assertEquals(InvitationJoinException.Reason.ALREADY_REQUESTED, duplicate.reason());
        assertEquals(1L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM class_join_requests
                WHERE class_id = ? AND student_user_id = ? AND status = 'PENDING'
                """, classId, studentId));
        assertEquals(0L, MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM class_enrollments
                WHERE class_id = ? AND student_user_id = ?
                """, classId, studentId));

        assertThrows(DataAccessException.class, () -> MySqlTestDatabase.execute(database, """
                INSERT INTO class_join_requests (class_id, student_user_id, status)
                VALUES (?, ?, 'PENDING')
                """, classId, studentId));
    }

    @Test
    void teacherCanAcceptPendingRequestAndEnrollStudent() {
        long classId = createActiveClass(teacherId, 10);
        long requestId = request(studentId, classId, "Please accept me.");

        requestService.acceptRequest(teacherId, requestId, CLIENT);

        assertEquals("APPROVED", requestStatus(requestId));
        assertEquals(1L, activeEnrollments(classId, studentId));
        assertEquals(1L, notifications(studentId, "JOIN_REQUEST_ACCEPTED"));
        assertEquals(1L, auditEvents("JOIN_REQUEST_ACCEPTED", requestId));
        assertEquals(0, requestService.findTeacherPendingRequests(teacherId).size());
    }

    @Test
    void teacherCanRejectPendingRequestWithReason() {
        long classId = createActiveClass(teacherId, 10);
        long requestId = request(studentId, classId, null);

        requestService.rejectRequest(teacherId, requestId, "Class is full for this term.", CLIENT);

        assertEquals("REJECTED", requestStatus(requestId));
        assertEquals(0L, activeEnrollments(classId, studentId));
        assertEquals(1L, notifications(studentId, "JOIN_REQUEST_REJECTED"));
        assertEquals(1L, auditEvents("JOIN_REQUEST_REJECTED", requestId));
        String message = MySqlTestDatabase.queryString(database, """
                SELECT message FROM notifications
                WHERE recipient_user_id = ? AND type = 'JOIN_REQUEST_REJECTED'
                ORDER BY id DESC LIMIT 1
                """, studentId);
        assertTrue(message.contains("Class is full for this term."));
    }

    @Test
    void acceptRejectsTeacherWhoDoesNotOwnTheClass() {
        long classId = createActiveClass(teacherId, 10);
        long requestId = request(studentId, classId, null);

        assertThrows(JoinRequestAccessException.class,
                () -> requestService.acceptRequest(otherTeacherId, requestId, CLIENT));

        assertEquals("PENDING", requestStatus(requestId));
        assertEquals(0L, activeEnrollments(classId, studentId));
        assertEquals(0L, notifications(studentId, "JOIN_REQUEST_ACCEPTED"));
    }

    @Test
    void acceptRejectsUnapprovedTeacher() {
        long classId = createActiveClass(teacherId, 10);
        long requestId = request(studentId, classId, null);

        assertThrows(JoinRequestAccessException.class,
                () -> requestService.acceptRequest(pendingTeacherId, requestId, CLIENT));

        assertEquals("PENDING", requestStatus(requestId));
        assertEquals(0L, activeEnrollments(classId, studentId));
    }

    @Test
    void acceptRejectsWhenClassIsAtCapacity() {
        long classId = createActiveClass(teacherId, 1);
        long requestId = request(studentId, classId, null);
        MySqlTestDatabase.execute(database, """
                INSERT INTO class_enrollments (class_id, student_user_id, status)
                VALUES (?, ?, 'ACTIVE')
                """, classId, otherStudentId);

        assertThrows(JoinRequestAccessException.class,
                () -> requestService.acceptRequest(teacherId, requestId, CLIENT));

        assertEquals("PENDING", requestStatus(requestId));
        assertEquals(0L, activeEnrollments(classId, studentId));
        assertEquals(0L, notifications(studentId, "JOIN_REQUEST_ACCEPTED"));
    }

    @Test
    void acceptRejectsAlreadyEnrolledStudent() {
        long classId = createActiveClass(teacherId, 10);
        long requestId = request(studentId, classId, null);
        MySqlTestDatabase.execute(database, """
                INSERT INTO class_enrollments (class_id, student_user_id, status)
                VALUES (?, ?, 'ACTIVE')
                """, classId, studentId);

        assertThrows(JoinRequestAccessException.class,
                () -> requestService.acceptRequest(teacherId, requestId, CLIENT));

        assertEquals("PENDING", requestStatus(requestId));
        assertEquals(1L, activeEnrollments(classId, studentId));
    }

    @Test
    void acceptRejectsRequestThatIsNoLongerPending() {
        long classId = createActiveClass(teacherId, 10);
        long requestId = request(studentId, classId, null);
        requestService.acceptRequest(teacherId, requestId, CLIENT);

        assertThrows(JoinRequestAccessException.class,
                () -> requestService.acceptRequest(teacherId, requestId, CLIENT));

        assertEquals("APPROVED", requestStatus(requestId));
        assertEquals(1L, activeEnrollments(classId, studentId));
        assertEquals(1L, auditEvents("JOIN_REQUEST_ACCEPTED", requestId));
    }

    @Test
    void acceptRequiresAnActiveClass() {
        long classId = createActiveClass(teacherId, 10);
        long requestId = request(studentId, classId, null);
        MySqlTestDatabase.execute(database, "UPDATE classes SET status = 'DRAFT' WHERE id = ?", classId);

        assertThrows(JoinRequestAccessException.class,
                () -> requestService.acceptRequest(teacherId, requestId, CLIENT));

        assertEquals("PENDING", requestStatus(requestId));
        assertEquals(0L, activeEnrollments(classId, studentId));
    }

    private String requestStatus(long requestId) {
        return MySqlTestDatabase.queryString(
                database, "SELECT status FROM class_join_requests WHERE id = ?", requestId);
    }

    private long activeEnrollments(long classId, long studentUserId) {
        return MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM class_enrollments
                WHERE class_id = ? AND student_user_id = ? AND status = 'ACTIVE'
                """, classId, studentUserId);
    }

    private long notifications(long recipientUserId, String type) {
        return MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM notifications WHERE recipient_user_id = ? AND type = ?
                """, recipientUserId, type);
    }

    private long auditEvents(String action, long requestId) {
        return MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE action = ? AND entity_type = 'JOIN_REQUEST' AND entity_id = ?
                """, action, requestId);
    }

    private long request(long studentUserId, long classId, String message) {
        JoinRequestResult result = invitationService.requestToJoin(
                tokenForClass(classId), studentUserId, message, CLIENT);
        return result.requestId();
    }

    private String tokenForClass(long classId) {
        IssuedInvitation issued = invitationService.issueInvitation(teacherIdFor(classId), classId, true, CLIENT);
        return issued.token();
    }

    private long teacherIdFor(long classId) {
        return MySqlTestDatabase.queryLong(
                database, "SELECT teacher_user_id FROM classes WHERE id = ?", classId);
    }

    private void review(long requestId, String status) {
        MySqlTestDatabase.execute(database, """
                UPDATE class_join_requests
                SET status = ?, reviewed_by_user_id = ?, reviewed_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                """, status, teacherId, requestId);
    }

    private long createActiveClass(long owningTeacherId, int maxStudents) {
        LearningClass created = classService.createManagedClass(new TeacherClassCreateRequest(
                owningTeacherId,
                mathSubjectId,
                "Phase 8 class " + UUID.randomUUID(),
                "Join request integration test class.",
                maxStudents), CLIENT);
        classIds.add(created.id());
        ClassViews.ClassDetails details = classService.findOwnedClass(owningTeacherId, created.id());
        classService.updateClass(new TeacherClassUpdateRequest(
                owningTeacherId,
                created.id(),
                details.getSubjectId(),
                details.getTitle(),
                details.getDescription(),
                details.getMaxStudents(),
                "ACTIVE"), CLIENT);
        return created.id();
    }

    private long createUser(UserRole role, ApprovalStatus approvalStatus) {
        String email = "phase8-" + UUID.randomUUID() + "@learnloop.test";
        long userId = database.inTransaction(connection -> new UserDao().insert(connection, new NewUser(
                email,
                "integration-test-hash",
                "Phase 8 " + role.name(),
                role,
                approvalStatus,
                approvalStatus == ApprovalStatus.APPROVED ? Instant.now() : null,
                null)));
        userIds.add(userId);
        return userId;
    }

    private void assign(long teacherUserId) {
        database.inTransaction(connection -> {
            new TeacherSubjectAssignmentDao().upsertActive(
                    connection, teacherUserId, mathSubjectId, teacherUserId);
            return null;
        });
    }

    private long subjectId(String code) {
        return MySqlTestDatabase.queryLong(database, "SELECT id FROM subjects WHERE code = ?", code);
    }
}
