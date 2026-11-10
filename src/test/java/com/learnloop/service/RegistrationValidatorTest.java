package com.learnloop.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationValidatorTest {

    private static final String STRONG_PASSWORD = "LearnLoop#Test9";

    @Test
    void acceptsValidStudentAndTeacherRegistrations() {
        assertTrue(RegistrationValidator.validate(request(
                "Student@Example.com ", " Student Name ", "STUDENT")).isEmpty());
        assertTrue(RegistrationValidator.validate(request(
                "teacher@example.com", "Teacher Name", "teacher")).isEmpty());
    }

    @Test
    void requiresNamesEmailsRolesAndConfirmation() {
        Map<String, String> errors = RegistrationValidator.validate(
                new RegistrationRequest(" ", " ", null, "", ""));

        assertTrue(errors.containsKey(RegistrationValidator.FULL_NAME));
        assertTrue(errors.containsKey(RegistrationValidator.EMAIL));
        assertTrue(errors.containsKey(RegistrationValidator.ROLE));
        assertTrue(errors.containsKey(RegistrationValidator.PASSWORD));
        assertTrue(errors.containsKey(RegistrationValidator.CONFIRM_PASSWORD));
    }

    @Test
    void rejectsMalformedEmailAddresses() {
        assertTrue(hasError("not-an-email", RegistrationValidator.EMAIL));
        assertTrue(hasError("student@example", RegistrationValidator.EMAIL));
        assertTrue(hasError("student@.example.com", RegistrationValidator.EMAIL));
        assertTrue(hasError("student..name@example.com", RegistrationValidator.EMAIL));
    }

    @Test
    void rejectsWeakAndMismatchedPasswords() {
        assertTrue(RegistrationValidator.validate(new RegistrationRequest(
                "student@example.com", "Student Name", "STUDENT", "weak", "weak"))
                .containsKey(RegistrationValidator.PASSWORD));

        assertTrue(RegistrationValidator.validate(new RegistrationRequest(
                "student@example.com", "Student Name", "STUDENT", STRONG_PASSWORD, "Different#9"))
                .containsKey(RegistrationValidator.CONFIRM_PASSWORD));
    }

    @Test
    void preventsPublicAdministratorRegistration() {
        Map<String, String> errors = RegistrationValidator.validate(request(
                "admin@example.com", "Admin Name", "ADMIN"));

        assertEquals(
                "Administrator accounts cannot be created through public registration.",
                errors.get(RegistrationValidator.ROLE));
    }

    @Test
    void rejectsUnknownRoles() {
        assertTrue(hasError("student@example.com", RegistrationValidator.ROLE, "OWNER"));
    }

    @Test
    void normalizesEmailFullNameAndRole() {
        assertEquals("student@example.com", RegistrationValidator.normalizeEmail(" Student@Example.com "));
        assertEquals("Student Name", RegistrationValidator.normalizeFullName("  Student Name  "));
        assertEquals("TEACHER", RegistrationValidator.normalizeRole(" teacher "));
        assertFalse(RegistrationValidator.validate(request(
                "student@example.com", "Student Name", "STUDENT")).containsKey(RegistrationValidator.EMAIL));
    }

    private static boolean hasError(String email, String field) {
        return hasError(email, field, "STUDENT");
    }

    private static boolean hasError(String email, String field, String role) {
        return RegistrationValidator.validate(request(email, "Student Name", role)).containsKey(field);
    }

    private static RegistrationRequest request(String email, String fullName, String role) {
        return new RegistrationRequest(email, fullName, role, STRONG_PASSWORD, STRONG_PASSWORD);
    }
}
