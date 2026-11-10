package com.learnloop.service;

import com.learnloop.dao.AuditLogDao;
import com.learnloop.dao.ClassDao;
import com.learnloop.dao.Database;
import com.learnloop.dao.EnrollmentDao;
import com.learnloop.dao.JoinRequestDao;
import com.learnloop.dao.JoinRequestDao.StoredJoinRequest;
import com.learnloop.dao.NotificationDao;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.LearningClass;
import com.learnloop.model.UserAccount;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;
import com.learnloop.service.DashboardViews.StudentJoinRequestRow;
import com.learnloop.service.DashboardViews.TeacherJoinRequestRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public final class JoinRequestService {

    private final Database database;
    private final UserDao userDao;
    private final JoinRequestDao requestDao;
    private final ClassDao classDao;
    private final EnrollmentDao enrollmentDao;
    private final NotificationDao notificationDao;
    private final AuditLogDao auditLogDao;

    public JoinRequestService(Database database) {
        this(database, new UserDao(), new JoinRequestDao(), new ClassDao(), new EnrollmentDao(),
                new NotificationDao(), new AuditLogDao());
    }

    public JoinRequestService(
            Database database,
            UserDao userDao,
            JoinRequestDao requestDao,
            ClassDao classDao,
            EnrollmentDao enrollmentDao,
            NotificationDao notificationDao,
            AuditLogDao auditLogDao) {
        this.database = Objects.requireNonNull(database, "database");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.requestDao = Objects.requireNonNull(requestDao, "requestDao");
        this.classDao = Objects.requireNonNull(classDao, "classDao");
        this.enrollmentDao = Objects.requireNonNull(enrollmentDao, "enrollmentDao");
        this.notificationDao = Objects.requireNonNull(notificationDao, "notificationDao");
        this.auditLogDao = Objects.requireNonNull(auditLogDao, "auditLogDao");
    }

    public List<StudentJoinRequestRow> findStudentRequests(long studentUserId) {
        return database.withConnection(connection -> {
            requireApprovedStudent(connection, studentUserId);
            return requestDao.findByStudent(connection, studentUserId, false).stream()
                    .map(JoinRequestService::toStudentRow)
                    .toList();
        });
    }

    public List<StudentJoinRequestRow> findStudentPendingRequests(long studentUserId) {
        return database.withConnection(connection -> {
            requireApprovedStudent(connection, studentUserId);
            return requestDao.findByStudent(connection, studentUserId, true).stream()
                    .map(JoinRequestService::toStudentRow)
                    .toList();
        });
    }

    public List<TeacherJoinRequestRow> findTeacherPendingRequests(long teacherUserId) {
        return database.withConnection(connection -> {
            requireApprovedTeacher(connection, teacherUserId);
            return requestDao.findPendingForTeacher(connection, teacherUserId).stream()
                    .map(JoinRequestService::toTeacherRow)
                    .toList();
        });
    }

    public void acceptRequest(long teacherUserId, long requestId, ClientContext clientContext) {
        if (teacherUserId <= 0 || requestId <= 0) {
            throw new JoinRequestAccessException("A valid teacher and request are required.");
        }
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            StoredJoinRequest request = requestDao.findByIdForUpdate(connection, requestId)
                    .orElseThrow(() -> new JoinRequestAccessException("Join request was not found."));

            LearningClass learningClass = classDao.findByIdAndTeacherForUpdate(connection, request.classId(), teacherUserId)
                    .orElseThrow(() -> new JoinRequestAccessException("You do not have permission to manage this request."));

            if (!"ACTIVE".equals(learningClass.status())) {
                throw new JoinRequestAccessException("Only active classes can accept join requests.");
            }
            if (!"PENDING".equals(request.status())) {
                throw new JoinRequestAccessException("Only pending join requests can be accepted.");
            }
            long studentUserId = getStudentUserId(connection, requestId);
            if (studentUserId <= 0) {
                throw new JoinRequestAccessException("Student account could not be found.");
            }
            if (enrollmentDao.exists(connection, learningClass.id(), studentUserId)) {
                throw new JoinRequestAccessException("The student is already enrolled in this class.");
            }
            Integer maxStudents = learningClass.maxStudents();
            if (maxStudents != null && maxStudents > 0) {
                long enrolled = classDao.countActiveEnrollments(connection, learningClass.id());
                if (enrolled >= maxStudents) {
                    throw new JoinRequestAccessException("This class is at capacity.");
                }
            }

            int updated = requestDao.updateStatus(connection, requestId, "APPROVED", teacherUserId);
            if (updated != 1) {
                throw new JoinRequestAccessException("The join request could not be accepted.");
            }
            enrollmentDao.insert(connection, learningClass.id(), studentUserId, requestId, teacherUserId);
            notificationDao.insert(
                    connection,
                    studentUserId,
                    teacherUserId,
                    "JOIN_REQUEST_ACCEPTED",
                    "Join request accepted",
                    "Your request to join " + learningClass.title() + " has been accepted.",
                    "/student/requests",
                    "JOIN_REQUEST",
                    requestId);
            auditLogDao.insert(
                    connection,
                    teacherUserId,
                    "JOIN_REQUEST_ACCEPTED",
                    "JOIN_REQUEST",
                    requestId,
                    "{\"classId\":" + learningClass.id() + "}",
                    clientContext == null ? null : clientContext.ipAddress(),
                    clientContext == null ? null : clientContext.userAgent());
            return null;
        });
    }

    public void rejectRequest(long teacherUserId, long requestId, String reason, ClientContext clientContext) {
        if (teacherUserId <= 0 || requestId <= 0) {
            throw new JoinRequestAccessException("A valid teacher and request are required.");
        }
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            StoredJoinRequest request = requestDao.findByIdForUpdate(connection, requestId)
                    .orElseThrow(() -> new JoinRequestAccessException("Join request was not found."));

            LearningClass learningClass = classDao.findByIdAndTeacherForUpdate(connection, request.classId(), teacherUserId)
                    .orElseThrow(() -> new JoinRequestAccessException("You do not have permission to manage this request."));

            if (!"PENDING".equals(request.status())) {
                throw new JoinRequestAccessException("Only pending join requests can be rejected.");
            }
            long studentUserId = getStudentUserId(connection, requestId);
            if (studentUserId <= 0) {
                throw new JoinRequestAccessException("Student account could not be found.");
            }
            int updated = requestDao.updateStatus(connection, requestId, "REJECTED", teacherUserId);
            if (updated != 1) {
                throw new JoinRequestAccessException("The join request could not be rejected.");
            }
            String message = "Your request to join " + learningClass.title() + " has been rejected.";
            if (reason != null && !reason.isBlank()) {
                message = message + " Reason: " + reason.trim();
            }
            notificationDao.insert(
                    connection,
                    studentUserId,
                    teacherUserId,
                    "JOIN_REQUEST_REJECTED",
                    "Join request rejected",
                    message,
                    "/student/requests",
                    "JOIN_REQUEST",
                    requestId);
            auditLogDao.insert(
                    connection,
                    teacherUserId,
                    "JOIN_REQUEST_REJECTED",
                    "JOIN_REQUEST",
                    requestId,
                    "{\"classId\":" + learningClass.id() + "}",
                    clientContext == null ? null : clientContext.ipAddress(),
                    clientContext == null ? null : clientContext.userAgent());
            return null;
        });
    }

    private long getStudentUserId(Connection connection, long requestId) throws SQLException {
        String sql = "SELECT student_user_id FROM class_join_requests WHERE id = ?";
        try (java.sql.PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requestId);
            try (java.sql.ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("student_user_id");
                }
            }
        }
        return -1L;
    }

    private void requireApprovedStudent(Connection connection, long studentUserId) throws SQLException {
        UserAccount student = requireUser(connection, studentUserId);
        if (student.role() != UserRole.STUDENT) {
            throw new JoinRequestAccessException("Only students can view their join requests.");
        }
        if (student.approvalStatus() != ApprovalStatus.APPROVED) {
            throw new JoinRequestAccessException("The student account is not approved.");
        }
    }

    private void requireApprovedTeacher(Connection connection, long teacherUserId) throws SQLException {
        UserAccount teacher = requireUser(connection, teacherUserId);
        if (teacher.role() != UserRole.TEACHER) {
            throw new JoinRequestAccessException("Only teachers can view class join requests.");
        }
        if (teacher.approvalStatus() != ApprovalStatus.APPROVED) {
            throw new JoinRequestAccessException("The teacher account is not approved.");
        }
    }

    private UserAccount requireApprovedTeacherForUpdate(Connection connection, long teacherUserId)
            throws SQLException {
        UserAccount teacher = userDao.findByIdForUpdate(connection, teacherUserId)
                .orElseThrow(() -> new JoinRequestAccessException("Teacher authorization could not be verified."));
        if (teacher.role() != UserRole.TEACHER) {
            throw new JoinRequestAccessException("Only teachers can manage class join requests.");
        }
        if (teacher.approvalStatus() != ApprovalStatus.APPROVED) {
            throw new JoinRequestAccessException("The teacher account is not approved.");
        }
        return teacher;
    }

    private UserAccount requireUser(Connection connection, long userId) throws SQLException {
        if (userId <= 0) {
            throw new JoinRequestAccessException("A valid user account is required.");
        }
        return userDao.findById(connection, userId)
                .orElseThrow(() -> new JoinRequestAccessException("The user account could not be found."));
    }

    private static StudentJoinRequestRow toStudentRow(StoredJoinRequest request) {
        return new StudentJoinRequestRow(
                request.id(),
                request.classTitle(),
                request.subjectCode(),
                request.status(),
                request.message(),
                toDate(request.createdAt()),
                request.classId(),
                request.classStatus(),
                request.subjectName(),
                request.teacherName(),
                toDate(request.reviewedAt()));
    }

    private static TeacherJoinRequestRow toTeacherRow(StoredJoinRequest request) {
        return new TeacherJoinRequestRow(
                request.id(),
                request.classId(),
                request.classTitle(),
                request.studentName(),
                request.studentEmail(),
                request.message(),
                toDate(request.createdAt()));
    }

    private static Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }
}
