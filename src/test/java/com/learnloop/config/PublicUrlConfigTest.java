package com.learnloop.config;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicUrlConfigTest {

    @Test
    void resolvesEnvironmentBeforePropertiesAndStripsTrailingSlash() {
        Properties system = new Properties();
        system.setProperty("learnloop.app.base-url", "https://system.example/learnloop");

        PublicUrlConfig config = PublicUrlConfig.resolve(
                Map.of("LEARNLOOP_APP_BASE_URL", "  https://learn.example/learnloop///  "),
                system);

        assertEquals("https://learn.example/learnloop", config.baseUrl());
        assertEquals(
                "https://learn.example/learnloop/invite/token_123",
                config.invitationUrl("token_123"));
    }

    @Test
    void fallsBackToSystemProperty() {
        Properties system = new Properties();
        system.setProperty("learnloop.app.base-url", "http://localhost:18081/learnloop/");

        PublicUrlConfig config = PublicUrlConfig.resolve(Map.of(), system);

        assertEquals("http://localhost:18081/learnloop", config.baseUrl());
    }

    @Test
    void rejectsMissingConfiguration() {
        assertThrows(PublicUrlConfigurationException.class,
                () -> PublicUrlConfig.resolve(Map.of(), new Properties()));
        assertThrows(PublicUrlConfigurationException.class,
                () -> PublicUrlConfig.resolve(Map.of("LEARNLOOP_APP_BASE_URL", "   "), new Properties()));
    }

    @Test
    void rejectsRelativeNonHttpAndCredentialUrls() {
        assertThrows(PublicUrlConfigurationException.class,
                () -> new PublicUrlConfig("/learnloop"));
        assertThrows(PublicUrlConfigurationException.class,
                () -> new PublicUrlConfig("ftp://learnloop.example"));
        assertThrows(PublicUrlConfigurationException.class,
                () -> new PublicUrlConfig("https://user:secret@learnloop.example"));
        assertThrows(PublicUrlConfigurationException.class,
                () -> new PublicUrlConfig("https://learnloop.example/learnloop?next=/teacher"));
        assertThrows(PublicUrlConfigurationException.class,
                () -> new PublicUrlConfig("https://learnloop.example/learnloop#invite"));
    }

    @Test
    void rejectsInvitationWithoutToken() {
        PublicUrlConfig config = new PublicUrlConfig("https://learnloop.example");

        assertThrows(IllegalArgumentException.class, () -> config.invitationUrl(null));
        assertThrows(IllegalArgumentException.class, () -> config.invitationUrl("  "));
    }
}
