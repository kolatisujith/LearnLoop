package com.learnloop.model;

public record UserAccount(
        long id,
        String email,
        String fullName,
        UserRole role,
        ApprovalStatus approvalStatus
) {
}
