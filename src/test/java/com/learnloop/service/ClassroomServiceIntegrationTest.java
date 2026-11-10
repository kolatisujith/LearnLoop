package com.learnloop.service;

import com.learnloop.dao.Database;
import com.learnloop.dao.TeacherSubjectAssignmentDao;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.LearningClass;
import com.learnloop.model.NewUser;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;
import com.learnloop.service.ClassroomViews.Classroom;
import com.learnloop.service.ClassroomViews.Material;
import com.learnloop.service.ClassroomViews.MaterialAccess;
import com.learnloop.support.MySqlTestDatabase;
import com.learnloop.util.MaterialFileStorage;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassroomServiceIntegrationTest {

    private static final ClientContext CLIENT = new ClientContext("127.0.0.1", "JUnit");

    @TempDir
    Path materialsDirectory;

    private Database database;
    private ClassService classService;
    private ClassroomService classroomService;
    private MaterialFileStorage fileStorage;
    private final Set<Long> classIds = new LinkedHashSet<>();
    private final Set<Long> userIds = new LinkedHashSet<>();
    private long teacherId;
    private long otherTeacherId;
    private long studentId;
    private long otherStudentId;
    private long pendingStudentId;
    private long adminId;
    private long mathSubjectId;

    @BeforeEach
    void setUp() {
        database = MySqlTestDatabase.database();
        classService = new ClassService(database);
        fileStorage = new MaterialFileStorage(materialsDirectory);
        classroomService = new ClassroomService(
                database,
                new com.learnloop.dao.ClassroomDao(),
                new com.learnloop.dao.ClassDao(),
                new com.learnloop.dao.EnrollmentDao(),
                new UserDao(),
                new com.learnloop.dao.AuditLogDao(),
                fileStorage);
        mathSubjectId = MySqlTestDatabase.queryLong(
                database, "SELECT id FROM subjects WHERE code = ?", "MATH");
        teacherId = createUser(UserRole.TEACHER, ApprovalStatus.APPROVED);
        otherTeacherId = createUser(UserRole.TEACHER, ApprovalStatus.APPROVED);
        studentId = createUser(UserRole.STUDENT, ApprovalStatus.APPROVED);
        otherStudentId = createUser(UserRole.STUDENT, ApprovalStatus.APPROVED);
        pendingStudentId = createUser(UserRole.STUDENT, ApprovalStatus.PENDING);
        adminId = createUser(UserRole.ADMIN, ApprovalStatus.APPROVED);
        assign(teacherId);
        assign(otherTeacherId);
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
    void teacherSeesAllMaterialsWhileStudentSeesOnlyPublishedOnes() {
        long classId = createActiveClass(teacherId);
        long publishedId = createLinkMaterial(teacherId, classId, "Published link",
                "https://example.com/reading");
        long draftId = createTextMaterial(teacherId, classId, "Draft reading", "Not ready yet.");
        classroomService.publishMaterial(teacherId, classId, publishedId, CLIENT);
        enrol(classId, studentId);

        Classroom teacherView = classroomService.teacherClassroom(teacherId, classId);
        assertEquals(2, teacherView.getMaterialCount());
        assertEquals(1, teacherView.getPublishedMaterialCount());

        Classroom studentView = classroomService.studentClassroom(studentId, classId);
        assertEquals(1, studentView.getMaterialCount());
        assertEquals(publishedId, studentView.getMaterials().get(0).getId());

        MaterialAccess published = classroomService.openMaterial(studentId, UserRole.STUDENT, publishedId);
        assertEquals("Published link", published.material().getTitle());
        assertThrows(ClassroomAccessException.class,
                () -> classroomService.openMaterial(studentId, UserRole.STUDENT, draftId));
        assertThrows(ClassroomAccessException.class,
                () -> classroomService.openMaterial(otherStudentId, UserRole.STUDENT, publishedId));
    }

    @Test
    void studentsWithoutActiveEnrollmentOrWithPendingAccountsAreDenied() {
        long classId = createActiveClass(teacherId);
        enrol(classId, otherStudentId);

        assertThrows(ClassroomAccessException.class,
                () -> classroomService.studentClassroom(studentId, classId));
        assertThrows(ClassroomAccessException.class,
                () -> classroomService.studentClassroom(pendingStudentId, classId));
        assertThrows(ClassroomAccessException.class,
                () -> classroomService.studentClassroom(teacherId, classId));
    }

    @Test
    void archivedClassesAreHiddenFromStudentsButVisibleToTheirTeacher() {
        long classId = createActiveClass(teacherId);
        enrol(classId, studentId);
        classService.archiveClass(teacherId, classId, CLIENT);

        assertThrows(ClassroomAccessException.class,
                () -> classroomService.studentClassroom(studentId, classId));
        assertEquals("ARCHIVED", classroomService.teacherClassroom(teacherId, classId).getStatus());
    }

    @Test
    void teacherCanPublishUnpublishAndDeleteOwnedMaterials() {
        long classId = createActiveClass(teacherId);
        long materialId = createLinkMaterial(teacherId, classId, "Lifecycle", "https://example.com/life");

        assertEquals(0, classroomService.teacherClassroom(teacherId, classId)
                .getPublishedMaterialCount());
        classroomService.publishMaterial(teacherId, classId, materialId, CLIENT);
        Material published = classroomService.teacherClassroom(teacherId, classId).getMaterials().get(0);
        assertTrue(published.isPublished());
        assertEquals(1L, auditCount("MATERIAL_PUBLISHED", materialId));

        classroomService.unpublishMaterial(teacherId, classId, materialId, CLIENT);
        assertFalse(classroomService.teacherClassroom(teacherId, classId)
                .getMaterials().get(0).isPublished());
        assertEquals(1L, auditCount("MATERIAL_UNPUBLISHED", materialId));

        classroomService.deleteMaterial(teacherId, classId, materialId, CLIENT);
        assertEquals(0, classroomService.teacherClassroom(teacherId, classId).getMaterialCount());
        assertEquals(1L, auditCount("MATERIAL_DELETED", materialId));
    }

    @Test
    void otherTeachersCannotReadOrManageAnotherTeachersClassroom() {
        long classId = createActiveClass(teacherId);
        long materialId = createLinkMaterial(teacherId, classId, "Private", "https://example.com/private");

        assertThrows(ClassroomAccessException.class,
                () -> classroomService.teacherClassroom(otherTeacherId, classId));
        assertThrows(ClassroomAccessException.class,
                () -> classroomService.openMaterial(otherTeacherId, UserRole.TEACHER, materialId));
        assertThrows(ClassroomAccessException.class,
                () -> classroomService.publishMaterial(otherTeacherId, classId, materialId, CLIENT));
        assertThrows(ClassroomAccessException.class,
                () -> classroomService.deleteMaterial(otherTeacherId, classId, materialId, CLIENT));
        assertThrows(ClassroomAccessException.class,
                () -> classroomService.updateMeetingUrl(otherTeacherId, classId,
                        "https://meet.google.com/abc-defg-hij", CLIENT));
    }

    @Test
    void linkMaterialsRequireSafeHttpUrls() {
        long classId = createActiveClass(teacherId);

        assertThrows(ValidationException.class, () -> classroomService.createMaterial(
                teacherId, classId,
                new ClassroomService.MaterialForm("Unsafe", null, "LINK", "javascript:alert(1)", null),
                null, CLIENT));
        assertThrows(ValidationException.class, () -> classroomService.createMaterial(
                teacherId, classId,
                new ClassroomService.MaterialForm("Relative", null, "LINK", "/local/path", null),
                null, CLIENT));
        assertThrows(ValidationException.class, () -> classroomService.createMaterial(
                teacherId, classId,
                new ClassroomService.MaterialForm("Empty", null, "TEXT", null, "   "),
                null, CLIENT));
    }

    @Test
    void meetingLinksAreValidatedAndVisibleOnlyToAuthorizedUsers() {
        long classId = createActiveClass(teacherId);
        enrol(classId, studentId);
        String meetLink = "https://meet.google.com/abc-defg-hij";

        classroomService.updateMeetingUrl(teacherId, classId, meetLink, CLIENT);
        assertEquals(meetLink, classroomService.teacherClassroom(teacherId, classId).getMeetingUrl());
        assertEquals(meetLink,
                classroomService.openMeetingUrl(studentId, UserRole.STUDENT, classId));
        assertEquals(meetLink,
                classroomService.openMeetingUrl(adminId, UserRole.ADMIN, classId));
        assertThrows(ClassroomAccessException.class,
                () -> classroomService.openMeetingUrl(otherStudentId, UserRole.STUDENT, classId));

        assertThrows(ValidationException.class, () -> classroomService.updateMeetingUrl(
                teacherId, classId, "https://example.com/not-meet", CLIENT));
        assertThrows(ValidationException.class, () -> classroomService.updateMeetingUrl(
                teacherId, classId, "http://meet.google.com/abc-defg-hij", CLIENT));

        classroomService.updateMeetingUrl(teacherId, classId, "", CLIENT);
        assertNull(classroomService.teacherClassroom(teacherId, classId).getMeetingUrl());
        assertThrows(ClassroomAccessException.class,
                () -> classroomService.openMeetingUrl(studentId, UserRole.STUDENT, classId));
    }

    @Test
    void administratorsCanInspectDraftMaterialsButStudentsCannot() {
        long classId = createActiveClass(teacherId);
        long materialId = createTextMaterial(teacherId, classId, "Draft notes", "Only staff for now.");
        enrol(classId, studentId);

        Classroom adminView = classroomService.adminClassroom(adminId, classId);
        assertEquals(1, adminView.getMaterialCount());
        assertEquals(materialId,
                classroomService.openMaterial(adminId, UserRole.ADMIN, materialId).material().getId());
        assertThrows(ClassroomAccessException.class,
                () -> classroomService.openMaterial(studentId, UserRole.STUDENT, materialId));
    }

    @Test
    void fileMaterialsAreStoredOutsideTheWebRootAndRemovedWhenReplacedOrDeleted() throws IOException {
        long classId = createActiveClass(teacherId);
        Part part = new TestPart("worksheet.pdf", "application/pdf",
                "file contents".getBytes(StandardCharsets.UTF_8));

        long materialId = classroomService.createMaterial(
                teacherId,
                classId,
                new ClassroomService.MaterialForm("Worksheet", null, "FILE", null, null),
                part,
                CLIENT);

        Material material = classroomService.teacherClassroom(teacherId, classId).getMaterials().get(0);
        assertEquals("worksheet.pdf", material.getFileName());
        assertTrue(fileStorage.exists(material.getStoredName()));
        assertEquals("file contents",
                Files.readString(fileStorage.resolve(material.getStoredName())));
        assertFalse(Files.exists(materialsDirectory.getParent().resolve("worksheet.pdf")));

        Part replacementPart = new TestPart("worksheet-v2.pdf", "application/pdf",
                "updated".getBytes(StandardCharsets.UTF_8));
        classroomService.updateMaterial(
                teacherId,
                classId,
                materialId,
                new ClassroomService.MaterialForm("Worksheet v2", null, "FILE", null, null),
                replacementPart,
                CLIENT);

        Material updated = classroomService.teacherClassroom(teacherId, classId).getMaterials().get(0);
        assertEquals("Worksheet v2", updated.getTitle());
        assertEquals("worksheet-v2.pdf", updated.getFileName());
        assertFalse(fileStorage.exists(material.getStoredName()));
        assertTrue(fileStorage.exists(updated.getStoredName()));

        classroomService.updateMaterial(
                teacherId,
                classId,
                materialId,
                new ClassroomService.MaterialForm("Now reading", null, "TEXT", null, "Read this."),
                null,
                CLIENT);
        Material converted = classroomService.teacherClassroom(teacherId, classId).getMaterials().get(0);
        assertTrue(converted.isText());
        assertNull(converted.getStoredName());
        assertFalse(fileStorage.exists(updated.getStoredName()));

        long secondFileId = classroomService.createMaterial(
                teacherId,
                classId,
                new ClassroomService.MaterialForm("Delete me", null, "FILE", null, null),
                new TestPart("delete-me.pdf", "application/pdf", new byte[]{1, 2, 3}),
                CLIENT);
        String secondStoredName = classroomService.teacherClassroom(teacherId, classId)
                .getMaterials().stream()
                .filter(item -> item.getId() == secondFileId)
                .findFirst()
                .orElseThrow()
                .getStoredName();
        classroomService.deleteMaterial(teacherId, classId, secondFileId, CLIENT);
        assertFalse(fileStorage.exists(secondStoredName));
    }

    @Test
    void archivedClassesCannotAcceptMaterialOrMeetingChanges() {
        long classId = createActiveClass(teacherId);
        classroomService.updateMeetingUrl(teacherId, classId, "https://meet.google.com/abc-defg-hij", CLIENT);
        classService.archiveClass(teacherId, classId, CLIENT);

        assertThrows(ValidationException.class, () -> classroomService.createMaterial(
                teacherId, classId,
                new ClassroomService.MaterialForm("Too late", null, "LINK", "https://example.com", null),
                null, CLIENT));
        assertThrows(ValidationException.class, () -> classroomService.updateMeetingUrl(
                teacherId, classId, "https://meet.google.com/xyz-uvwx-rst", CLIENT));
    }

    private long createLinkMaterial(long ownerId, long classId, String title, String url) {
        return classroomService.createMaterial(
                ownerId,
                classId,
                new ClassroomService.MaterialForm(title, "Material description", "LINK", url, null),
                null,
                CLIENT);
    }

    private long createTextMaterial(long ownerId, long classId, String title, String content) {
        return classroomService.createMaterial(
                ownerId,
                classId,
                new ClassroomService.MaterialForm(title, null, "TEXT", null, content),
                null,
                CLIENT);
    }

    private long auditCount(String action, long materialId) {
        return MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE action = ? AND entity_type = 'MATERIAL' AND entity_id = ?
                """, action, materialId);
    }

    private void enrol(long classId, long studentUserId) {
        MySqlTestDatabase.execute(database, """
                INSERT INTO class_enrollments (class_id, student_user_id, status)
                VALUES (?, ?, 'ACTIVE')
                """, classId, studentUserId);
    }

    private long createActiveClass(long owningTeacherId) {
        LearningClass created = classService.createManagedClass(new TeacherClassCreateRequest(
                owningTeacherId,
                mathSubjectId,
                "Phase 10 class " + UUID.randomUUID(),
                "Classroom integration test class.",
                25), CLIENT);
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
        String email = "phase10-" + UUID.randomUUID() + "@learnloop.test";
        long userId = database.inTransaction(connection -> new UserDao().insert(connection, new NewUser(
                email,
                "integration-test-hash",
                "Phase 10 " + role.name(),
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

    private static final class TestPart implements Part {

        private final String submittedFileName;
        private final String contentType;
        private final byte[] content;

        private TestPart(String submittedFileName, String contentType, byte[] content) {
            this.submittedFileName = submittedFileName;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getSubmittedFileName() {
            return submittedFileName;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public void write(String fileName) {
        }

        @Override
        public void delete() {
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return List.of();
        }

        @Override
        public Collection<String> getHeaderNames() {
            return List.of();
        }
    }
}
