package com.learnloop.config;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConfigTest {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/learnloop_db";

    @Test
    void resolvesConfigurationFromEnvironmentFirst() {
        Map<String, String> environment = Map.of(
                "LEARNLOOP_DB_URL", "jdbc:mysql://env-host:3306/learnloop_db",
                "LEARNLOOP_DB_USER", "env-user",
                "LEARNLOOP_DB_PASSWORD", "env-password",
                "LEARNLOOP_DB_DRIVER", "com.example.Driver");
        Properties system = new Properties();
        system.setProperty("learnloop.db.url", "jdbc:mysql://system-host:3306/learnloop_db");
        system.setProperty("learnloop.db.username", "system-user");
        system.setProperty("learnloop.db.password", "system-password");
        Properties file = new Properties();
        file.setProperty("db.url", "jdbc:mysql://file-host:3306/learnloop_db");
        file.setProperty("db.username", "file-user");
        file.setProperty("db.password", "file-password");

        DatabaseConfig config = DatabaseConfig.resolve(environment, system, file);

        assertEquals("jdbc:mysql://env-host:3306/learnloop_db", config.url());
        assertEquals("env-user", config.username());
        assertEquals("env-password", config.password());
        assertEquals("com.example.Driver", config.driverClassName());
    }

    @Test
    void fallsBackToSystemPropertiesThenLocalFile() {
        Properties system = new Properties();
        system.setProperty("learnloop.db.username", "system-user");
        Properties file = new Properties();
        file.setProperty("db.url", URL);
        file.setProperty("db.password", "file-password");

        DatabaseConfig config = DatabaseConfig.resolve(Map.of(), system, file);

        assertEquals(URL, config.url());
        assertEquals("system-user", config.username());
        assertEquals("file-password", config.password());
        assertEquals("com.mysql.cj.jdbc.Driver", config.driverClassName());
    }

    @Test
    void defaultsPasswordToEmptyWhenUnset() {
        Properties file = new Properties();
        file.setProperty("db.url", URL);
        file.setProperty("db.username", "learnloop");

        DatabaseConfig config = DatabaseConfig.resolve(Map.of(), new Properties(), file);

        assertEquals("", config.password());
    }

    @Test
    void rejectsMissingUrl() {
        assertThrows(DatabaseConfigurationException.class,
                () -> DatabaseConfig.resolve(Map.of(), new Properties(), new Properties()));
    }

    @Test
    void rejectsMissingUsername() {
        Properties file = new Properties();
        file.setProperty("db.url", URL);

        assertThrows(DatabaseConfigurationException.class,
                () -> DatabaseConfig.resolve(Map.of(), new Properties(), file));
    }

    @Test
    void rejectsNonJdbcUrl() {
        Properties file = new Properties();
        file.setProperty("db.url", "mysql://127.0.0.1/learnloop_db");
        file.setProperty("db.username", "learnloop");

        assertThrows(DatabaseConfigurationException.class,
                () -> DatabaseConfig.resolve(Map.of(), new Properties(), file));
    }

    @Test
    void redactsPasswordFromToString() {
        DatabaseConfig config = new DatabaseConfig(URL, "learnloop", "super-secret", null);

        assertFalse(config.toString().contains("super-secret"));
        assertTrue(config.toString().contains("password=***"));
    }
}
