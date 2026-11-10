package com.learnloop.model;

public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED;

    public static ApprovalStatus fromDatabase(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Approval status is required.");
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported approval status: " + value, exception);
        }
    }
}
