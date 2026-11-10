package com.learnloop.dao;

import com.learnloop.service.DashboardViews.AdminAssignmentRow;
import com.learnloop.service.DashboardViews.AdminClassRow;
import com.learnloop.service.DashboardViews.AdminOverview;
import com.learnloop.service.DashboardViews.AdminSubjectRow;
import com.learnloop.service.DashboardViews.AdminUserRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AdminDashboardDao {

    private static final String USER_COLUMNS = """
            SELECT id, full_name, email, role, approval_status, rejection_reason, created_at
            FROM users
            """;

    private static final String SUBJECT_COLUMNS = """
            SELECT
                s.id,
                s.code,
                s.name,
                s.description,
                s.is_active,
                COUNT(DISTINCT CASE WHEN tsa.is_active = TRUE THEN tsa.teacher_user_id END)
                    AS teacher_count,
                COUNT(DISTINCT c.id) AS class_count
            FROM subjects s
            LEFT JOIN teacher_subject_assignments tsa ON tsa.subject_id = s.id
            LEFT JOIN classes c ON c.subject_id = s.id
            """;

    private static final String ASSIGNMENT_COLUMNS = """
            SELECT
                tsa.id,
                tsa.teacher_user_id,
                u.full_name AS teacher_name,
                u.email AS teacher_email,
                tsa.subject_id,
                s.code AS subject_code,
                s.name AS subject_name,
                tsa.is_active,
                tsa.created_at AS assigned_at
            FROM teacher_subject_assignments tsa
            JOIN users u ON u.id = tsa.teacher_user_id
            JOIN subjects s ON s.id = tsa.subject_id
            """;

    private static final String CLASS_COLUMNS = """
            SELECT
                c.id,
                c.title,
                s.code AS subject_code,
                u.full_name AS teacher_name,
                c.status,
                c.max_students,
                c.created_at,
                (SELECT COUNT(*) FROM class_enrollments e
                 WHERE e.class_id = c.id AND e.status = 'ACTIVE') AS enrolled_count,
                (SELECT COUNT(*) FROM class_join_requests jr
                 WHERE jr.class_id = c.id AND jr.status = 'PENDING') AS pending_request_count
            FROM classes c
            JOIN subjects s ON s.id = c.subject_id
            JOIN users u ON u.id = c.teacher_user_id
            """;

    public AdminOverview loadOverview(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM users) AS user_count,
                    (SELECT COUNT(*) FROM users
                     WHERE role = 'TEACHER' AND approval_status = 'PENDING') AS pending_teacher_count,
                    (SELECT COUNT(*) FROM subjects) AS subject_count,
                    (SELECT COUNT(*) FROM classes) AS class_count,
                    (SELECT COUNT(*) FROM class_join_requests) AS join_request_count,
                    (SELECT COUNT(*) FROM class_join_requests
                     WHERE status = 'PENDING') AS pending_join_request_count
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new SQLException("Admin dashboard counts could not be loaded.");
            }
            return new AdminOverview(
                    resultSet.getLong("user_count"),
                    resultSet.getLong("pending_teacher_count"),
                    resultSet.getLong("subject_count"),
                    resultSet.getLong("class_count"),
                    resultSet.getLong("join_request_count"),
                    resultSet.getLong("pending_join_request_count"),
                    findPendingTeachers(connection, 5));
        }
    }

    public List<AdminUserRow> findPendingTeachers(Connection connection, int limit) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = USER_COLUMNS + """
                WHERE role = 'TEACHER' AND approval_status = 'PENDING'
                ORDER BY created_at ASC, id ASC
                LIMIT ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapUsers(resultSet);
            }
        }
    }

    public List<AdminUserRow> findTeachers(Connection connection) throws SQLException {
        return findTeachers(connection, null, Integer.MAX_VALUE, 0);
    }

    public List<AdminUserRow> findTeachers(Connection connection, String query, int limit, int offset)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Search search = Search.forUsers(query);
        String sql = USER_COLUMNS + search.whereTeacher() + """
                ORDER BY CASE approval_status
                    WHEN 'PENDING' THEN 1
                    WHEN 'APPROVED' THEN 2
                    WHEN 'SUSPENDED' THEN 3
                    ELSE 4
                END, full_name, email
                LIMIT ? OFFSET ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int nextIndex = bindSearch(statement, search);
            statement.setInt(nextIndex, limit);
            statement.setInt(nextIndex + 1, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapUsers(resultSet);
            }
        }
    }

    public long countTeachers(Connection connection, String query) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Search search = Search.forUsers(query);
        return count(connection, "SELECT COUNT(*) FROM users " + search.whereTeacher(), search);
    }

    public List<AdminUserRow> findUsers(Connection connection) throws SQLException {
        return findUsers(connection, null, Integer.MAX_VALUE, 0);
    }

    public List<AdminUserRow> findUsers(Connection connection, String query, int limit, int offset)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Search search = Search.forUsers(query);
        String sql = USER_COLUMNS + search.whereAll() + """
                ORDER BY FIELD(role, 'ADMIN', 'TEACHER', 'STUDENT'), full_name, email
                LIMIT ? OFFSET ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int nextIndex = bindSearch(statement, search);
            statement.setInt(nextIndex, limit);
            statement.setInt(nextIndex + 1, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapUsers(resultSet);
            }
        }
    }

    public long countUsers(Connection connection, String query) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Search search = Search.forUsers(query);
        return count(connection, "SELECT COUNT(*) FROM users " + search.whereAll(), search);
    }

    public List<AdminUserRow> findApprovedTeachers(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = USER_COLUMNS + """
                WHERE role = 'TEACHER' AND approval_status = 'APPROVED'
                ORDER BY full_name, email
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapUsers(resultSet);
        }
    }

    public List<AdminSubjectRow> findSubjects(Connection connection) throws SQLException {
        return findSubjects(connection, null, Integer.MAX_VALUE, 0);
    }

    public List<AdminSubjectRow> findSubjects(Connection connection, String query, int limit, int offset)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Search search = Search.forSubjects(query);
        String sql = SUBJECT_COLUMNS + search.whereSubjects() + """
                GROUP BY s.id, s.code, s.name, s.description, s.is_active
                ORDER BY s.name, s.code
                LIMIT ? OFFSET ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int nextIndex = bindSearch(statement, search);
            statement.setInt(nextIndex, limit);
            statement.setInt(nextIndex + 1, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapSubjects(resultSet);
            }
        }
    }

    public long countSubjects(Connection connection, String query) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Search search = Search.forSubjects(query);
        return count(connection, "SELECT COUNT(*) FROM subjects s " + search.whereSubjects(), search);
    }

    public List<AdminSubjectRow> findActiveSubjects(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = SUBJECT_COLUMNS + """
                WHERE s.is_active = TRUE
                GROUP BY s.id, s.code, s.name, s.description, s.is_active
                ORDER BY s.name, s.code
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapSubjects(resultSet);
        }
    }

    public List<AdminAssignmentRow> findAssignments(Connection connection) throws SQLException {
        return findAssignments(connection, null, Integer.MAX_VALUE, 0);
    }

    public List<AdminAssignmentRow> findAssignments(Connection connection, String query, int limit, int offset)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Search search = Search.forAssignments(query);
        String sql = ASSIGNMENT_COLUMNS + search.whereAssignments() + """
                ORDER BY u.full_name, s.name
                LIMIT ? OFFSET ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int nextIndex = bindSearch(statement, search);
            statement.setInt(nextIndex, limit);
            statement.setInt(nextIndex + 1, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAssignments(resultSet);
            }
        }
    }

    public long countAssignments(Connection connection, String query) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Search search = Search.forAssignments(query);
        return count(connection, """
                SELECT COUNT(*)
                FROM teacher_subject_assignments tsa
                JOIN users u ON u.id = tsa.teacher_user_id
                JOIN subjects s ON s.id = tsa.subject_id
                """ + search.whereAssignments(), search);
    }

    public List<AdminClassRow> findClasses(Connection connection) throws SQLException {
        return findClasses(connection, null, Integer.MAX_VALUE, 0);
    }

    public List<AdminClassRow> findClasses(Connection connection, String query, int limit, int offset)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Search search = Search.forClasses(query);
        String sql = CLASS_COLUMNS + search.whereClasses() + """
                ORDER BY c.created_at DESC, c.id DESC
                LIMIT ? OFFSET ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int nextIndex = bindSearch(statement, search);
            statement.setInt(nextIndex, limit);
            statement.setInt(nextIndex + 1, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapClasses(resultSet);
            }
        }
    }

    public long countClasses(Connection connection, String query) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Search search = Search.forClasses(query);
        return count(connection, """
                SELECT COUNT(*)
                FROM classes c
                JOIN subjects s ON s.id = c.subject_id
                JOIN users u ON u.id = c.teacher_user_id
                """ + search.whereClasses(), search);
    }

    private static long count(Connection connection, String sql, Search search) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSearch(statement, search);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static int bindSearch(PreparedStatement statement, Search search) throws SQLException {
        int index = 1;
        for (String parameter : search.parameters()) {
            statement.setString(index++, parameter);
        }
        return index;
    }

    private static List<AdminUserRow> mapUsers(ResultSet resultSet) throws SQLException {
        List<AdminUserRow> users = new ArrayList<>();
        while (resultSet.next()) {
            users.add(new AdminUserRow(
                    resultSet.getLong("id"),
                    resultSet.getString("full_name"),
                    resultSet.getString("email"),
                    resultSet.getString("role"),
                    resultSet.getString("approval_status"),
                    resultSet.getString("rejection_reason"),
                    JdbcSupport.getDate(resultSet, "created_at")));
        }
        return List.copyOf(users);
    }

    private static List<AdminSubjectRow> mapSubjects(ResultSet resultSet) throws SQLException {
        List<AdminSubjectRow> subjects = new ArrayList<>();
        while (resultSet.next()) {
            subjects.add(new AdminSubjectRow(
                    resultSet.getLong("id"),
                    resultSet.getString("code"),
                    resultSet.getString("name"),
                    resultSet.getString("description"),
                    resultSet.getBoolean("is_active"),
                    resultSet.getLong("teacher_count"),
                    resultSet.getLong("class_count")));
        }
        return List.copyOf(subjects);
    }

    private static List<AdminAssignmentRow> mapAssignments(ResultSet resultSet) throws SQLException {
        List<AdminAssignmentRow> assignments = new ArrayList<>();
        while (resultSet.next()) {
            assignments.add(new AdminAssignmentRow(
                    resultSet.getLong("id"),
                    resultSet.getLong("teacher_user_id"),
                    resultSet.getString("teacher_name"),
                    resultSet.getString("teacher_email"),
                    resultSet.getLong("subject_id"),
                    resultSet.getString("subject_code"),
                    resultSet.getString("subject_name"),
                    resultSet.getBoolean("is_active"),
                    JdbcSupport.getDate(resultSet, "assigned_at")));
        }
        return List.copyOf(assignments);
    }

    private static List<AdminClassRow> mapClasses(ResultSet resultSet) throws SQLException {
        List<AdminClassRow> classes = new ArrayList<>();
        while (resultSet.next()) {
            classes.add(new AdminClassRow(
                    resultSet.getLong("id"),
                    resultSet.getString("title"),
                    resultSet.getString("subject_code"),
                    resultSet.getString("teacher_name"),
                    resultSet.getString("status"),
                    JdbcSupport.getNullableInteger(resultSet, "max_students"),
                    resultSet.getLong("enrolled_count"),
                    resultSet.getLong("pending_request_count"),
                    JdbcSupport.getDate(resultSet, "created_at")));
        }
        return List.copyOf(classes);
    }

    private static String likePattern(String query) {
        String escaped = query
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private record Search(String where, List<String> parameters) {

        static Search forUsers(String query) {
            if (query == null) {
                return new Search("", List.of());
            }
            String pattern = likePattern(query);
            return new Search("WHERE (full_name LIKE ? OR email LIKE ?)", List.of(pattern, pattern));
        }

        static Search forSubjects(String query) {
            if (query == null) {
                return new Search("", List.of());
            }
            String pattern = likePattern(query);
            return new Search("WHERE (s.code LIKE ? OR s.name LIKE ? OR s.description LIKE ?)",
                    List.of(pattern, pattern, pattern));
        }

        static Search forAssignments(String query) {
            if (query == null) {
                return new Search("", List.of());
            }
            String pattern = likePattern(query);
            return new Search("""
                    WHERE (u.full_name LIKE ? OR u.email LIKE ? OR s.code LIKE ? OR s.name LIKE ?)
                    """, List.of(pattern, pattern, pattern, pattern));
        }

        static Search forClasses(String query) {
            if (query == null) {
                return new Search("", List.of());
            }
            String pattern = likePattern(query);
            return new Search("""
                    WHERE (c.title LIKE ? OR s.code LIKE ? OR s.name LIKE ? OR u.full_name LIKE ?)
                    """, List.of(pattern, pattern, pattern, pattern));
        }

        String whereTeacher() {
            return where.isEmpty() ? "WHERE role = 'TEACHER'" : where + " AND role = 'TEACHER'";
        }

        String whereAll() {
            return where;
        }

        String whereSubjects() {
            return where;
        }

        String whereAssignments() {
            return where;
        }

        String whereClasses() {
            return where;
        }
    }
}
