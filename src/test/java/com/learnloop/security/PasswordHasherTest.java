package com.learnloop.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher(4);

    @Test
    void hashesWithSaltedBcryptAndVerifiesThePassword() {
        String rawPassword = "LearnLoop#Test9";

        String firstHash = hasher.hash(rawPassword);
        String secondHash = hasher.hash(rawPassword);

        assertNotEquals(rawPassword, firstHash);
        assertTrue(firstHash.startsWith("$2a$04$"));
        assertNotEquals(firstHash, secondHash);
        assertTrue(hasher.matches(rawPassword, firstHash));
        assertTrue(hasher.matches(rawPassword, secondHash));
    }

    @Test
    void rejectsWrongOrMissingPasswordHashes() {
        String hash = hasher.hash("LearnLoop#Test9");

        assertFalse(hasher.matches("Wrong#Password1", hash));
        assertFalse(hasher.matches(null, hash));
        assertFalse(hasher.matches("LearnLoop#Test9", null));
        assertFalse(hasher.matches("LearnLoop#Test9", "not-a-bcrypt-hash"));
    }

    @Test
    void dummyVerificationNeverAuthenticates() {
        assertFalse(hasher.matchesDummy("LearnLoop#Test9"));
        assertFalse(hasher.matchesDummy(null));
    }

    @Test
    void refusesPasswordsBeyondBcryptInputLimit() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash("a".repeat(73)));
    }

    @Test
    void rejectsUnsafeBcryptCost() {
        assertThrows(IllegalArgumentException.class, () -> new PasswordHasher(3));
        assertThrows(IllegalArgumentException.class, () -> new PasswordHasher(32));
    }
}
