package com.learnloop.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public record DatabaseConfig(String url, String username, String password, String driverClassName) {

    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String CONFIG_PROPERTY = "learnloop.db.config";
    private static final String CONFIG_ENV = "LEARNLOOP_DB_CONFIG";

    public DatabaseConfig {
        url = trimToNull(url);
        username = trimToNull(username);
        password = password == null ? "" : password;
        driverClassName = trimToNull(driverClassName);

        if (url == null) {
            throw new DatabaseConfigurationException(
                    "Database URL is not configured. Set LEARNLOOP_DB_URL, the learnloop.db.url system property, "
                            + "or db.url in db.properties.");
        }
        if (!url.startsWith("jdbc:")) {
            throw new DatabaseConfigurationException("Database URL must start with 'jdbc:'.");
        }
        if (username == null) {
            throw new DatabaseConfigurationException(
                    "Database username is not configured. Set LEARNLOOP_DB_USER, the learnloop.db.username "
                            + "system property, or db.username in db.properties.");
        }
        if (driverClassName == null) {
            driverClassName = DEFAULT_DRIVER;
        }
    }

    public static DatabaseConfig load() {
        return resolve(System.getenv(), System.getProperties(), loadLocalProperties());
    }

    static DatabaseConfig resolve(Map<String, String> environment, Properties systemProperties,
            Properties fileProperties) {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(systemProperties, "systemProperties");
        Objects.requireNonNull(fileProperties, "fileProperties");

        String url = firstNonBlank(
                environment.get("LEARNLOOP_DB_URL"),
                systemProperties.getProperty("learnloop.db.url"),
                fileProperties.getProperty("db.url"));
        String username = firstNonBlank(
                environment.get("LEARNLOOP_DB_USER"),
                environment.get("LEARNLOOP_DB_USERNAME"),
                systemProperties.getProperty("learnloop.db.username"),
                fileProperties.getProperty("db.username"));
        String password = firstNonNull(
                environment.get("LEARNLOOP_DB_PASSWORD"),
                systemProperties.getProperty("learnloop.db.password"),
                fileProperties.getProperty("db.password"));
        String driver = firstNonBlank(
                environment.get("LEARNLOOP_DB_DRIVER"),
                systemProperties.getProperty("learnloop.db.driver"),
                fileProperties.getProperty("db.driver"));

        return new DatabaseConfig(url, username, password, driver);
    }

    private static Properties loadLocalProperties() {
        String configuredPath = firstNonBlank(
                System.getenv(CONFIG_ENV),
                System.getProperty(CONFIG_PROPERTY));
        if (configuredPath != null) {
            return loadPropertiesFile(Path.of(configuredPath));
        }

        Properties properties = new Properties();
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException exception) {
            throw new DatabaseConfigurationException("Could not read db.properties from the classpath.", exception);
        }
        return properties;
    }

    private static Properties loadPropertiesFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new DatabaseConfigurationException("Database configuration file was not found: " + path);
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new DatabaseConfigurationException("Could not read database configuration file: " + path,
                    exception);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public String toString() {
        return "DatabaseConfig[url=" + url
                + ", username=" + username
                + ", password=***"
                + ", driverClassName=" + driverClassName
                + "]";
    }
}
