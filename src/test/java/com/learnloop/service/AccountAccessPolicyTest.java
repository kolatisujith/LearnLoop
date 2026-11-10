package com.learnloop.service;

import com.learnloop.model.ApprovalStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountAccessPolicyTest {

    @Test
    void allowsOnlyApprovedAccounts() {
        assertEquals(LoginStatus.SUCCESS, AccountAccessPolicy.evaluate(ApprovalStatus.APPROVED));
        assertTrue(AccountAccessPolicy.isActive(ApprovalStatus.APPROVED));

        assertEquals(LoginStatus.ACCOUNT_PENDING, AccountAccessPolicy.evaluate(ApprovalStatus.PENDING));
        assertEquals(LoginStatus.ACCOUNT_REJECTED, AccountAccessPolicy.evaluate(ApprovalStatus.REJECTED));
        assertEquals(LoginStatus.ACCOUNT_SUSPENDED, AccountAccessPolicy.evaluate(ApprovalStatus.SUSPENDED));
        assertFalse(AccountAccessPolicy.isActive(ApprovalStatus.PENDING));
        assertFalse(AccountAccessPolicy.isActive(ApprovalStatus.REJECTED));
        assertFalse(AccountAccessPolicy.isActive(ApprovalStatus.SUSPENDED));
    }

    @Test
    void treatsMissingStatusAsSuspended() {
        assertEquals(LoginStatus.ACCOUNT_SUSPENDED, AccountAccessPolicy.evaluate(null));
    }
}
