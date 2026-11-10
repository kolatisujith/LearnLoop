package com.learnloop.service;

public record RegistrationRequest(
        String email,
        String fullName,
        String role,
        String password,
        String confirmPassword
) {

    @Override
    public String toString() {
        return "RegistrationRequest[email=" + email
                + ", fullName=" + fullName
                + ", role=" + role
                + ", password=***"
                + ", confirmPassword=***]";
    }
}
