package com.learnloop.service;

import com.learnloop.dao.AuditLogDao;
import com.learnloop.dao.ClassDao;
import com.learnloop.dao.ClassroomDao;
import com.learnloop.dao.ClassroomDao.MaterialUpdate;
import com.learnloop.dao.ClassroomDao.NewMaterial;
import com.learnloop.dao.Database;
import com.learnloop.dao.EnrollmentDao;
import com.learnloop.dao.JdbcSupport;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.LearningClass;
import com.learnloop.model.UserAccount;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;
import com.learnloop.service.ClassroomViews.Classroom;
import com.learnloop.service.ClassroomViews.Material;
import com.learnloop.service.ClassroomViews.MaterialAccess;
import com.learnloop.util.ExternalResourceUrls;
import com.learnloop.util.MaterialFileStorage;
import com.learnloop.util.MaterialFileStorage.StoredFile;

import jakarta.servlet.http.Part;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class ClassroomService {

    private static final int MAX_TITLE_LENGTH = 160;
    private static final int MAX_DESCRIPTION_LENGTH = 4000;
    private static final int MAX_TEXT_LENGTH = 100_000;
    private static final Set<String> MATERIAL_TYPES = Set.of("LINK", "FILE", "TEXT");
    private static final Set<String> EDITABLE_STATUSES = Set.of("DRAFT", "ACTIVE");
    private static final String MATERIAL_ENTITY_TYPE = "MATERIAL";
    private static final String CLASS_ENTITY_TYPE = "CLASS";

    private final Database database;
    private final ClassroomDao classroomDao;
    private final ClassDao classDao;
    private final EnrollmentDao enrollmentDao;
    private final UserDao userDao;
    private final AuditLogDao auditLogDao;
    private final MaterialFileStorage fileStorage;

    public ClassroomService(Database database) {
        this(database, new ClassroomDao(), new ClassDao(), new EnrollmentDao(), new UserDao(),
                new AuditLogDao(), new MaterialFileStorage());
    }

    public ClassroomService(
            Database database,
            ClassroomDao classroomDao,
            ClassDao classDao,
            EnrollmentDao enrollmentDao,
            UserDao userDao,
            AuditLogDao auditLogDao,
            MaterialFileStorage fileStorage) {
        this.database = Objects.requireNonNull(database, "database");
        this.classroomDao = Objects.requireNonNull(classroomDao, "classroomDao");
        this.classDao = Objects.requireNonNull(classDao, "classDao");
        this.enrollmentDao = Objects.requireNonNull(enrollmentDao, "enrollmentDao");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.auditLogDao = Objects.requireNonNull(auditLogDao, "auditLogDao");
        this.fileStorage = Objects.requireNonNull(fileStorage, "fileStorage");
    }

    public Classroom teacherClassroom(long teacherUserId, long classId) {
        requirePositive(teacherUserId, "Teacher authorization could not be verified.");
        requirePositive(classId, "Class was not found.");
        return database.withConnection(connection -> {
            requireApprovedUser(connection, teacherUserId, UserRole.TEACHER);
            Classroom classroom = classroomDao.findClassroom(connection, classId, false)
                    .orElseThrow(() -> new ClassroomAccessException("Class was not found."));
            if (classroom.getTeacherUserId() != teacherUserId) {
                throw new ClassroomAccessException("Class was not found.");
            }
            return classroom;
        });
    }

    public Classroom studentClassroom(long studentUserId, long classId) {
        requirePositive(studentUserId, "Student authorization could not be verified.");
        requirePositive(classId, "Class was not found.");
        return database.withConnection(connection -> {
            requireApprovedUser(connection, studentUserId, UserRole.STUDENT);
            Classroom classroom = classroomDao.findClassroom(connection, classId, true)
                    .orElseThrow(() -> new ClassroomAccessException("Class was not found."));
            if (!"ACTIVE".equals(classroom.getStatus())) {
                throw new ClassroomAccessException("Class was not found.");
            }
            if (!enrollmentDao.exists(connection, classId, studentUserId)) {
                throw new ClassroomAccessException("Class was not found.");
            }
            return classroom;
        });
    }

    public Classroom adminClassroom(long adminUserId, long classId) {
        requirePositive(adminUserId, "Administrator authorization could not be verified.");
        requirePositive(classId, "Class was not found.");
        return database.withConnection(connection -> {
            requireApprovedUser(connection, adminUserId, UserRole.ADMIN);
            return classroomDao.findClassroom(connection, classId, false)
                    .orElseThrow(() -> new ClassroomAccessException("Class was not found."));
        });
    }

    public MaterialAccess openMaterial(long userId, UserRole role, long materialId) {
        requirePositive(userId, "Authorization could not be verified.");
        requirePositive(materialId, "Material was not found.");
        Objects.requireNonNull(role, "role");
        return database.withConnection(connection -> {
            requireApprovedUser(connection, userId, role);
            Material material = classroomDao.findMaterial(connection, materialId)
                    .orElseThrow(() -> new ClassroomAccessException("Material was not found."));
            switch (role) {
                case ADMIN -> {
                    // Administrators may inspect every material, including drafts.
                }
                case TEACHER -> {
                    if (material.getTeacherUserId() != userId) {
                        throw new ClassroomAccessException("Material was not found.");
                    }
                }
                case STUDENT -> {
                    if (!material.isPublished()
                            || !"ACTIVE".equals(material.getClassStatus())
                            || !enrollmentDao.exists(connection, material.getClassId(), userId)) {
                        throw new ClassroomAccessException("Material was not found.");
                    }
                }
                default -> throw new ClassroomAccessException("Material was not found.");
            }
            Classroom classroom = classroomDao
                    .findClassroom(connection, material.getClassId(), role == UserRole.STUDENT)
                    .orElseThrow(() -> new ClassroomAccessException("Material was not found."));
            return new MaterialAccess(classroom, material);
        });
    }

    public String openMeetingUrl(long userId, UserRole role, long classId) {
        requirePositive(userId, "Authorization could not be verified.");
        requirePositive(classId, "Class was not found.");
        Objects.requireNonNull(role, "role");
        return database.withConnection(connection -> {
            requireApprovedUser(connection, userId, role);
            Classroom classroom = classroomDao
                    .findClassroom(connection, classId, role == UserRole.STUDENT)
                    .orElseThrow(() -> new ClassroomAccessException("Live session link was not found."));
            switch (role) {
                case ADMIN -> {
                    // Administrators can inspect the live-session link.
                }
                case TEACHER -> {
                    if (classroom.getTeacherUserId() != userId) {
                        throw new ClassroomAccessException("Live session link was not found.");
                    }
                }
                case STUDENT -> {
                    if (!"ACTIVE".equals(classroom.getStatus())
                            || !enrollmentDao.exists(connection, classId, userId)) {
                        throw new ClassroomAccessException("Live session link was not found.");
                    }
                }
                default -> throw new ClassroomAccessException("Live session link was not found.");
            }
            if (!classroom.isLiveSessionConfigured()) {
                throw new ClassroomAccessException("Live session link was not found.");
            }
            try {
                return ExternalResourceUrls.meetingUrl(classroom.getMeetingUrl());
            } catch (IllegalArgumentException exception) {
                throw new ClassroomAccessException("Live session link was not found.");
            }
        });
    }

    public long createMaterial(
            long teacherUserId,
            long classId,
            MaterialForm form,
            Part filePart,
            ClientContext clientContext) {
        requirePositive(teacherUserId, "Teacher authorization could not be verified.");
        requirePositive(classId, "Class was not found.");
        Objects.requireNonNull(form, "form");
        ValidatedForm values = validateForm(form);
        StoredFile storedFile = null;
        if ("FILE".equals(values.materialType())) {
            storedFile = storeUpload(filePart, true);
        } else {
            rejectUnexpectedUpload(filePart);
        }

        StoredFile createdFile = storedFile;
        try {
            return database.inTransaction(connection -> {
                requireApprovedTeacherForUpdate(connection, teacherUserId);
                LearningClass current = requireOwnedEditableClassForUpdate(connection, classId, teacherUserId);
                int position = classroomDao.nextPosition(connection, current.id());
                long materialId = classroomDao.insertMaterial(connection, new NewMaterial(
                        current.id(),
                        teacherUserId,
                        values.title(),
                        values.description(),
                        values.materialType(),
                        values.resourceUrl(),
                        values.contentText(),
                        createdFile == null ? null : createdFile.originalName(),
                        createdFile == null ? null : createdFile.storedName(),
                        createdFile == null ? null : createdFile.mimeType(),
                        createdFile == null ? null : createdFile.sizeBytes(),
                        position,
                        false,
                        null));
                auditMaterial(
                        connection,
                        teacherUserId,
                        "MATERIAL_CREATED",
                        materialId,
                        current.id(),
                        "{\"materialType\":\"" + values.materialType() + "\",\"published\":false}",
                        clientContext);
                return materialId;
            });
        } catch (RuntimeException exception) {
            if (createdFile != null) {
                fileStorage.deleteQuietly(createdFile.storedName());
            }
            throw exception;
        }
    }

    public void updateMaterial(
            long teacherUserId,
            long classId,
            long materialId,
            MaterialForm form,
            Part filePart,
            ClientContext clientContext) {
        requirePositive(teacherUserId, "Teacher authorization could not be verified.");
        requirePositive(classId, "Class was not found.");
        requirePositive(materialId, "Material was not found.");
        Objects.requireNonNull(form, "form");
        ValidatedForm values = validateForm(form);
        StoredFile replacement = "FILE".equals(values.materialType()) ? storeUpload(filePart, false) : null;
        if (replacement == null) {
            rejectUnexpectedUpload(filePart);
        }
        AtomicReference<String> obsoleteStoredName = new AtomicReference<>();
        try {
            database.inTransaction(connection -> {
                requireApprovedTeacherForUpdate(connection, teacherUserId);
                LearningClass current = requireOwnedEditableClassForUpdate(connection, classId, teacherUserId);
                Material existing = requireMaterialForClass(connection, materialId, current.id());

                String fileName;
                String storedName;
                String mimeType;
                Long fileSize;
                if ("FILE".equals(values.materialType())) {
                    if (replacement != null) {
                        fileName = replacement.originalName();
                        storedName = replacement.storedName();
                        mimeType = replacement.mimeType();
                        fileSize = replacement.sizeBytes();
                        if (existing.isFile() && existing.getStoredName() != null) {
                            obsoleteStoredName.set(existing.getStoredName());
                        }
                    } else if (existing.isFile()) {
                        fileName = existing.getFileName();
                        storedName = existing.getStoredName();
                        mimeType = existing.getMimeType();
                        fileSize = existing.getFileSizeBytes();
                    } else {
                        throw new ValidationException("file", "Choose a file to upload.");
                    }
                } else {
                    fileName = null;
                    storedName = null;
                    mimeType = null;
                    fileSize = null;
                    if (existing.isFile() && existing.getStoredName() != null) {
                        obsoleteStoredName.set(existing.getStoredName());
                    }
                }

                int updated = classroomDao.updateMaterial(connection, materialId, current.id(),
                        new MaterialUpdate(
                                values.title(),
                                values.description(),
                                values.materialType(),
                                values.resourceUrl(),
                                values.contentText(),
                                fileName,
                                storedName,
                                mimeType,
                                fileSize));
                if (updated != 1) {
                    throw new ClassroomAccessException("Material was not found.");
                }
                auditMaterial(
                        connection,
                        teacherUserId,
                        "MATERIAL_UPDATED",
                        materialId,
                        current.id(),
                        "{\"materialType\":\"" + values.materialType() + "\"}",
                        clientContext);
                return null;
            });
        } catch (RuntimeException exception) {
            if (replacement != null) {
                fileStorage.deleteQuietly(replacement.storedName());
            }
            throw exception;
        }
        fileStorage.deleteQuietly(obsoleteStoredName.get());
    }

    public void publishMaterial(
            long teacherUserId, long classId, long materialId, ClientContext clientContext) {
        setMaterialPublished(teacherUserId, classId, materialId, true, "MATERIAL_PUBLISHED", clientContext);
    }

    public void unpublishMaterial(
            long teacherUserId, long classId, long materialId, ClientContext clientContext) {
        setMaterialPublished(teacherUserId, classId, materialId, false, "MATERIAL_UNPUBLISHED", clientContext);
    }

    public void deleteMaterial(
            long teacherUserId, long classId, long materialId, ClientContext clientContext) {
        requirePositive(teacherUserId, "Teacher authorization could not be verified.");
        requirePositive(classId, "Class was not found.");
        requirePositive(materialId, "Material was not found.");
        AtomicReference<String> storedName = new AtomicReference<>();
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            LearningClass current = requireOwnedEditableClassForUpdate(connection, classId, teacherUserId);
            Material existing = requireMaterialForClass(connection, materialId, current.id());
            storedName.set(existing.getStoredName());
            int deleted = classroomDao.deleteMaterial(connection, materialId, current.id());
            if (deleted != 1) {
                throw new ClassroomAccessException("Material was not found.");
            }
            auditMaterial(
                    connection,
                    teacherUserId,
                    "MATERIAL_DELETED",
                    materialId,
                    current.id(),
                    "{\"title\":\"" + escape(existing.getTitle()) + "\"}",
                    clientContext);
            return null;
        });
        fileStorage.deleteQuietly(storedName.get());
    }

    public void updateMeetingUrl(
            long teacherUserId, long classId, String meetingUrl, ClientContext clientContext) {
        requirePositive(teacherUserId, "Teacher authorization could not be verified.");
        requirePositive(classId, "Class was not found.");
        String normalized = null;
        if (meetingUrl != null && !meetingUrl.isBlank()) {
            try {
                normalized = ExternalResourceUrls.meetingUrl(meetingUrl);
            } catch (IllegalArgumentException exception) {
                throw new ValidationException("meetingUrl", exception.getMessage());
            }
        }
        String value = normalized;
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            LearningClass current = requireOwnedEditableClassForUpdate(connection, classId, teacherUserId);
            int updated = classroomDao.updateMeetingUrl(connection, current.id(), teacherUserId, value);
            if (updated != 1) {
                throw new ClassroomAccessException("Class was not found.");
            }
            auditLogDao.insert(
                    connection,
                    teacherUserId,
                    "CLASS_MEETING_UPDATED",
                    CLASS_ENTITY_TYPE,
                    current.id(),
                    "{\"configured\":" + (value != null) + "}",
                    clientContext == null ? null : clientContext.ipAddress(),
                    clientContext == null ? null : clientContext.userAgent());
            return null;
        });
    }

    private void setMaterialPublished(
            long teacherUserId,
            long classId,
            long materialId,
            boolean published,
            String action,
            ClientContext clientContext) {
        requirePositive(teacherUserId, "Teacher authorization could not be verified.");
        requirePositive(classId, "Class was not found.");
        requirePositive(materialId, "Material was not found.");
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            LearningClass current = requireOwnedEditableClassForUpdate(connection, classId, teacherUserId);
            requireMaterialForClass(connection, materialId, current.id());
            int updated = classroomDao.setPublished(connection, materialId, current.id(), published);
            if (updated != 1) {
                throw new ClassroomAccessException("Material was not found.");
            }
            auditMaterial(
                    connection,
                    teacherUserId,
                    action,
                    materialId,
                    current.id(),
                    "{\"published\":" + published + "}",
                    clientContext);
            return null;
        });
    }

    private UserAccount requireApprovedUser(Connection connection, long userId, UserRole expectedRole)
            throws SQLException {
        UserAccount user = userDao.findById(connection, userId)
                .orElseThrow(() -> new ClassroomAccessException("Authorization could not be verified."));
        if (user.role() != expectedRole || user.approvalStatus() != ApprovalStatus.APPROVED) {
            throw new ClassroomAccessException("Authorization could not be verified.");
        }
        return user;
    }

    private UserAccount requireApprovedTeacherForUpdate(Connection connection, long teacherUserId)
            throws SQLException {
        UserAccount teacher = userDao.findByIdForUpdate(connection, teacherUserId)
                .orElseThrow(() -> new ClassroomAccessException("Authorization could not be verified."));
        if (teacher.role() != UserRole.TEACHER || teacher.approvalStatus() != ApprovalStatus.APPROVED) {
            throw new ClassroomAccessException("An approved teacher account is required for class management.");
        }
        return teacher;
    }

    private LearningClass requireOwnedEditableClassForUpdate(
            Connection connection, long classId, long teacherUserId) throws SQLException {
        LearningClass current = classDao.findByIdAndTeacherForUpdate(connection, classId, teacherUserId)
                .orElseThrow(() -> new ClassroomAccessException("Class was not found."));
        if (!EDITABLE_STATUSES.contains(current.status())) {
            throw new ValidationException(
                    "class", "Archived or cancelled classes cannot be changed.");
        }
        return current;
    }

    private Material requireMaterialForClass(Connection connection, long materialId, long classId)
            throws SQLException {
        return classroomDao.findMaterial(connection, materialId)
                .filter(material -> material.getClassId() == classId)
                .orElseThrow(() -> new ClassroomAccessException("Material was not found."));
    }

    private void auditMaterial(
            Connection connection,
            long actorUserId,
            String action,
            long materialId,
            long classId,
            String details,
            ClientContext clientContext) throws SQLException {
        auditLogDao.insert(
                connection,
                actorUserId,
                action,
                MATERIAL_ENTITY_TYPE,
                materialId,
                details,
                clientContext == null ? null : clientContext.ipAddress(),
                clientContext == null ? null : clientContext.userAgent());
    }

    private StoredFile storeUpload(Part filePart, boolean required) {
        if (filePart == null || filePart.getSize() <= 0) {
            if (required) {
                throw new ValidationException("file", "Choose a file to upload.");
            }
            return null;
        }
        try {
            return fileStorage.store(filePart);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("file", exception.getMessage());
        } catch (IOException exception) {
            throw new ValidationException("file", "The file could not be stored. Please try again.");
        }
    }

    private static void rejectUnexpectedUpload(Part filePart) {
        if (filePart != null && filePart.getSize() > 0) {
            throw new ValidationException("file", "Only file materials accept an uploaded file.");
        }
    }

    private static ValidatedForm validateForm(MaterialForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        String title = JdbcSupport.trimToNull(form.title());
        if (title == null) {
            errors.put("title", "Material title is required.");
        } else if (title.length() > MAX_TITLE_LENGTH) {
            errors.put("title", "Material title must be " + MAX_TITLE_LENGTH + " characters or fewer.");
        }
        String description = JdbcSupport.trimToNull(form.description());
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            errors.put("description",
                    "Description must be " + MAX_DESCRIPTION_LENGTH + " characters or fewer.");
        }
        String materialType = form.materialType() == null
                ? ""
                : form.materialType().trim().toUpperCase(Locale.ROOT);
        if (!MATERIAL_TYPES.contains(materialType)) {
            errors.put("materialType", "Choose a link, file or reading material.");
        }
        String resourceUrl = null;
        String contentText = null;
        if ("LINK".equals(materialType)) {
            resourceUrl = ExternalResourceUrls.safeHttpUrl(form.resourceUrl());
            if (resourceUrl == null) {
                errors.put("resourceUrl", "Enter a valid http:// or https:// link.");
            }
        } else if ("TEXT".equals(materialType)) {
            contentText = JdbcSupport.trimToNull(form.contentText());
            if (contentText == null) {
                errors.put("contentText", "Reading content is required.");
            } else if (contentText.length() > MAX_TEXT_LENGTH) {
                errors.put("contentText",
                        "Reading content must be " + MAX_TEXT_LENGTH + " characters or fewer.");
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new ValidatedForm(title, description, materialType, resourceUrl, contentText);
    }

    private static void requirePositive(long value, String message) {
        if (value <= 0) {
            throw new ClassroomAccessException(message);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record MaterialForm(
            String title,
            String description,
            String materialType,
            String resourceUrl,
            String contentText
    ) {
    }

    private record ValidatedForm(
            String title,
            String description,
            String materialType,
            String resourceUrl,
            String contentText
    ) {
    }
}
