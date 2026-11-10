package com.learnloop.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public record PublicUrlConfig(String baseUrl) {

    private static final String ENV_NAME = "LEARNLOOP_APP_BASE_URL";
    private static final String PROPERTY_NAME = "learnloop.app.base-url";

    public PublicUrlConfig {
        baseUrl = normalize(baseUrl);
    }

    public static PublicUrlConfig load() {
        return resolve(System.getenv(), System.getProperties());
    }

    static PublicUrlConfig resolve(Map<String, String> environment, Properties systemProperties) {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(systemProperties, "systemProperties");

        String configured = trimToNull(environment.get(ENV_NAME));
        if (configured == null) {
            configured = trimToNull(systemProperties.getProperty(PROPERTY_NAME));
        }
        if (configured == null) {
            throw new PublicUrlConfigurationException(
                    "Application base URL is not configured. Set " + ENV_NAME + " or the "
                            + PROPERTY_NAME + " system property.");
        }
        return new PublicUrlConfig(configured);
    }

    public String invitationUrl(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Invitation token is required.");
        }
        return baseUrl + "/invite/" + token;
    }

    private static String normalize(String value) {
        String configured = trimToNull(value);
        if (configured == null) {
            throw new PublicUrlConfigurationException("Application base URL must not be blank.");
        }

        URI uri;
        try {
            uri = new URI(configured);
        } catch (URISyntaxException exception) {
            throw new PublicUrlConfigurationException("Application base URL is not a valid URL.", exception);
        }

        String scheme = uri.getScheme();
        if (scheme == null
                || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                || uri.getHost() == null
                || uri.getHost().isBlank()) {
            throw new PublicUrlConfigurationException(
                    "Application base URL must be an absolute http:// or https:// URL.");
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new PublicUrlConfigurationException(
                    "Application base URL must not contain a query string or fragment.");
        }
        if (uri.getUserInfo() != null) {
            throw new PublicUrlConfigurationException(
                    "Application base URL must not include user credentials.");
        }

        String normalized = configured;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
