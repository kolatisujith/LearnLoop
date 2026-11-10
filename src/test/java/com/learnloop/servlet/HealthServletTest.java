package com.learnloop.servlet;

import com.learnloop.config.AppMetadata;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthServletTest {

    @Test
    void buildsUpStatusPayloadWithApplicationMetadata() {
        String payload = HealthServlet.buildHealthJson(Instant.parse("2026-10-25T10:00:00Z"));

        assertTrue(payload.contains("\"status\":\"UP\""));
        assertTrue(payload.contains("\"application\":\"" + AppMetadata.NAME + "\""));
        assertTrue(payload.contains("\"version\":\"" + AppMetadata.VERSION + "\""));
        assertTrue(payload.contains("\"timestamp\":\"2026-10-25T10:00:00Z\""));
        assertEquals('}', payload.charAt(payload.length() - 1));
    }
}
