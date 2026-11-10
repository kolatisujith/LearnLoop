package com.learnloop.service;

import com.learnloop.security.PasswordPolicy;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class RegistrationValidator {

    public static final String EMAIL = "email";
    public static final String FULL_NAME = "fullName";
    public static final String ROLE = "role";
    public static final String PASSWORD = "password";
    public static final String CONFIRM_PASSWORD = "confirmPassword";

    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_FULL_NAME_LENGTH = 120;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+"
                    + "(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9]"
                    + "(?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");

    private RegistrationValidator() {
    }

    public static Map<String, String> validate(RegistrationRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (request == null) {
            errors.put(FULL_NAME, "Registration details are required.");
            return errors;
        }

        String fullName = normalizeFullName(request.fullName());
        if (fullName == null) {
            errors.put(FULL_NAME, "Full name is required.");
        } else if (fullName.length() > MAX_FULL_NAME_LENGTH) {
            errors.put(FULL_NAME, "Full name must be " + MAX_FULL_NAME_LENGTH + " characters or fewer.");
        }

        String email = normalizeEmail(request.email());
        if (email == null) {
            errors.put(EMAIL, "Email address is required.");
        } else if (email.length() > MAX_EMAIL_LENGTH) {
            errors.put(EMAIL, "Email address must be " + MAX_EMAIL_LENGTH + " characters or fewer.");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.put(EMAIL, "Enter a valid email address.");
        }

        validateRole(request.role(), errors);

        String passwordError = PasswordPolicy.validate(request.password());
        if (passwordError != null) {
            errors.put(PASSWORD, passwordError);
        }
        if (request.confirmPassword() == null || request.confirmPassword().isEmpty()) {
            errors.put(CONFIRM_PASSWORD, "Confirm your password.");
        } else if (passwordError == null && !request.password().equals(request.confirmPassword())) {
            errors.put(CONFIRM_PASSWORD, "Passwords do not match.");
        }

        return errors;
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    public static String normalizeFullName(String fullName) {
        if (fullName == null) {
            return null;
        }
        String normalized = fullName.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public static String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static void validateRole(String role, Map<String, String> errors) {
        String normalized = normalizeRole(role);
        if (normalized == null) {
            errors.put(ROLE, "Choose a student or teacher account.");
            return;
        }
        if ("ADMIN".equals(normalized)) {
            errors.put(ROLE, "Administrator accounts cannot be created through public registration.");
            return;
        }
        if (!"STUDENT".equals(normalized) && !"TEACHER".equals(normalized)) {
            errors.put(ROLE, "Choose a student or teacher account.");
        }
    }
}
