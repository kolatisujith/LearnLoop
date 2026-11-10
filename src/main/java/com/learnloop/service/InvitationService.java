package com.learnloop.service;

import com.learnloop.config.PublicUrlConfig;
import com.learnloop.dao.AuditLogDao;
import com.learnloop.dao.ClassDao;
import com.learnloop.dao.Database;
import com.learnloop.dao.InvitationDao;
import com.learnloop.dao.InvitationDao.StoredInvitation;
import com.learnloop.dao.JdbcSupport;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.UserAccount;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;
import com.learnloop.service.InvitationViews.IssuedInvitation;
import com.learnloop.service.InvitationViews.JoinRequestResult;
import com.learnloop.service.InvitationViews.PublicInvitation;
import com.learnloop.service.InvitationViews.TeacherInvitation;
import com.learnloop.util.InvitationTokens;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

public final class InvitationService {

    private static final Duration INVITATION_LIFETIME = Duration.ofDays(14);
    private static final String CLASS_ENTITY_TYPE = "CLASS";
    private static final String JOIN_REQUEST_ENTITY_TYPE = "JOIN_REQUEST";
    private static final String INVITATION_ISSUED_ACTION = "INVITATION_ISSUED";
    private static final String INVITATION_REGENERATED_ACTION = "INVITATION_REGENERATED";
    private static final String INVITATION_REVOKED_ACTION = "INVITATION_REVOKED";
    private static final String INVITATION_EXPIRED_ACTION = "INVITATION_EXPIRED";
    private static final String JOIN_REQUEST_CREATED_ACTION = "JOIN_REQUEST_CREATED";
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final Database database;
    private final UserDao userDao;
    private final ClassDao classDao;
    private final InvitationDao invitationDao;
    private final AuditLogDao auditLogDao;
    private final PublicUrlConfig publicUrlConfig;

    public InvitationService(Database database) {
        this(database, null, new UserDao(), new ClassDao(), new InvitationDao(), new AuditLogDao());
    }

    public InvitationService(Database database, PublicUrlConfig publicUrlConfig) {
        this(database, publicUrlConfig, new UserDao(), new ClassDao(), new InvitationDao(), new AuditLogDao());
    }

    InvitationService(
            Database database,
            PublicUrlConfig publicUrlConfig,
            UserDao userDao,
            ClassDao classDao,
            InvitationDao invitationDao,
            AuditLogDao auditLogDao) {
        this.database = Objects.requireNonNull(database, "database");
        this.publicUrlConfig = publicUrlConfig;
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.classDao = Objects.requireNonNull(classDao, "classDao");
        this.invitationDao = Objects.requireNonNull(invitationDao, "invitationDao");
        this.auditLogDao = Objects.requireNonNull(auditLogDao, "auditLogDao");
    }

    public TeacherInvitation findTeacherInvitation(long teacherUserId, long classId) {
        requirePositiveIdentifiers(teacherUserId, classId);
        return database.withConnection(connection -> {
            requireApprovedTeacher(connection, teacherUserId);
            StoredInvitation invitation = invitationDao.findOwned(connection, classId, teacherUserId)
                    .orElseThrow(() -> new ClassNotFoundException("Class was not found."));
            return toTeacherInvitation(invitation, Instant.now());
        });
    }

