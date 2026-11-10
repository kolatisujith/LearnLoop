package com.learnloop.service;

import com.learnloop.dao.AdminDashboardDao;
import com.learnloop.dao.Database;
import com.learnloop.dao.StudentDashboardDao;
import com.learnloop.dao.TeacherDashboardDao;
import com.learnloop.service.DashboardViews.AdminAssignmentRow;
import com.learnloop.service.DashboardViews.AdminClassRow;
import com.learnloop.service.DashboardViews.AdminOverview;
import com.learnloop.service.DashboardViews.AdminSubjectRow;
import com.learnloop.service.DashboardViews.AdminUserRow;
import com.learnloop.service.DashboardViews.DiscoveryClassRow;
import com.learnloop.service.DashboardViews.StudentAssessmentRow;
import com.learnloop.service.DashboardViews.StudentClassRow;
import com.learnloop.service.DashboardViews.StudentJoinRequestRow;
import com.learnloop.service.DashboardViews.StudentMaterialRow;
import com.learnloop.service.DashboardViews.StudentOverview;
import com.learnloop.service.DashboardViews.StudentResultRow;
import com.learnloop.service.DashboardViews.TeacherAssessmentRow;
import com.learnloop.service.DashboardViews.TeacherClassRow;
import com.learnloop.service.DashboardViews.TeacherJoinRequestRow;
import com.learnloop.service.DashboardViews.TeacherMaterialRow;
import com.learnloop.service.DashboardViews.TeacherOverview;
import com.learnloop.service.DashboardViews.TeacherProgressRow;

import java.util.List;
import java.util.Objects;

public final class DashboardService {

    public static final int ADMIN_PAGE_SIZE = 12;

    private final Database database;
    private final AdminDashboardDao adminDao;
    private final TeacherDashboardDao teacherDao;
    private final StudentDashboardDao studentDao;

    public DashboardService(Database database) {
        this(database, new AdminDashboardDao(), new TeacherDashboardDao(), new StudentDashboardDao());
    }

    public DashboardService(
            Database database,
            AdminDashboardDao adminDao,
            TeacherDashboardDao teacherDao,
            StudentDashboardDao studentDao) {
        this.database = Objects.requireNonNull(database, "database");
        this.adminDao = Objects.requireNonNull(adminDao, "adminDao");
        this.teacherDao = Objects.requireNonNull(teacherDao, "teacherDao");
        this.studentDao = Objects.requireNonNull(studentDao, "studentDao");
    }

    public AdminOverview adminOverview() {
        return database.withConnection(adminDao::loadOverview);
    }

    public List<AdminUserRow> adminTeachers() {
        return database.withConnection(adminDao::findTeachers);
    }

    public List<AdminUserRow> adminUsers() {
        return database.withConnection(adminDao::findUsers);
    }

    public List<AdminSubjectRow> adminSubjects() {
        return database.withConnection(adminDao::findSubjects);
    }

    public List<AdminAssignmentRow> adminAssignments() {
        return database.withConnection(adminDao::findAssignments);
    }

    public List<AdminClassRow> adminClasses() {
        return database.withConnection(adminDao::findClasses);
    }

    public PagedResult<AdminUserRow> adminTeachers(String query, int requestedPage) {
        String normalizedQuery = normalizeQuery(query);
        return database.withConnection(connection -> {
            long total = adminDao.countTeachers(connection, normalizedQuery);
            int page = safePage(requestedPage, total);
            List<AdminUserRow> rows = adminDao.findTeachers(
                    connection,
                    normalizedQuery,
                    ADMIN_PAGE_SIZE,
                    (page - 1) * ADMIN_PAGE_SIZE);
            return new PagedResult<>(rows, normalizedQuery, page, ADMIN_PAGE_SIZE, total);
        });
    }

    public PagedResult<AdminUserRow> adminUsers(String query, int requestedPage) {
        String normalizedQuery = normalizeQuery(query);
        return database.withConnection(connection -> {
            long total = adminDao.countUsers(connection, normalizedQuery);
            int page = safePage(requestedPage, total);
            List<AdminUserRow> rows = adminDao.findUsers(
                    connection,
                    normalizedQuery,
                    ADMIN_PAGE_SIZE,
                    (page - 1) * ADMIN_PAGE_SIZE);
            return new PagedResult<>(rows, normalizedQuery, page, ADMIN_PAGE_SIZE, total);
        });
    }

