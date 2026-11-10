package com.learnloop.model;

import java.time.Instant;

public record NewUser(
        String email,
        String passwordHash,
        String fullName,
        UserRole role,
        ApprovalStatus approvalStatus,
        Instant approvedAt,
        Long approvedByUserId
) {

    @Override
    public String toString() {
        return "NewUser[email=" + email
                + ", passwordHash=***"
                + ", fullName=" + fullName
                + ", role=" + role
                + ", approvalStatus=" + approvalStatus
                + ", approvedAt=" + approvedAt
                + ", approvedByUserId=" + approvedByUserId + "]";
    }
}
