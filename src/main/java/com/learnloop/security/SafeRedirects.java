package com.learnloop.security;

import java.util.Locale;

public final class SafeRedirects {

    private SafeRedirects() {
    }

    public static String safeLocalPath(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        String path = candidate.trim();
        if (!path.startsWith("/") || path.startsWith("//") || path.indexOf('\\') >= 0) {
            return null;
        }
        if (path.indexOf('\r') >= 0 || path.indexOf('\n') >= 0) {
            return null;
        }
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/web-inf") || lower.startsWith("/meta-inf")) {
            return null;
        }
        if (lower.equals("/login") || lower.equals("/register") || lower.equals("/logout")) {
            return null;
        }
        return path;
    }
}
