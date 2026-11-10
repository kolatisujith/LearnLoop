package com.learnloop.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsrfTokensTest {

    @Test
    void comparesTokensInConstantTimeSemantics() {
        assertTrue(CsrfTokens.constantTimeEquals("token-value", "token-value"));
        assertFalse(CsrfTokens.constantTimeEquals("token-value", "token-VALUE"));
        assertFalse(CsrfTokens.constantTimeEquals("short", "a-much-longer-token"));
    }
}
