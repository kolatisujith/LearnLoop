package com.learnloop.security;

import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class CsrfTokens {

    public static final String SESSION_ATTRIBUTE = "csrfToken";
    public static final String PARAMETER_NAME = "_csrf";
    public static final String HEADER_NAME = "X-CSRF-Token";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private CsrfTokens() {
    }

    public static String ensureToken(HttpSession session) {
        if (session == null) {
            throw new IllegalArgumentException("A session is required to create a CSRF token.");
        }
        synchronized (session) {
            Object existing = session.getAttribute(SESSION_ATTRIBUTE);
            if (existing instanceof String token && !token.isBlank()) {
                return token;
            }
            return storeNewToken(session);
        }
    }

    public static String rotateToken(HttpSession session) {
        if (session == null) {
            throw new IllegalArgumentException("A session is required to rotate a CSRF token.");
        }
        synchronized (session) {
            return storeNewToken(session);
        }
    }

    public static boolean isValid(HttpSession session, String submittedToken) {
        if (session == null || submittedToken == null || submittedToken.isBlank()) {
            return false;
        }
        Object stored = session.getAttribute(SESSION_ATTRIBUTE);
        if (!(stored instanceof String expected) || expected.isBlank()) {
            return false;
        }
        return constantTimeEquals(expected, submittedToken);
    }

    static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }

    private static String storeNewToken(HttpSession session) {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_ATTRIBUTE, token);
        return token;
    }
}
