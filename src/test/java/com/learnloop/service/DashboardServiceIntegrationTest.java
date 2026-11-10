package com.learnloop.service;

import com.learnloop.dao.Database;
import com.learnloop.service.DashboardViews.AdminOverview;
import com.learnloop.service.DashboardViews.StudentOverview;
import com.learnloop.service.DashboardViews.TeacherOverview;
import com.learnloop.support.MySqlTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardServiceIntegrationTest {

    private Database database;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        database = MySqlTestDatabase.database();
        service = new DashboardService(database);
    }

    @Test
    void adminOverviewMatchesDatabaseTotals() {
        AdminOverview overview = service.adminOverview();
        long pendingTeachers = MySqlTestDatabase.queryLong(database,
                "SELECT COUNT(*) FROM users WHERE role = 'TEACHER' AND approval_status = 'PENDING'");

        assertEquals(MySqlTestDatabase.queryLong(database, "SELECT COUNT(*) FROM users"),
                overview.getUserCount());
        assertEquals(pendingTeachers, overview.getPendingTeacherCount());
        assertEquals(MySqlTestDatabase.queryLong(database, "SELECT COUNT(*) FROM subjects"),
                overview.getSubjectCount());
        assertEquals(MySqlTestDatabase.queryLong(database, "SELECT COUNT(*) FROM classes"),
                overview.getClassCount());
        assertEquals(MySqlTestDatabase.queryLong(database, "SELECT COUNT(*) FROM class_join_requests"),
                overview.getJoinRequestCount());
        assertEquals(MySqlTestDatabase.queryLong(database,
                        "SELECT COUNT(*) FROM class_join_requests WHERE status = 'PENDING'"),
                overview.getPendingJoinRequestCount());
        assertEquals(Math.min(5, pendingTeachers), overview.getPendingTeachers().size());
        assertTrue(overview.getPendingTeachers().stream()
                .allMatch(teacher -> "TEACHER".equals(teacher.getRole())
                        && "PENDING".equals(teacher.getApprovalStatus())));
    }

    @Test
    void adminDirectoryViewsLoadRealPlatformRecords() {
        assertFalse(service.adminTeachers().isEmpty());
        assertTrue(service.adminTeachers().stream()
                .allMatch(teacher -> "TEACHER".equals(teacher.getRole())));

        Set<String> roles = service.adminUsers().stream()
                .map(DashboardViews.AdminUserRow::getRole)
                .collect(Collectors.toSet());
        assertTrue(roles.contains("TEACHER"));
        assertTrue(roles.contains("STUDENT"));

        assertFalse(service.adminSubjects().isEmpty());
        assertTrue(service.adminSubjects().stream()
                .allMatch(subject -> subject.getCode() != null && !subject.getCode().isBlank()));

        assertFalse(service.adminAssignments().isEmpty());
        assertTrue(service.adminAssignments().stream()
                .allMatch(assignment -> assignment.getTeacherEmail() != null
                        && assignment.getSubjectCode() != null));

        assertFalse(service.adminClasses().isEmpty());
        assertTrue(service.adminClasses().stream()
                .allMatch(learningClass -> learningClass.getTitle() != null
                        && learningClass.getEnrolledCount() >= 0
                        && learningClass.getPendingRequestCount() >= 0));
    }

    @Test
    void teacherDashboardIsScopedToAuthenticatedTeacher() {
        long teacherId = userId("teacher@learnloop.local");
        TeacherOverview overview = service.teacherOverview(teacherId);
        Set<Long> ownedClassIds = queryIds(
                "SELECT id FROM classes WHERE teacher_user_id = ?", teacherId);

        assertEquals("APPROVED", overview.getApprovalStatus());
        assertEquals(ownedClassIds, overview.getClasses().stream()
                .map(DashboardViews.TeacherClassRow::getId)
                .collect(Collectors.toSet()));
        assertFalse(overview.getSubjects().isEmpty());
        assertEquals(overview.getClasses().stream()
                        .mapToLong(DashboardViews.TeacherClassRow::getEnrolledCount)
                        .sum(),
                overview.getTotalEnrolledStudents());
        assertEquals(overview.getClasses().size(), overview.getClassCount());
        assertEquals(overview.getSubjects().size(), overview.getSubjectCount());
        assertEquals(overview.getPendingRequests().size(), overview.getPendingRequestCount());
        assertTrue(overview.getPendingRequests().stream()
                .allMatch(request -> ownedClassIds.contains(request.getClassId()))
                && overview.getPendingRequests().stream()
                .allMatch(request -> "PENDING".equals(statusOfRequest(request.getId()))));
    }

    @Test
    void teacherListingViewsAreScopedToOwnedClasses() {
        long teacherId = userId("teacher@learnloop.local");
        Set<Long> ownedClassIds = queryIds(
                "SELECT id FROM classes WHERE teacher_user_id = ?", teacherId);
        Set<Long> requestClassIds = service.teacherRequests(teacherId).stream()
                .map(DashboardViews.TeacherJoinRequestRow::getClassId)
                .collect(Collectors.toSet());

        assertTrue(ownedClassIds.containsAll(requestClassIds));
        assertEquals(ownedClassIds, service.teacherProgress(teacherId).stream()
                .map(DashboardViews.TeacherProgressRow::getClassId)
                .collect(Collectors.toSet()));
        assertFalse(service.teacherMaterials(teacherId).isEmpty());
        assertFalse(service.teacherAssessments(teacherId).isEmpty());
        assertTrue(service.teacherMaterials(teacherId).stream()
                .allMatch(material -> ownedClassIds.contains(
                        classIdByTitle(material.getClassTitle()))));
        assertTrue(service.teacherAssessments(teacherId).stream()
                .allMatch(assessment -> ownedClassIds.contains(
                        classIdByTitle(assessment.getClassTitle()))));
    }

    @Test
    void studentDashboardIsScopedToAuthenticatedStudent() {
        long aliceId = userId("alice.student@learnloop.local");
        long bobId = userId("bob.student@learnloop.local");
        StudentOverview alice = service.studentOverview(aliceId);
        StudentOverview bob = service.studentOverview(bobId);
        Set<Long> aliceClassIds = queryIds(
                "SELECT class_id FROM class_enrollments WHERE student_user_id = ? AND status = 'ACTIVE'",
                aliceId);
        Set<Long> bobClassIds = queryIds(
                "SELECT class_id FROM class_enrollments WHERE student_user_id = ? AND status = 'ACTIVE'",
                bobId);

        assertEquals(aliceClassIds, alice.getClasses().stream()
                .map(DashboardViews.StudentClassRow::getId)
                .collect(Collectors.toSet()));
        assertEquals(bobClassIds, bob.getClasses().stream()
                .map(DashboardViews.StudentClassRow::getId)
                .collect(Collectors.toSet()));
        assertTrue(java.util.Collections.disjoint(aliceClassIds, bobClassIds));
        assertEquals(aliceClassIds.size(), alice.getEnrolledClassCount());
        assertEquals(alice.getPendingRequests().size(), alice.getPendingRequestCount());
        assertEquals(alice.getAvailableAssessments().size(), alice.getAvailableAssessmentCount());
        assertEquals(alice.getRecentResults().size(), alice.getRecentResultCount());
        assertTrue(alice.getRecentResults().stream()
                .allMatch(result -> queryIds(
                        "SELECT id FROM assessment_attempts WHERE student_user_id = ?",
                        aliceId).contains(result.getAttemptId())));
    }

    @Test
    void studentDiscoveryMaterialsAndAssessmentsRespectEnrollmentAndPublication() {
        long aliceId = userId("alice.student@learnloop.local");
        long bobId = userId("bob.student@learnloop.local");
        StudentOverview alice = service.studentOverview(aliceId);

        Set<Long> aliceClassIds = alice.getClasses().stream()
                .map(DashboardViews.StudentClassRow::getId)
                .collect(Collectors.toSet());
        Set<Long> discoverableIds = service.discoverClasses(aliceId).stream()
                .map(DashboardViews.DiscoveryClassRow::getId)
                .collect(Collectors.toSet());
        assertTrue(java.util.Collections.disjoint(aliceClassIds, discoverableIds));
        assertTrue(service.studentMaterials(aliceId).stream()
                .noneMatch(material -> "Starter repository".equals(material.getTitle())));
        assertTrue(service.studentAssessments(aliceId).stream()
                .noneMatch(assessment -> "Programming Basics Checkpoint".equals(assessment.getTitle())));
        assertTrue(service.discoverClasses(bobId).stream()
                .anyMatch(learningClass -> "PENDING".equals(learningClass.getRequestStatus())));
        assertTrue(service.studentRequests(bobId).stream()
                .allMatch(request -> "PENDING".equals(request.getStatus())));
        assertFalse(service.discoverClasses(bobId).stream()
                .anyMatch(learningClass -> "Intro to Programming".equals(learningClass.getTitle())));
    }

    private long userId(String email) {
        return MySqlTestDatabase.queryLong(database, "SELECT id FROM users WHERE email = ?", email);
    }

    private String statusOfRequest(long requestId) {
        return MySqlTestDatabase.queryString(database,
                "SELECT status FROM class_join_requests WHERE id = ?", requestId);
    }

    private long classIdByTitle(String title) {
        return MySqlTestDatabase.queryLong(database,
                "SELECT id FROM classes WHERE title = ?", title);
    }

    private Set<Long> queryIds(String sql, Object... parameters) {
        return database.withConnection(connection -> {
            Set<Long> values = new LinkedHashSet<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int index = 0; index < parameters.length; index++) {
                    statement.setObject(index + 1, parameters[index]);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        values.add(resultSet.getLong(1));
                    }
                }
            }
            return values;
        });
    }
}
