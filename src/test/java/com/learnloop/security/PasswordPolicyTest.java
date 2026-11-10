package com.learnloop.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPasswords() {
        assertNull(PasswordPolicy.validate("LearnLoopSeed#2026"));
        assertNull(PasswordPolicy.validate("Aa1!aaaa"));
    }

    @Test
    void rejectsMissingShortAndOversizedPasswords() {
        assertNotNull(PasswordPolicy.validate(null));
        assertNotNull(PasswordPolicy.validate(""));
        assertNotNull(PasswordPolicy.validate("Aa1!aaa"));
        assertNotNull(PasswordPolicy.validate("Aa1!" + "a".repeat(69)));
    }

    @Test
    void requiresEachCharacterCategory() {
        assertNotNull(PasswordPolicy.validate("aa1!aaaa"));
        assertNotNull(PasswordPolicy.validate("AA1!AAAA"));
        assertNotNull(PasswordPolicy.validate("Aaa!aaaa"));
        assertNotNull(PasswordPolicy.validate("Aa11aaaa"));
    }
}