    public PagedResult<AdminSubjectRow> adminSubjects(String query, int requestedPage) {
        String normalizedQuery = normalizeQuery(query);
        return database.withConnection(connection -> {
            long total = adminDao.countSubjects(connection, normalizedQuery);
            int page = safePage(requestedPage, total);
            List<AdminSubjectRow> rows = adminDao.findSubjects(
                    connection,
                    normalizedQuery,
                    ADMIN_PAGE_SIZE,
                    (page - 1) * ADMIN_PAGE_SIZE);
            return new PagedResult<>(rows, normalizedQuery, page, ADMIN_PAGE_SIZE, total);
        });
    }

    public PagedResult<AdminAssignmentRow> adminAssignments(String query, int requestedPage) {
        String normalizedQuery = normalizeQuery(query);
        return database.withConnection(connection -> {
            long total = adminDao.countAssignments(connection, normalizedQuery);
            int page = safePage(requestedPage, total);
            List<AdminAssignmentRow> rows = adminDao.findAssignments(
                    connection,
                    normalizedQuery,
                    ADMIN_PAGE_SIZE,
                    (page - 1) * ADMIN_PAGE_SIZE);
            return new PagedResult<>(rows, normalizedQuery, page, ADMIN_PAGE_SIZE, total);
        });
    }

    public PagedResult<AdminClassRow> adminClasses(String query, int requestedPage) {
        String normalizedQuery = normalizeQuery(query);
        return database.withConnection(connection -> {
            long total = adminDao.countClasses(connection, normalizedQuery);
            int page = safePage(requestedPage, total);
            List<AdminClassRow> rows = adminDao.findClasses(
                    connection,
                    normalizedQuery,
                    ADMIN_PAGE_SIZE,
                    (page - 1) * ADMIN_PAGE_SIZE);
            return new PagedResult<>(rows, normalizedQuery, page, ADMIN_PAGE_SIZE, total);
        });
    }

    public List<AdminUserRow> adminApprovedTeachers() {
        return database.withConnection(adminDao::findApprovedTeachers);
    }

    public List<AdminSubjectRow> adminActiveSubjects() {
        return database.withConnection(adminDao::findActiveSubjects);
    }

    public TeacherOverview teacherOverview(long teacherUserId) {
        return database.withConnection(connection -> teacherDao.loadOverview(connection, teacherUserId));
    }

    public List<TeacherClassRow> teacherClasses(long teacherUserId) {
        return database.withConnection(connection -> teacherDao.findClasses(connection, teacherUserId));
    }

    public List<TeacherJoinRequestRow> teacherRequests(long teacherUserId) {
        return database.withConnection(connection -> teacherDao.findPendingRequests(connection, teacherUserId));
    }

    public List<TeacherMaterialRow> teacherMaterials(long teacherUserId) {
        return database.withConnection(connection -> teacherDao.findMaterials(connection, teacherUserId));
    }

    public List<TeacherAssessmentRow> teacherAssessments(long teacherUserId) {
        return database.withConnection(connection -> teacherDao.findAssessments(connection, teacherUserId));
    }

    public List<TeacherProgressRow> teacherProgress(long teacherUserId) {
        return database.withConnection(connection -> teacherDao.findProgress(connection, teacherUserId));
    }

    public StudentOverview studentOverview(long studentUserId) {
        return database.withConnection(connection -> studentDao.loadOverview(connection, studentUserId));
    }

    public List<StudentClassRow> studentClasses(long studentUserId) {
        return database.withConnection(connection -> studentDao.findEnrolledClasses(connection, studentUserId));
    }

    public List<StudentJoinRequestRow> studentRequests(long studentUserId) {
        return database.withConnection(connection -> studentDao.findPendingRequests(connection, studentUserId));
    }

    public List<StudentAssessmentRow> studentAssessments(long studentUserId) {
        return database.withConnection(connection -> studentDao.findAvailableAssessments(connection, studentUserId));
    }

    public List<StudentResultRow> studentResults(long studentUserId) {
        return database.withConnection(connection -> studentDao.findRecentResults(connection, studentUserId));
    }

    public List<StudentMaterialRow> studentMaterials(long studentUserId) {
        return database.withConnection(connection -> studentDao.findEnrolledMaterials(connection, studentUserId));
    }

    public List<DiscoveryClassRow> discoverClasses(long studentUserId) {
        return database.withConnection(connection -> studentDao.findDiscoverableClasses(connection, studentUserId));
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int safePage(int requestedPage, long totalItems) {
        if (totalItems == 0) {
            return 1;
        }
        int totalPages = (int) Math.min(
                Integer.MAX_VALUE,
                (totalItems + ADMIN_PAGE_SIZE - 1) / ADMIN_PAGE_SIZE);
        return Math.min(Math.max(1, requestedPage), totalPages);
    }
}
