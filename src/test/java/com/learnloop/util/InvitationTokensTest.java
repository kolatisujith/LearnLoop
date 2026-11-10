package com.learnloop.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationTokensTest {

    @Test
    void generatesUrlSafeTokensWithEnoughEntropy() {
        String first = InvitationTokens.generate();
        String second = InvitationTokens.generate();

        assertNotEquals(first, second);
        assertEquals(43, first.length());
        assertTrue(first.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void hashesWithSha256() {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                InvitationTokens.sha256Hex("abc"));
    }

    @Test
    void comparesTokensToHashes() {
        String token = InvitationTokens.generate();
        String hash = InvitationTokens.sha256Hex(token);

        assertTrue(InvitationTokens.matches(token, hash));
        assertFalse(InvitationTokens.matches(token + "x", hash));
        assertFalse(InvitationTokens.matches(null, hash));
        assertFalse(InvitationTokens.matches(token, null));
    }

    @Test
    void acceptsUrlSafeTokensWithinConfiguredBounds() {
        assertTrue(InvitationTokens.isValidFormat("A".repeat(16)));
        assertTrue(InvitationTokens.isValidFormat("learnloop-dev-invite-math-class-2026"));
        assertTrue(InvitationTokens.isValidFormat(InvitationTokens.generate()));
        assertTrue(InvitationTokens.isValidFormat("aZ09_-".repeat(17) + "aZ09_-"));

        assertFalse(InvitationTokens.isValidFormat(null));
        assertFalse(InvitationTokens.isValidFormat(""));
        assertFalse(InvitationTokens.isValidFormat("A".repeat(15)));
        assertFalse(InvitationTokens.isValidFormat("A".repeat(129)));
    }

    @Test
    void rejectsTokensWithCharactersThatAreNotUrlSafe() {
        assertFalse(InvitationTokens.isValidFormat("valid-token-with space"));
        assertFalse(InvitationTokens.isValidFormat("valid-token-with/slash"));
        assertFalse(InvitationTokens.isValidFormat("valid-token-with?query"));
        assertFalse(InvitationTokens.isValidFormat("valid-token-with#fragment"));
        assertFalse(InvitationTokens.isValidFormat("valid-token-with.dot"));
        assertFalse(InvitationTokens.isValidFormat("valid-token-with+plus"));
    }
}
