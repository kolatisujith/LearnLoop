package com.learnloop.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.annotation.WebListener;

/**
 * Applies browser cookie defaults centrally. Enable the Secure flag in HTTPS deployments with
 * -Dlearnloop.cookie.secure=true; leaving it disabled keeps local HTTP development usable.
 */
@WebListener
public class SecurityConfigurationListener implements ServletContextListener {

    public static final String SECURE_COOKIE_PROPERTY = "learnloop.cookie.secure";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        SessionCookieConfig cookie = event.getServletContext().getSessionCookieConfig();
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setSecure(Boolean.parseBoolean(System.getProperty(SECURE_COOKIE_PROPERTY, "false")));
    }
}
