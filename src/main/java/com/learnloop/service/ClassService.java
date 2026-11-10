package com.learnloop.service;

import com.learnloop.dao.AuditLogDao;
import com.learnloop.dao.ClassDao;
import com.learnloop.dao.DataAccessException;
import com.learnloop.dao.Database;
import com.learnloop.dao.JdbcSupport;
import com.learnloop.dao.SubjectDao;
import com.learnloop.dao.TeacherSubjectAssignmentDao;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.LearningClass;
import com.learnloop.model.NewLearningClass;
import com.learnloop.model.Subject;
import com.learnloop.model.UserAccount;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;
import com.learnloop.service.ClassViews.AssignedSubject;
import com.learnloop.service.ClassViews.ClassDetails;
import com.learnloop.service.ClassViews.ClassSummary;
import com.learnloop.util.InvitationTokens;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ClassService {

    private static final int MAX_TITLE_LENGTH = 160;
    private static final int MAX_DESCRIPTION_LENGTH = 4000;
    private static final String INITIAL_STATUS = "DRAFT";
    private static final String ARCHIVED_STATUS = "ARCHIVED";
    private static final Set<String> EDITABLE_STATUSES = Set.of("DRAFT", "ACTIVE");
    private static final String CLASS_ENTITY_TYPE = "CLASS";
    private static final String CLASS_CREATED_ACTION = "CLASS_CREATED";
    private static final String CLASS_UPDATED_ACTION = "CLASS_UPDATED";
    private static final String CLASS_ARCHIVED_ACTION = "CLASS_ARCHIVED";
    private static final String CLASS_DELETED_ACTION = "CLASS_DELETED";

    private final Database database;
    private final UserDao userDao;
    private final SubjectDao subjectDao;
    private final TeacherSubjectAssignmentDao assignmentDao;
    private final ClassDao classDao;
    private final AuditLogDao auditLogDao;

    public ClassService(Database database) {
        this(database, new UserDao(), new SubjectDao(), new TeacherSubjectAssignmentDao(), new ClassDao(),
                new AuditLogDao());
    }

    public ClassService(
            Database database,
            UserDao userDao,
            SubjectDao subjectDao,
            TeacherSubjectAssignmentDao assignmentDao,
            ClassDao classDao,
            AuditLogDao auditLogDao) {
        this.database = Objects.requireNonNull(database, "database");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.subjectDao = Objects.requireNonNull(subjectDao, "subjectDao");
        this.assignmentDao = Objects.requireNonNull(assignmentDao, "assignmentDao");
        this.classDao = Objects.requireNonNull(classDao, "classDao");
        this.auditLogDao = Objects.requireNonNull(auditLogDao, "auditLogDao");
    }

    public ClassCreationResult createClass(ClassCreationRequest request) {
        Objects.requireNonNull(request, "request");
        String title = normalizeTitle(request.title());
        String description = JdbcSupport.trimToNull(request.description());
        validateIdentifiers(request.teacherUserId(), request.subjectId());
        validateInvitationExpiry(request.invitationExpiresAt());

        if (request.maxStudents() != null && request.maxStudents() <= 0) {
            throw new IllegalArgumentException("Maximum students must be greater than zero when provided.");
        }

        String invitationToken = InvitationTokens.generate();
        String invitationTokenHash = InvitationTokens.sha256Hex(invitationToken);
        Instant invitationCreatedAt = Instant.now();

        return database.inTransaction(connection -> {
            UserAccount teacher = userDao.findByIdForUpdate(connection, request.teacherUserId())
                    .orElseThrow(() -> new ClassCreationException("Teacher account was not found."));
            if (teacher.role() != UserRole.TEACHER) {
                throw new ClassCreationException("Only teacher accounts can create classes.");
            }
            if (teacher.approvalStatus() != ApprovalStatus.APPROVED) {
                throw new ClassCreationException("Teacher account must be approved before creating classes.");
            }

            Subject subject = subjectDao.findByIdForUpdate(connection, request.subjectId())
                    .orElseThrow(() -> new ClassCreationException("Subject was not found."));
            if (!subject.active()) {
                throw new ClassCreationException("Subject is not active.");
            }

            assignmentDao.findActiveForUpdate(connection, request.teacherUserId(), request.subjectId())
                    .orElseThrow(() -> new ClassCreationException(
                            "Teacher is not actively assigned to the selected subject."));

            NewLearningClass newClass = new NewLearningClass(
                    request.subjectId(),
                    request.teacherUserId(),
                    title,
                    description,
                    invitationTokenHash,
                    invitationCreatedAt,
                    request.invitationExpiresAt(),
                    null,
                    INITIAL_STATUS,
                    request.maxStudents());
            long classId = classDao.insert(connection, newClass);

            auditLogDao.insert(
                    connection,
                    request.teacherUserId(),
                    CLASS_CREATED_ACTION,
                    CLASS_ENTITY_TYPE,
                    classId,
                    "{\"subjectId\":" + request.subjectId() + "}",
                    null,
                    null);

            LearningClass created = classDao.findById(connection, classId)
                    .orElseThrow(() -> new DataAccessException("The created class could not be reloaded."));
            return new ClassCreationResult(created, invitationToken);
        });
    }

    public LearningClass createManagedClass(TeacherClassCreateRequest request, ClientContext clientContext) {
        Objects.requireNonNull(request, "request");
        ClassValues values = validateClassValues(
                request.title(), request.description(), request.maxStudents(), INITIAL_STATUS);
        validateManagedIdentifiers(request.teacherUserId(), request.subjectId());

        String invitationTokenHash = InvitationTokens.sha256Hex(InvitationTokens.generate());

        return database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, request.teacherUserId());
            requireAssignableSubject(connection, request.teacherUserId(), request.subjectId());

            NewLearningClass newClass = new NewLearningClass(
                    request.subjectId(),
                    request.teacherUserId(),
                    values.title(),
                    values.description(),
                    invitationTokenHash,
                    null,
                    null,
                    null,
                    INITIAL_STATUS,
                    values.maxStudents());
            long classId = classDao.insert(connection, newClass);

            audit(
                    connection,
                    request.teacherUserId(),
                    CLASS_CREATED_ACTION,
                    classId,
                    "{\"subjectId\":" + request.subjectId() + ",\"status\":\"" + INITIAL_STATUS + "\"}",
                    clientContext);

            return classDao.findById(connection, classId)
                    .orElseThrow(() -> new DataAccessException("The created class could not be reloaded."));
        });
    }

    public List<AssignedSubject> findAssignedSubjects(long teacherUserId) {
        if (teacherUserId <= 0) {
            throw new ClassAccessException("Teacher authorization could not be verified.");
        }
        return database.withConnection(connection -> {
            requireApprovedTeacher(connection, teacherUserId);
            return classDao.findAssignedSubjects(connection, teacherUserId);
        });
    }

    public List<ClassSummary> findOwnedClasses(long teacherUserId) {
        if (teacherUserId <= 0) {
            throw new ClassAccessException("Teacher authorization could not be verified.");
        }
        return database.withConnection(connection -> {
            requireApprovedTeacher(connection, teacherUserId);
            return classDao.findOwnedClasses(connection, teacherUserId);
        });
    }

    public ClassDetails findOwnedClass(long teacherUserId, long classId) {
        if (teacherUserId <= 0) {
            throw new ClassAccessException("Teacher authorization could not be verified.");
        }
        if (classId <= 0) {
            throw new ClassNotFoundException("Class was not found.");
        }
        return database.withConnection(connection -> {
            requireApprovedTeacher(connection, teacherUserId);
            return classDao.findOwnedDetails(connection, classId, teacherUserId)
                    .orElseThrow(() -> new ClassNotFoundException("Class was not found."));
        });
    }

    public void updateClass(TeacherClassUpdateRequest request, ClientContext clientContext) {
        Objects.requireNonNull(request, "request");
        String status = normalizeStatus(request.status());
        ClassValues values = validateClassValues(
                request.title(), request.description(), request.maxStudents(), status);
        validateManagedIdentifiers(request.teacherUserId(), request.subjectId());
        if (request.classId() <= 0) {
            throw new ValidationException("classId", "Select a valid class.");
        }

        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, request.teacherUserId());
            LearningClass current = classDao
                    .findByIdAndTeacherForUpdate(connection, request.classId(), request.teacherUserId())
                    .orElseThrow(() -> new ClassNotFoundException("Class was not found."));
            if (ARCHIVED_STATUS.equals(current.status()) || "CANCELLED".equals(current.status())) {
                throw new ValidationException("status", "Archived or cancelled classes cannot be edited.");
            }
            requireAssignableSubject(connection, request.teacherUserId(), request.subjectId());

            long enrolledCount = classDao.countActiveEnrollments(connection, current.id());
            if (values.maxStudents() != null && values.maxStudents() < enrolledCount) {
                throw new ValidationException(
                        "maxStudents",
                        "Capacity cannot be lower than the " + enrolledCount + " currently enrolled students.");
            }

            int updated = classDao.updateOwned(
                    connection,
                    current.id(),
                    request.teacherUserId(),
                    request.subjectId(),
                    values.title(),
                    values.description(),
                    values.maxStudents(),
                    values.status());
            if (updated != 1) {
                throw new ClassNotFoundException("Class was not found.");
            }
            classDao.revokeInvitationIfClassInactive(connection, current.id(), Instant.now());

            audit(
                    connection,
                    request.teacherUserId(),
                    CLASS_UPDATED_ACTION,
                    current.id(),
                    "{\"subjectId\":" + request.subjectId() + ",\"status\":\"" + values.status() + "\"}",
                    clientContext);
            return null;
        });
    }

    public void archiveClass(long teacherUserId, long classId, ClientContext clientContext) {
        validateOwnedClassIdentifiers(teacherUserId, classId);
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            LearningClass current = classDao
                    .findByIdAndTeacherForUpdate(connection, classId, teacherUserId)
                    .orElseThrow(() -> new ClassNotFoundException("Class was not found."));
            if (ARCHIVED_STATUS.equals(current.status())) {
                return null;
            }

            int updated = classDao.archiveOwned(connection, classId, teacherUserId, ARCHIVED_STATUS);
            if (updated != 1) {
                throw new ClassNotFoundException("Class was not found.");
            }
            classDao.revokeInvitationIfClassInactive(connection, classId, Instant.now());
            audit(
                    connection,
                    teacherUserId,
                    CLASS_ARCHIVED_ACTION,
                    classId,
                    "{\"previousStatus\":\"" + current.status() + "\",\"status\":\"" + ARCHIVED_STATUS + "\"}",
                    clientContext);
            return null;
        });
    }

    public void deleteClass(long teacherUserId, long classId, ClientContext clientContext) {
        validateOwnedClassIdentifiers(teacherUserId, classId);
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            LearningClass current = classDao
                    .findByIdAndTeacherForUpdate(connection, classId, teacherUserId)
                    .orElseThrow(() -> new ClassNotFoundException("Class was not found."));
            if (!INITIAL_STATUS.equals(current.status())) {
                throw new ValidationException(
                        "classId", "Only draft classes can be deleted. Archive this class instead.");
            }

            if (classDao.countEnrollments(connection, classId) > 0
                    || classDao.countJoinRequests(connection, classId) > 0
                    || classDao.countMaterials(connection, classId) > 0
                    || classDao.countAssessments(connection, classId) > 0) {
                throw new ValidationException(
                        "classId",
                        "This class has enrollment, request, material or assessment records. Archive it instead.");
            }

            int deleted = classDao.deleteOwnedDraft(connection, classId, teacherUserId);
            if (deleted != 1) {
                throw new ClassNotFoundException("Class was not found.");
            }
            audit(
                    connection,
                    teacherUserId,
                    CLASS_DELETED_ACTION,
                    classId,
                    "{\"status\":\"" + current.status() + "\"}",
                    clientContext);
            return null;
        });
    }

    private UserAccount requireApprovedTeacher(Connection connection, long teacherUserId) throws SQLException {
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
            throw new ClassAccessException("An approved teacher account is required for class management.");
        }
    }

    private void requireAssignableSubject(Connection connection, long teacherUserId, long subjectId)
            throws SQLException {
        Subject subject = subjectDao.findByIdForUpdate(connection, subjectId)
                .orElseThrow(() -> new ValidationException("subjectId", "Subject was not found."));
        if (!subject.active()) {
            throw new ValidationException("subjectId", "Select an active subject.");
        }
        assignmentDao.findActiveForUpdate(connection, teacherUserId, subjectId)
                .orElseThrow(() -> new ValidationException(
                        "subjectId", "Select a subject currently assigned to you."));
    }

    private void audit(
            Connection connection,
            long teacherUserId,
            String action,
            long classId,
            String details,
            ClientContext clientContext) throws SQLException {
        auditLogDao.insert(
                connection,
                teacherUserId,
                action,
                CLASS_ENTITY_TYPE,
                classId,
                details,
                clientContext == null ? null : clientContext.ipAddress(),
                clientContext == null ? null : clientContext.userAgent());
    }

    private static ClassValues validateClassValues(
            String title, String description, Integer maxStudents, String status) {
        Map<String, String> errors = new LinkedHashMap<>();

        String normalizedTitle = JdbcSupport.trimToNull(title);
        if (normalizedTitle == null) {
            errors.put("title", "Class name is required.");
        } else if (normalizedTitle.length() > MAX_TITLE_LENGTH) {
            errors.put("title", "Class name must be " + MAX_TITLE_LENGTH + " characters or fewer.");
        }

        String normalizedDescription = JdbcSupport.trimToNull(description);
        if (normalizedDescription != null && normalizedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            errors.put(
                    "description",
                    "Description must be " + MAX_DESCRIPTION_LENGTH + " characters or fewer.");
        }

        if (maxStudents != null && maxStudents <= 0) {
            errors.put("maxStudents", "Capacity must be greater than zero when provided.");
        }

        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!EDITABLE_STATUSES.contains(normalizedStatus)) {
            errors.put("status", "Choose Draft or Active for the class status.");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new ClassValues(normalizedTitle, normalizedDescription, maxStudents, normalizedStatus);
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private static void validateManagedIdentifiers(long teacherUserId, long subjectId) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (teacherUserId <= 0) {
            errors.put("teacherId", "Teacher authorization could not be verified.");
        }
        if (subjectId <= 0) {
            errors.put("subjectId", "Select a subject.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static void validateOwnedClassIdentifiers(long teacherUserId, long classId) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (teacherUserId <= 0) {
            errors.put("teacherId", "Teacher authorization could not be verified.");
        }
        if (classId <= 0) {
            errors.put("classId", "Select a valid class.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static void validateIdentifiers(long teacherUserId, long subjectId) {
        if (teacherUserId <= 0) {
            throw new IllegalArgumentException("Teacher user id must be greater than zero.");
        }
        if (subjectId <= 0) {
            throw new IllegalArgumentException("Subject id must be greater than zero.");
        }
    }

    private static void validateInvitationExpiry(Instant invitationExpiresAt) {
        if (invitationExpiresAt != null && !invitationExpiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Invitation expiry must be in the future.");
        }
    }

    private static String normalizeTitle(String title) {
        String normalized = JdbcSupport.trimToNull(title);
        if (normalized == null) {
            throw new IllegalArgumentException("Class title is required.");
        }
        if (normalized.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "Class title must be " + MAX_TITLE_LENGTH + " characters or fewer.");
        }
        return normalized;
    }

    private record ClassValues(String title, String description, Integer maxStudents, String status) {
    }
}
