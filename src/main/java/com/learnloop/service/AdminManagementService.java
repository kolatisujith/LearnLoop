package com.learnloop.service;

import com.learnloop.dao.AuditLogDao;
import com.learnloop.dao.DataAccessException;
import com.learnloop.dao.Database;
import com.learnloop.dao.JdbcSupport;
import com.learnloop.dao.SubjectDao;
import com.learnloop.dao.TeacherSubjectAssignmentDao;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.Subject;
import com.learnloop.model.TeacherSubjectAssignment;
import com.learnloop.model.UserAccount;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class AdminManagementService {

    private static final String USER_ENTITY = "USER";
    private static final String SUBJECT_ENTITY = "SUBJECT";
    private static final String ASSIGNMENT_ENTITY = "TEACHER_SUBJECT_ASSIGNMENT";
    private static final int MAX_SUBJECT_CODE_LENGTH = 20;
    private static final int MAX_SUBJECT_NAME_LENGTH = 120;
    private static final int MAX_SUBJECT_DESCRIPTION_LENGTH = 4000;
    private static final int MAX_REJECTION_REASON_LENGTH = 500;
    private static final Pattern SUBJECT_CODE_PATTERN = Pattern.compile("[A-Z0-9][A-Z0-9_-]{1,19}");

    private final Database database;
    private final UserDao userDao;
    private final SubjectDao subjectDao;
    private final TeacherSubjectAssignmentDao assignmentDao;
    private final AuditLogDao auditLogDao;

    public AdminManagementService(Database database) {
        this(database, new UserDao(), new SubjectDao(), new TeacherSubjectAssignmentDao(), new AuditLogDao());
    }

    public AdminManagementService(
            Database database,
            UserDao userDao,
            SubjectDao subjectDao,
            TeacherSubjectAssignmentDao assignmentDao,
            AuditLogDao auditLogDao) {
        this.database = Objects.requireNonNull(database, "database");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.subjectDao = Objects.requireNonNull(subjectDao, "subjectDao");
        this.assignmentDao = Objects.requireNonNull(assignmentDao, "assignmentDao");
        this.auditLogDao = Objects.requireNonNull(auditLogDao, "auditLogDao");
    }

    public void approveTeacher(long adminUserId, long teacherUserId, ClientContext clientContext) {
        validateId(adminUserId, "administrator");
        validateId(teacherUserId, "teacher");
        database.inTransaction(connection -> {
            requireAdmin(connection, adminUserId);
            UserAccount teacher = requirePendingTeacher(connection, teacherUserId);
            userDao.markApproved(connection, teacher.id(), adminUserId, Instant.now());
            audit(
                    connection,
                    adminUserId,
                    "USER_APPROVED",
                    USER_ENTITY,
                    teacher.id(),
                    "{\"role\":\"TEACHER\",\"approvalStatus\":\"APPROVED\"}",
                    clientContext);
            return null;
        });
    }

    public void rejectTeacher(
            long adminUserId,
            long teacherUserId,
            String reason,
            ClientContext clientContext) {
        validateId(adminUserId, "administrator");
        validateId(teacherUserId, "teacher");
        String rejectionReason = normalizeReason(reason);
        database.inTransaction(connection -> {
            requireAdmin(connection, adminUserId);
            UserAccount teacher = requirePendingTeacher(connection, teacherUserId);
            userDao.markRejected(connection, teacher.id(), rejectionReason);
            audit(
                    connection,
                    adminUserId,
                    "USER_REJECTED",
                    USER_ENTITY,
                    teacher.id(),
                    "{\"role\":\"TEACHER\",\"approvalStatus\":\"REJECTED\",\"reason\":"
                            + jsonString(rejectionReason) + "}",
                    clientContext);
            return null;
        });
    }

    public long createSubject(
            long adminUserId,
            String code,
            String name,
            String description,
            ClientContext clientContext) {
        validateId(adminUserId, "administrator");
        SubjectValues values = validateSubject(code, name, description);
        return database.inTransaction(connection -> {
            requireAdmin(connection, adminUserId);
            ensureSubjectUnique(connection, values, null);
            long subjectId;
            try {
                subjectId = subjectDao.insert(
                        connection, values.code(), values.name(), values.description(), true);
            } catch (SQLException exception) {
                throw mapDuplicateSubject(exception);
            }
            audit(
                    connection,
                    adminUserId,
                    "SUBJECT_CREATED",
                    SUBJECT_ENTITY,
                    subjectId,
                    "{\"code\":" + jsonString(values.code()) + "}",
                    clientContext);
            return subjectId;
        });
    }

    public void updateSubject(
            long adminUserId,
            long subjectId,
            String code,
            String name,
            String description,
            ClientContext clientContext) {
        validateId(adminUserId, "administrator");
        validateId(subjectId, "subject");
        SubjectValues values = validateSubject(code, name, description);
        database.inTransaction(connection -> {
            requireAdmin(connection, adminUserId);
            subjectDao.findByIdForUpdate(connection, subjectId)
                    .orElseThrow(() -> new ValidationException("subjectId", "Subject was not found."));
            ensureSubjectUnique(connection, values, subjectId);
            try {
                subjectDao.updateDetails(
                        connection, subjectId, values.code(), values.name(), values.description());
            } catch (SQLException exception) {
                throw mapDuplicateSubject(exception);
            }
            audit(
                    connection,
                    adminUserId,
                    "SUBJECT_UPDATED",
                    SUBJECT_ENTITY,
                    subjectId,
                    "{\"code\":" + jsonString(values.code()) + "}",
                    clientContext);
            return null;
        });
    }

    public void setSubjectActive(
            long adminUserId,
            long subjectId,
            boolean active,
            ClientContext clientContext) {
        validateId(adminUserId, "administrator");
        validateId(subjectId, "subject");
        database.inTransaction(connection -> {
            requireAdmin(connection, adminUserId);
            Subject subject = subjectDao.findByIdForUpdate(connection, subjectId)
                    .orElseThrow(() -> new ValidationException("subjectId", "Subject was not found."));
            if (subject.active() == active) {
                return null;
            }
            subjectDao.setActive(connection, subjectId, active);
            audit(
                    connection,
                    adminUserId,
                    active ? "SUBJECT_ACTIVATED" : "SUBJECT_DEACTIVATED",
                    SUBJECT_ENTITY,
                    subjectId,
                    "{\"code\":" + jsonString(subject.code()) + ",\"active\":" + active + "}",
                    clientContext);
            return null;
        });
    }

    public void assignTeacher(
            long adminUserId,
            long teacherUserId,
            long subjectId,
            ClientContext clientContext) {
        validateId(adminUserId, "administrator");
        validateId(teacherUserId, "teacher");
        validateId(subjectId, "subject");
        database.inTransaction(connection -> {
            requireAdmin(connection, adminUserId);
            UserAccount teacher = userDao.findByIdForUpdate(connection, teacherUserId)
                    .orElseThrow(() -> new ValidationException("teacherId", "Teacher was not found."));
            if (teacher.role() != UserRole.TEACHER) {
                throw new ValidationException("teacherId", "Select a teacher account.");
            }
            if (teacher.approvalStatus() != ApprovalStatus.APPROVED) {
                throw new ValidationException("teacherId", "Only approved teachers can be assigned to subjects.");
            }

            Subject subject = subjectDao.findByIdForUpdate(connection, subjectId)
                    .orElseThrow(() -> new ValidationException("subjectId", "Subject was not found."));
            if (!subject.active()) {
                throw new ValidationException("subjectId", "Inactive subjects cannot receive new assignments.");
            }

            Optional<TeacherSubjectAssignment> existing =
                    assignmentDao.findForUpdate(connection, teacherUserId, subjectId);
            if (existing.isPresent() && existing.get().active()) {
                throw new ValidationException(
                        "teacherId", "This teacher is already assigned to the selected subject.");
            }

            assignmentDao.upsertActive(connection, teacherUserId, subjectId, adminUserId);
            TeacherSubjectAssignment assignment = assignmentDao
                    .findForUpdate(connection, teacherUserId, subjectId)
                    .orElseThrow(() -> new DataAccessException("The teacher assignment could not be reloaded."));
            audit(
                    connection,
                    adminUserId,
                    "TEACHER_ASSIGNED",
                    ASSIGNMENT_ENTITY,
                    assignment.id(),
                    "{\"teacherUserId\":" + teacherUserId + ",\"subjectId\":" + subjectId + "}",
                    clientContext);
            return null;
        });
    }

    public void removeAssignment(
            long adminUserId,
            long teacherUserId,
            long subjectId,
            ClientContext clientContext) {
        validateId(adminUserId, "administrator");
        validateId(teacherUserId, "teacher");
        validateId(subjectId, "subject");
        database.inTransaction(connection -> {
            requireAdmin(connection, adminUserId);
            TeacherSubjectAssignment assignment = assignmentDao
                    .findForUpdate(connection, teacherUserId, subjectId)
                    .filter(TeacherSubjectAssignment::active)
                    .orElseThrow(() -> new ValidationException(
                            "teacherId", "The selected teacher assignment is not active."));
            assignmentDao.deactivate(connection, teacherUserId, subjectId);
            audit(
                    connection,
                    adminUserId,
                    "TEACHER_UNASSIGNED",
                    ASSIGNMENT_ENTITY,
                    assignment.id(),
                    "{\"teacherUserId\":" + teacherUserId + ",\"subjectId\":" + subjectId + "}",
                    clientContext);
            return null;
        });
    }

    private UserAccount requireAdmin(java.sql.Connection connection, long adminUserId) throws SQLException {
        UserAccount admin = userDao.findByIdForUpdate(connection, adminUserId)
                .orElseThrow(() -> new AdminAuthorizationException(
                        "Administrator authorization could not be verified."));
        if (admin.role() != UserRole.ADMIN || admin.approvalStatus() != ApprovalStatus.APPROVED) {
            throw new AdminAuthorizationException("Administrator authorization is required for this action.");
        }
        return admin;
    }

    private UserAccount requirePendingTeacher(java.sql.Connection connection, long teacherUserId)
            throws SQLException {
        UserAccount teacher = userDao.findByIdForUpdate(connection, teacherUserId)
                .orElseThrow(() -> new ValidationException("teacherId", "Teacher was not found."));
        if (teacher.role() != UserRole.TEACHER) {
            throw new ValidationException("teacherId", "The selected account is not a teacher.");
        }
        if (teacher.approvalStatus() != ApprovalStatus.PENDING) {
            throw new ValidationException(
                    "teacherId", "Only pending teacher accounts can be reviewed.");
        }
        return teacher;
    }

    private void ensureSubjectUnique(
            java.sql.Connection connection,
            SubjectValues values,
            Long currentSubjectId) throws SQLException {
        long allowedSubjectId = currentSubjectId == null ? -1 : currentSubjectId;
        Optional<Subject> byCode = subjectDao.findByCode(connection, values.code());
        if (byCode.isPresent() && byCode.get().id() != allowedSubjectId) {
            throw new ValidationException("code", "A subject with this code already exists.");
        }
        Optional<Subject> byName = subjectDao.findByName(connection, values.name());
        if (byName.isPresent() && byName.get().id() != allowedSubjectId) {
            throw new ValidationException("name", "A subject with this name already exists.");
        }
    }

    private void audit(
            java.sql.Connection connection,
            long actorUserId,
            String action,
            String entityType,
            long entityId,
            String detailsJson,
            ClientContext clientContext) throws SQLException {
        auditLogDao.insert(
                connection,
                actorUserId,
                action,
                entityType,
                entityId,
                detailsJson,
                clientContext == null ? null : clientContext.ipAddress(),
                clientContext == null ? null : clientContext.userAgent());
    }

    private static SubjectValues validateSubject(String code, String name, String description) {
        Map<String, String> errors = new LinkedHashMap<>();
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            errors.put("code", "Subject code is required.");
        } else if (normalizedCode.length() > MAX_SUBJECT_CODE_LENGTH) {
            errors.put("code", "Subject code must be " + MAX_SUBJECT_CODE_LENGTH + " characters or fewer.");
        } else if (!SUBJECT_CODE_PATTERN.matcher(normalizedCode).matches()) {
            errors.put("code", "Use 2-20 letters, numbers, underscores or hyphens, starting with a letter or number.");
        }

        String normalizedName = JdbcSupport.trimToNull(name);
        if (normalizedName == null) {
            errors.put("name", "Subject name is required.");
        } else if (normalizedName.length() > MAX_SUBJECT_NAME_LENGTH) {
            errors.put("name", "Subject name must be " + MAX_SUBJECT_NAME_LENGTH + " characters or fewer.");
        }

        String normalizedDescription = JdbcSupport.trimToNull(description);
        if (normalizedDescription != null
                && normalizedDescription.length() > MAX_SUBJECT_DESCRIPTION_LENGTH) {
            errors.put("description",
                    "Description must be " + MAX_SUBJECT_DESCRIPTION_LENGTH + " characters or fewer.");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new SubjectValues(normalizedCode, normalizedName, normalizedDescription);
    }

    private static String normalizeCode(String code) {
        String normalized = JdbcSupport.trimToNull(code);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeReason(String reason) {
        String normalized = JdbcSupport.trimToNull(reason);
        if (normalized == null) {
            throw new ValidationException("reason", "Enter a reason for rejecting this teacher.");
        }
        if (normalized.length() > MAX_REJECTION_REASON_LENGTH) {
            throw new ValidationException(
                    "reason", "Rejection reason must be " + MAX_REJECTION_REASON_LENGTH + " characters or fewer.");
        }
        return normalized;
    }

    private static void validateId(long id, String label) {
        if (id <= 0) {
            throw new ValidationException(label + "Id", "A valid " + label + " is required.");
        }
    }

    private static ValidationException mapDuplicateSubject(SQLException exception) {
        if (isDuplicateKey(exception)) {
            return new ValidationException("code", "A subject with this code or name already exists.");
        }
        throw new DataAccessException("The subject could not be saved.", exception);
    }

    private static boolean isDuplicateKey(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (current instanceof SQLException sqlException
                    && "23000".equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String jsonString(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    private record SubjectValues(String code, String name, String description) {
    }
}
