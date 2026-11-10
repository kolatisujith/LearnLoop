package com.learnloop.security;

import com.learnloop.model.UserRole;

import java.util.Locale;

public final class ProtectedRoutes {

    public static final String ADMIN_PREFIX = "/admin/";
    public static final String TEACHER_PREFIX = "/teacher/";
    public static final String STUDENT_PREFIX = "/student/";
    public static final String ADMIN_VIEW_PREFIX = "/web-inf/views/admin/";
    public static final String TEACHER_VIEW_PREFIX = "/web-inf/views/teacher/";
    public static final String STUDENT_VIEW_PREFIX = "/web-inf/views/student/";

    private ProtectedRoutes() {
    }

    public static UserRole requiredRole(String requestPath) {
        String path = normalize(requestPath);
        if (path.startsWith(ADMIN_PREFIX) || path.startsWith(ADMIN_VIEW_PREFIX)) {
            return UserRole.ADMIN;
        }
        if (path.startsWith(TEACHER_PREFIX) || path.startsWith(TEACHER_VIEW_PREFIX)) {
            return UserRole.TEACHER;
        }
        if (path.startsWith(STUDENT_PREFIX) || path.startsWith(STUDENT_VIEW_PREFIX)) {
            return UserRole.STUDENT;
        }
        return null;
    }

    public static String dashboardFor(UserRole role) {
        return switch (role) {
            case ADMIN -> "/admin/dashboard";
            case TEACHER -> "/teacher/dashboard";
            case STUDENT -> "/student/dashboard";
        };
    }

    private static String normalize(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return "";
        }
        String path = requestPath.trim();
        int semicolon = path.indexOf(';');
        if (semicolon >= 0) {
            path = path.substring(0, semicolon);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path.toLowerCase(Locale.ROOT);
    }
}
