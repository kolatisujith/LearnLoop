package com.learnloop.filter;

import com.learnloop.security.SafeRedirects;
import com.learnloop.security.SessionSecurity;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class AuthRedirects {

    private AuthRedirects() {
    }

    static String loginUrl(HttpServletRequest request) {
        String loginPath = request.getContextPath() + "/login";
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return loginPath;
        }
        String next = SafeRedirects.safeLocalPath(SessionSecurity.requestPath(request));
        if (next == null) {
            return loginPath;
        }
        return loginPath + "?next=" + URLEncoder.encode(next, StandardCharsets.UTF_8);
    }
}
