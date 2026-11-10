package com.learnloop.security;

import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.UserRole;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionUserTest {

    @Test
    void exposesJavaBeanPropertiesForJspEl() throws Exception {
        Set<String> properties = Arrays.stream(
                        Introspector.getBeanInfo(SessionUser.class).getPropertyDescriptors())
                .map(descriptor -> descriptor.getName())
                .collect(Collectors.toSet());

        assertTrue(properties.containsAll(Set.of("id", "email", "fullName", "role", "approvalStatus")));
    }

    @Test
    void reportsApprovalState() {
        SessionUser approved = new SessionUser(
                1L, "student@example.com", "Student", UserRole.STUDENT, ApprovalStatus.APPROVED);
        SessionUser pending = new SessionUser(
                2L, "teacher@example.com", "Teacher", UserRole.TEACHER, ApprovalStatus.PENDING);

        assertTrue(approved.isApproved());
        assertEquals(UserRole.STUDENT, approved.getRole());
        assertEquals("Student", approved.getFullName());
        assertEquals("student@example.com", approved.getEmail());
        assertEquals(1L, approved.getId());
        assertEquals(ApprovalStatus.APPROVED, approved.getApprovalStatus());
        assertFalse(pending.isApproved());
    }
}
