package com.learnloop.security;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_UTF8_BYTES = 72;

    private PasswordPolicy() {
    }

    public static String validate(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required.";
        }
        if (password.length() < MIN_LENGTH) {
            return "Password must be at least " + MIN_LENGTH + " characters long.";
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) {
            return "Password must be " + MAX_UTF8_BYTES + " bytes or fewer.";
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            return "Password must include an uppercase letter.";
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            return "Password must include a lowercase letter.";
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            return "Password must include a number.";
        }
        if (password.chars().allMatch(Character::isLetterOrDigit)) {
            return "Password must include a symbol.";
        }
        return null;
    }
}
