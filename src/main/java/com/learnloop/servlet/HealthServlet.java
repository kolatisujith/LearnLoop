package com.learnloop.servlet;

import com.learnloop.config.AppMetadata;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@WebServlet(name = "HealthServlet", urlPatterns = "/health")
public class HealthServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getWriter().write(buildHealthJson(Instant.now()));
    }

    static String buildHealthJson(Instant timestamp) {
        return String.format(
                Locale.ROOT,
                "{\"status\":\"UP\",\"application\":\"%s\",\"version\":\"%s\",\"phase\":\"%s\",\"timestamp\":\"%s\"}",
                AppMetadata.NAME,
                AppMetadata.VERSION,
                AppMetadata.PHASE,
                DateTimeFormatter.ISO_INSTANT.format(timestamp)
        );
    }
}
