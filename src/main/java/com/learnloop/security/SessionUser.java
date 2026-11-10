package com.learnloop.security;

import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.UserAccount;
import com.learnloop.model.UserRole;

import java.io.Serializable;

public record SessionUser(
        long id,
        String email,
        String fullName,
        UserRole role,
        ApprovalStatus approvalStatus
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public static SessionUser from(UserAccount account) {
        return new SessionUser(
                account.id(),
                account.email(),
                account.fullName(),
                account.role(),
                account.approvalStatus());
    }

    public long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public boolean isApproved() {
        return approvalStatus == ApprovalStatus.APPROVED;
    }
}
