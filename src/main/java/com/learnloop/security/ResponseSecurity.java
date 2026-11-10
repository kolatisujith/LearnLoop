package com.learnloop.security;

import jakarta.servlet.http.HttpServletResponse;

public final class ResponseSecurity {

    private ResponseSecurity() {
    }

    public static void noStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
