package com.learnloop.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class SessionSecurity {

    public static final String SESSION_USER_ATTRIBUTE = "authUser";
    public static final String POST_LOGIN_PATH_ATTRIBUTE = "postLoginPath";

    private SessionSecurity() {
    }

    public static SessionUser currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER_ATTRIBUTE);
        return value instanceof SessionUser user ? user : null;
    }

    public static SessionUser signIn(HttpServletRequest request, SessionUser user) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(SESSION_USER_ATTRIBUTE, user);
        CsrfTokens.rotateToken(session);
        return user;
    }

    public static void signOut(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public static void rememberPostLoginPath(HttpServletRequest request, String path) {
        String safePath = SafeRedirects.safeLocalPath(path);
        if (safePath != null) {
            request.getSession(true).setAttribute(POST_LOGIN_PATH_ATTRIBUTE, safePath);
        }
    }

    public static String postLoginPath(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(POST_LOGIN_PATH_ATTRIBUTE);
        return value instanceof String path ? SafeRedirects.safeLocalPath(path) : null;
    }

    public static void clearPostLoginPath(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(POST_LOGIN_PATH_ATTRIBUTE);
        }
    }

    public static String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (uri == null) {
            return "/";
        }
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
