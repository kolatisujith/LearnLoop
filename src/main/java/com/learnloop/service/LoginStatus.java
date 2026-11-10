package com.learnloop.service;

public enum LoginStatus {
    SUCCESS("Signed in successfully."),
    INVALID_CREDENTIALS("The email or password is incorrect."),
    ACCOUNT_PENDING("Your teacher account is waiting for administrator approval."),
    ACCOUNT_REJECTED("This account was not approved. Contact an administrator for help."),
    ACCOUNT_SUSPENDED("This account is suspended. Contact an administrator for help."),
    RATE_LIMITED("Too many sign-in attempts. Wait a few minutes before trying again.");

    private final String message;

    LoginStatus(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
