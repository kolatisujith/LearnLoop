package com.learnloop.service;

import com.learnloop.model.ApprovalStatus;

public final class AccountAccessPolicy {

    private AccountAccessPolicy() {
    }

    public static LoginStatus evaluate(ApprovalStatus approvalStatus) {
        if (approvalStatus == null) {
            return LoginStatus.ACCOUNT_SUSPENDED;
        }
        return switch (approvalStatus) {
            case APPROVED -> LoginStatus.SUCCESS;
            case PENDING -> LoginStatus.ACCOUNT_PENDING;
            case REJECTED -> LoginStatus.ACCOUNT_REJECTED;
            case SUSPENDED -> LoginStatus.ACCOUNT_SUSPENDED;
        };
    }

    public static boolean isActive(ApprovalStatus approvalStatus) {
        return approvalStatus == ApprovalStatus.APPROVED;
    }
}
