package com.learnloop.security;

import com.learnloop.model.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProtectedRoutesTest {

    @Test
    void mapsServletAndViewPrefixesToRequiredRoles() {
        assertEquals(UserRole.ADMIN, ProtectedRoutes.requiredRole("/admin/dashboard"));
        assertEquals(UserRole.ADMIN, ProtectedRoutes.requiredRole("/admin/teachers"));
        assertEquals(UserRole.ADMIN, ProtectedRoutes.requiredRole("/admin/subjects"));
        assertEquals(UserRole.ADMIN, ProtectedRoutes.requiredRole("/admin/assignments"));
        assertEquals(UserRole.ADMIN, ProtectedRoutes.requiredRole("/admin/users"));
        assertEquals(UserRole.ADMIN, ProtectedRoutes.requiredRole("/admin/classes"));
        assertEquals(UserRole.TEACHER, ProtectedRoutes.requiredRole("/teacher/dashboard"));
        assertEquals(UserRole.TEACHER, ProtectedRoutes.requiredRole("/teacher/classes"));
        assertEquals(UserRole.TEACHER, ProtectedRoutes.requiredRole("/teacher/requests"));
        assertEquals(UserRole.TEACHER, ProtectedRoutes.requiredRole("/teacher/materials"));
        assertEquals(UserRole.TEACHER, ProtectedRoutes.requiredRole("/teacher/assessments"));
        assertEquals(UserRole.TEACHER, ProtectedRoutes.requiredRole("/teacher/progress"));
        assertEquals(UserRole.STUDENT, ProtectedRoutes.requiredRole("/student/dashboard"));
        assertEquals(UserRole.STUDENT, ProtectedRoutes.requiredRole("/student/classes"));
        assertEquals(UserRole.STUDENT, ProtectedRoutes.requiredRole("/student/requests"));
        assertEquals(UserRole.STUDENT, ProtectedRoutes.requiredRole("/student/discover"));
        assertEquals(UserRole.STUDENT, ProtectedRoutes.requiredRole("/student/learning"));
        assertEquals(UserRole.STUDENT, ProtectedRoutes.requiredRole("/student/assessments"));
        assertEquals(UserRole.ADMIN, ProtectedRoutes.requiredRole("/WEB-INF/views/admin/dashboard.jsp"));
        assertEquals(UserRole.TEACHER, ProtectedRoutes.requiredRole("/WEB-INF/views/teacher/dashboard.jsp"));
        assertEquals(UserRole.STUDENT, ProtectedRoutes.requiredRole("/WEB-INF/views/student/dashboard.jsp"));
    }

    @Test
    void doesNotProtectPublicRoutes() {
        assertNull(ProtectedRoutes.requiredRole("/"));
        assertNull(ProtectedRoutes.requiredRole("/login"));
        assertNull(ProtectedRoutes.requiredRole("/admin"));
        assertNull(ProtectedRoutes.requiredRole("/administrator"));
    }

    @Test
    void providesOneDashboardPerRole() {
        assertEquals("/admin/dashboard", ProtectedRoutes.dashboardFor(UserRole.ADMIN));
        assertEquals("/teacher/dashboard", ProtectedRoutes.dashboardFor(UserRole.TEACHER));
        assertEquals("/student/dashboard", ProtectedRoutes.dashboardFor(UserRole.STUDENT));
    }
}