    public IssuedInvitation issueInvitation(
            long teacherUserId,
            long classId,
            boolean regenerate,
            ClientContext clientContext) {
        requirePositiveIdentifiers(teacherUserId, classId);
        PublicUrlConfig baseUrl = publicUrlConfig == null ? PublicUrlConfig.load() : publicUrlConfig;
        String token = InvitationTokens.generate();
        String tokenHash = InvitationTokens.sha256Hex(token);
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(INVITATION_LIFETIME);

        IssuedClass issuedClass = database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            StoredInvitation current = invitationDao.findOwnedForUpdate(connection, classId, teacherUserId)
                    .orElseThrow(() -> new ClassNotFoundException("Class was not found."));
            if (!"ACTIVE".equals(current.classStatus())) {
                throw new ValidationException(
                        "invitation", "Activate this class before generating an invitation link.");
            }
            if (!regenerate && isActiveInvitation(current, issuedAt)) {
                throw new ValidationException(
                        "invitation",
                        "An active invitation already exists. Regenerate it to replace the current link.");
            }

            int updated = invitationDao.issue(
                    connection, classId, teacherUserId, tokenHash, issuedAt, expiresAt);
            if (updated != 1) {
                throw new ClassNotFoundException("Class was not found.");
            }

            audit(
                    connection,
                    teacherUserId,
                    regenerate ? INVITATION_REGENERATED_ACTION : INVITATION_ISSUED_ACTION,
                    CLASS_ENTITY_TYPE,
                    classId,
                    "{\"expiresAt\":\"" + expiresAt + "\"}",
                    clientContext);
            return new IssuedClass(current.title(), expiresAt);
        });

        return new IssuedInvitation(
                classId,
                issuedClass.title(),
                token,
                baseUrl.invitationUrl(token),
                Date.from(issuedClass.expiresAt()));
    }

    public void revokeInvitation(
            long teacherUserId, long classId, ClientContext clientContext) {
        requirePositiveIdentifiers(teacherUserId, classId);
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            StoredInvitation current = invitationDao.findOwnedForUpdate(connection, classId, teacherUserId)
                    .orElseThrow(() -> new ClassNotFoundException("Class was not found."));
            requireIssuedInvitation(current);
            if (current.invitationRevokedAt() != null) {
                throw new ValidationException("invitation", "This invitation is already revoked.");
            }

            Instant now = Instant.now();
            int updated = invitationDao.revoke(connection, classId, teacherUserId, now);
            if (updated != 1) {
                throw new ValidationException("invitation", "This invitation could not be revoked.");
            }
            audit(
                    connection,
                    teacherUserId,
                    INVITATION_REVOKED_ACTION,
                    CLASS_ENTITY_TYPE,
                    classId,
                    "{}",
                    clientContext);
            return null;
        });
    }

    public void expireInvitation(
            long teacherUserId, long classId, ClientContext clientContext) {
        requirePositiveIdentifiers(teacherUserId, classId);
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            StoredInvitation current = invitationDao.findOwnedForUpdate(connection, classId, teacherUserId)
                    .orElseThrow(() -> new ClassNotFoundException("Class was not found."));
            requireIssuedInvitation(current);
            if (current.invitationRevokedAt() != null) {
                throw new ValidationException("invitation", "A revoked invitation cannot be expired.");
            }

            Instant now = Instant.now();
            int updated = invitationDao.expire(connection, classId, teacherUserId, now);
            if (updated != 1) {
                throw new ValidationException("invitation", "This invitation could not be expired.");
            }
            audit(
                    connection,
                    teacherUserId,
                    INVITATION_EXPIRED_ACTION,
                    CLASS_ENTITY_TYPE,
                    classId,
                    "{}",
                    clientContext);
            return null;
        });
    }

    public PublicInvitation findPublicInvitation(String rawToken, Long viewerUserId) {
        return database.withConnection(connection -> {
            StoredInvitation invitation = requireAccessibleInvitation(connection, rawToken, false);
            return toPublicInvitation(connection, invitation, viewerUserId);
        });
    }

    public JoinRequestResult requestToJoin(
            String rawToken,
            long studentUserId,
            String message,
            ClientContext clientContext) {
        if (studentUserId <= 0) {
            throw new InvitationJoinException(
                    InvitationJoinException.Reason.NOT_ELIGIBLE,
                    "Sign in with an approved student account to request a place.");
        }
        String normalizedMessage = JdbcSupport.trimToNull(message);
        if (normalizedMessage != null && normalizedMessage.length() > MAX_MESSAGE_LENGTH) {
            throw new ValidationException(
                    "message", "Your message must be " + MAX_MESSAGE_LENGTH + " characters or fewer.");
        }

        return database.inTransaction(connection -> {
            StoredInvitation invitation = requireAccessibleInvitation(connection, rawToken, true);
            UserAccount student = userDao.findByIdForUpdate(connection, studentUserId)
                    .orElseThrow(() -> new InvitationJoinException(
                            InvitationJoinException.Reason.NOT_ELIGIBLE,
                            "Sign in with an approved student account to request a place."));
            requireApprovedStudent(student);

            long enrolledCount = classDao.countActiveEnrollments(connection, invitation.classId());
            Optional<String> enrollmentStatus = invitationDao.findEnrollmentStatus(
                    connection, invitation.classId(), studentUserId);
            if (enrollmentStatus.isPresent()) {
                if ("ACTIVE".equals(enrollmentStatus.get())) {
                    throw new InvitationJoinException(
                            InvitationJoinException.Reason.ALREADY_ENROLLED,
                            "You are already enrolled in this class.");
                }
                throw new InvitationJoinException(
                        InvitationJoinException.Reason.REQUEST_REVIEWED,
                        "Your enrollment for this class has already been reviewed.");
            }

            Optional<String> requestStatus = invitationDao.findJoinRequestStatus(
                    connection, invitation.classId(), studentUserId);
            if (requestStatus.isPresent()) {
                if ("PENDING".equals(requestStatus.get())) {
                    throw new InvitationJoinException(
                            InvitationJoinException.Reason.ALREADY_REQUESTED,
                            "Your request is already waiting for teacher review.");
                }
                throw new InvitationJoinException(
                        InvitationJoinException.Reason.REQUEST_REVIEWED,
                        reviewedRequestMessage(requestStatus.get()));
            }

            if (isFull(invitation, enrolledCount)) {
                throw new InvitationJoinException(
                        InvitationJoinException.Reason.FULL,
                        "This class is currently full.");
            }

            long requestId = invitationDao.insertJoinRequest(
                    connection, invitation.classId(), studentUserId, normalizedMessage);
            audit(
                    connection,
                    studentUserId,
                    JOIN_REQUEST_CREATED_ACTION,
                    JOIN_REQUEST_ENTITY_TYPE,
                    requestId,
                    "{\"classId\":" + invitation.classId() + "}",
                    clientContext);
            return new JoinRequestResult(requestId, invitation.classId(), invitation.title());
        });
    }

    private StoredInvitation requireAccessibleInvitation(
            Connection connection, String rawToken, boolean forUpdate) throws SQLException {
        if (!InvitationTokens.isValidFormat(rawToken)) {
            throw new InvitationAccessException(InvitationAccessException.Reason.MALFORMED);
        }
        String tokenHash = InvitationTokens.sha256Hex(rawToken);
        Optional<StoredInvitation> found = forUpdate
                ? invitationDao.findByTokenHashForUpdate(connection, tokenHash)
                : invitationDao.findByTokenHash(connection, tokenHash);
        StoredInvitation invitation = found.orElseThrow(
                () -> new InvitationAccessException(InvitationAccessException.Reason.NOT_FOUND));

        if (invitation.invitationCreatedAt() == null) {
            throw new InvitationAccessException(InvitationAccessException.Reason.NOT_ISSUED);
        }
        if (invitation.invitationRevokedAt() != null) {
            throw new InvitationAccessException(InvitationAccessException.Reason.REVOKED);
        }
        if (invitation.invitationExpiresAt() != null
                && !invitation.invitationExpiresAt().isAfter(Instant.now())) {
            throw new InvitationAccessException(InvitationAccessException.Reason.EXPIRED);
        }
        if (!"ACTIVE".equals(invitation.classStatus())) {
            throw new InvitationAccessException(InvitationAccessException.Reason.CLASS_INACTIVE);
        }
        return invitation;
    }

    private PublicInvitation toPublicInvitation(
            Connection connection,
            StoredInvitation invitation,
            Long viewerUserId) throws SQLException {
        long enrolledCount = classDao.countActiveEnrollments(connection, invitation.classId());
        boolean full = isFull(invitation, enrolledCount);
        Integer seatsRemaining = invitation.maxStudents() == null
                ? null
                : Math.max(0, invitation.maxStudents() - (int) Math.min(enrolledCount, Integer.MAX_VALUE));

        ViewerState viewerState = viewerState(connection, invitation, viewerUserId, full);
        return new PublicInvitation(
                invitation.classId(),
                invitation.title(),
                invitation.description(),
                invitation.subjectCode(),
                invitation.subjectName(),
                invitation.teacherName(),
                invitation.maxStudents(),
                enrolledCount,
                seatsRemaining,
                invitation.invitationExpiresAt() == null ? null : Date.from(invitation.invitationExpiresAt()),
                viewerState.state(),
                viewerState.message(),
                viewerState.canRequest());
    }

    private ViewerState viewerState(
            Connection connection,
            StoredInvitation invitation,
            Long viewerUserId,
            boolean full) throws SQLException {
        if (viewerUserId == null) {
            return new ViewerState(
                    "SIGN_IN",
                    "Sign in with an approved student account to request a place.",
                    false);
        }

        Optional<UserAccount> viewer = userDao.findById(connection, viewerUserId);
        if (viewer.isEmpty()
                || viewer.get().role() != UserRole.STUDENT
                || viewer.get().approvalStatus() != ApprovalStatus.APPROVED) {
            return new ViewerState(
                    "NOT_ELIGIBLE",
                    "Use an approved student account to request a place in this class.",
                    false);
        }

        Optional<String> enrollmentStatus = invitationDao.findEnrollmentStatus(
                connection, invitation.classId(), viewerUserId);
        if (enrollmentStatus.isPresent()) {
            if ("ACTIVE".equals(enrollmentStatus.get())) {
                return new ViewerState(
                        "ENROLLED",
                        "You are enrolled in this class.",
                        false);
            }
            return new ViewerState(
                    "REVIEWED",
                    "Your previous enrollment in this class is no longer active.",
                    false);
        }

        Optional<String> requestStatus = invitationDao.findJoinRequestStatus(
                connection, invitation.classId(), viewerUserId);
        if (requestStatus.isPresent()) {
            String status = requestStatus.get();
            if ("PENDING".equals(status)) {
                return new ViewerState(
                        "PENDING",
                        "Your request is waiting for the teacher to review it.",
                        false);
            }
            if ("APPROVED".equals(status) || "ACCEPTED".equals(status)) {
                return new ViewerState(
                        "ACCEPTED",
                        "Your request was accepted. Your enrollment will be completed by the teacher.",
                        false);
            }
            if ("REJECTED".equals(status)) {
                return new ViewerState(
                        "REJECTED",
                        "Your request was not accepted. Contact the teacher if you need more information.",
                        false);
            }
            return new ViewerState(
                    "REVIEWED",
                    "Your previous request for this class was cancelled.",
                    false);
        }

        if (full) {
            return new ViewerState(
                    "FULL",
                    "This class is currently full. Ask the teacher to increase capacity or contact them directly.",
                    false);
        }

        return new ViewerState(
                "AVAILABLE",
                "Add an optional note for the teacher, then request a place.",
                true);
    }

    private static String reviewedRequestMessage(String status) {
        return switch (status) {
            case "APPROVED", "ACCEPTED" -> "Your request for this class has already been accepted.";
            case "REJECTED" -> "Your previous request for this class was not accepted.";
            case "CANCELLED" -> "Your previous request for this class was cancelled.";
            default -> "Your previous request for this class has already been reviewed.";
        };
    }

    private static void requireApprovedStudent(UserAccount student) {
        if (student.role() != UserRole.STUDENT
                || student.approvalStatus() != ApprovalStatus.APPROVED) {
            throw new InvitationJoinException(
                    InvitationJoinException.Reason.NOT_ELIGIBLE,
                    "An approved student account is required to request a place.");
        }
    }

    private UserAccount requireApprovedTeacher(Connection connection, long teacherUserId)
            throws SQLException {
        UserAccount teacher = userDao.findById(connection, teacherUserId)
                .orElseThrow(() -> new ClassAccessException(
                        "Teacher authorization could not be verified."));
        requireApprovedTeacher(teacher);
        return teacher;
    }

    private UserAccount requireApprovedTeacherForUpdate(Connection connection, long teacherUserId)
            throws SQLException {
        UserAccount teacher = userDao.findByIdForUpdate(connection, teacherUserId)
                .orElseThrow(() -> new ClassAccessException(
                        "Teacher authorization could not be verified."));
        requireApprovedTeacher(teacher);
        return teacher;
    }

    private static void requireApprovedTeacher(UserAccount teacher) {
        if (teacher.role() != UserRole.TEACHER || teacher.approvalStatus() != ApprovalStatus.APPROVED) {
            throw new ClassAccessException("An approved teacher account is required to manage invitations.");
        }
    }

    private static void requireIssuedInvitation(StoredInvitation invitation) {
        if (invitation.invitationCreatedAt() == null) {
            throw new ValidationException("invitation", "No invitation has been generated for this class.");
        }
    }

    private static boolean isActiveInvitation(StoredInvitation invitation, Instant now) {
        return invitation.invitationCreatedAt() != null
                && invitation.invitationRevokedAt() == null
                && (invitation.invitationExpiresAt() == null
                        || invitation.invitationExpiresAt().isAfter(now));
    }

    private static boolean isFull(StoredInvitation invitation, long enrolledCount) {
        return invitation.maxStudents() != null && enrolledCount >= invitation.maxStudents();
    }

    private static TeacherInvitation toTeacherInvitation(StoredInvitation invitation, Instant now) {
        String state;
        if (!"ACTIVE".equals(invitation.classStatus())) {
            state = "CLASS_INACTIVE";
        } else if (invitation.invitationCreatedAt() == null) {
            state = "NOT_GENERATED";
        } else if (invitation.invitationRevokedAt() != null) {
            state = "REVOKED";
        } else if (invitation.invitationExpiresAt() != null
                && !invitation.invitationExpiresAt().isAfter(now)) {
            state = "EXPIRED";
        } else {
            state = "ACTIVE";
        }

        boolean classActive = "ACTIVE".equals(invitation.classStatus());
        return new TeacherInvitation(
                invitation.classId(),
                invitation.title(),
                invitation.classStatus(),
                state,
                toDate(invitation.invitationCreatedAt()),
                toDate(invitation.invitationExpiresAt()),
                toDate(invitation.invitationRevokedAt()),
                classActive && !"ACTIVE".equals(state),
                classActive && "ACTIVE".equals(state),
                classActive && "ACTIVE".equals(state),
                classActive && "ACTIVE".equals(state));
    }

    private static Date toDate(Instant value) {
        return value == null ? null : Date.from(value);
    }

    private void audit(
            Connection connection,
            long actorUserId,
            String action,
            String entityType,
            long entityId,
            String details,
            ClientContext clientContext) throws SQLException {
        auditLogDao.insert(
                connection,
                actorUserId,
                action,
                entityType,
                entityId,
                details,
                clientContext == null ? null : clientContext.ipAddress(),
                clientContext == null ? null : clientContext.userAgent());
    }

    private static void requirePositiveIdentifiers(long teacherUserId, long classId) {
        if (teacherUserId <= 0) {
            throw new ValidationException("teacherId", "Teacher authorization could not be verified.");
        }
        if (classId <= 0) {
            throw new ValidationException("classId", "Select a valid class.");
        }
    }

    private record IssuedClass(String title, Instant expiresAt) {
    }

    private record ViewerState(String state, String message, boolean canRequest) {
    }
}
