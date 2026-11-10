package com.learnloop.security;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class PasswordHasher {

    public static final int DEFAULT_LOG_ROUNDS = 12;
    private static final int BCRYPT_MAX_BYTES = 72;
    private static final String DUMMY_HASH =
            "$2a$12$d2vA3EPkCqB87Mou7DrG0uru1MdmTjkMacoSW.Wm6WUlEZ9V95Gpu";

    private final int logRounds;

    public PasswordHasher() {
        this(DEFAULT_LOG_ROUNDS);
    }

    public PasswordHasher(int logRounds) {
        if (logRounds < 4 || logRounds > 31) {
            throw new IllegalArgumentException("BCrypt log rounds must be between 4 and 31.");
        }
        this.logRounds = logRounds;
    }

    public String hash(String rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new IllegalArgumentException("Password is too long to hash safely.");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(logRounds));
    }

    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean matchesDummy(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        try {
            BCrypt.checkpw(rawPassword, DUMMY_HASH);
        } catch (IllegalArgumentException ignored) {
        }
        return false;
    }
}
