package com.learnloop.model;

public record UserCredentials(
        long id,
        String email,
        String fullName,
        UserRole role,
        ApprovalStatus approvalStatus,
        String passwordHash
) {

    public UserAccount toAccount() {
        return new UserAccount(id, email, fullName, role, approvalStatus);
    }

    @Override
    public String toString() {
        return "UserCredentials[id=" + id
                + ", email=" + email
                + ", fullName=" + fullName
                + ", role=" + role
                + ", approvalStatus=" + approvalStatus
                + ", passwordHash=***]";
    }
}
