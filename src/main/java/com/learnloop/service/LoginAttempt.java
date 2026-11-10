package com.learnloop.service;

public record LoginAttempt(
        String email,
        String password,
        String ipAddress,
        String userAgent
) {

    @Override
    public String toString() {
        return "LoginAttempt[email=" + email
                + ", password=***"
                + ", ipAddress=" + ipAddress
                + ", userAgent=" + userAgent + "]";
    }
}
