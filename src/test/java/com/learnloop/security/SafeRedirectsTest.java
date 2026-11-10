package com.learnloop.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SafeRedirectsTest {

    @Test
    void acceptsLocalApplicationPaths() {
        assertEquals("/student/dashboard", SafeRedirects.safeLocalPath("/student/dashboard"));
        assertEquals("/teacher/dashboard?tab=classes",
                SafeRedirects.safeLocalPath("/teacher/dashboard?tab=classes"));
    }

    @Test
    void rejectsExternalAndSensitiveTargets() {
        assertNull(SafeRedirects.safeLocalPath("https://example.com"));
        assertNull(SafeRedirects.safeLocalPath("//example.com"));
        assertNull(SafeRedirects.safeLocalPath("\\\\example.com"));
        assertNull(SafeRedirects.safeLocalPath("/WEB-INF/views/admin/dashboard.jsp"));
        assertNull(SafeRedirects.safeLocalPath("/login"));
        assertNull(SafeRedirects.safeLocalPath("/register"));
        assertNull(SafeRedirects.safeLocalPath("/logout"));
        assertNull(SafeRedirects.safeLocalPath("/safe\r\nLocation: https://example.com"));
    }
}
