package com.learnloop.security;

import jakarta.servlet.http.HttpServletRequest;

public record ClientContext(String ipAddress, String userAgent) {

    private static final int MAX_IP_LENGTH = 45;
    private static final int MAX_USER_AGENT_LENGTH = 512;

    public static ClientContext from(HttpServletRequest request) {
        return new ClientContext(
                truncate(request.getRemoteAddr(), MAX_IP_LENGTH),
                truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH));
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
