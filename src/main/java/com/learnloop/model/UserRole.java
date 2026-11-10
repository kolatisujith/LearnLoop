package com.learnloop.model;

public enum UserRole {
    ADMIN,
    TEACHER,
    STUDENT;

    public static UserRole fromDatabase(String value) {
        if (value == null) {
            throw new IllegalArgumentException("User role is required.");
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported user role: " + value, exception);
        }
    }
}
