package com.learnloop.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates external resource links before they are stored or rendered.
 *
 * <p>Only absolute {@code http://} and {@code https://} URLs without embedded credentials are
 * accepted. Dangerous schemes such as {@code javascript:}, {@code data:} and {@code file:} are
 * rejected so a stored value can never turn into a script URL in a page.</p>
 */
public final class ExternalResourceUrls {

    public static final int MAX_URL_LENGTH = 1000;
    public static final String GOOGLE_MEET_HOST = "meet.google.com";
    public static final String JITSI_MEET_HOST = "meet.jit.si";
    private static final Pattern JITSI_ROOM_PATTERN = Pattern.compile("[A-Za-z0-9_-]{8,120}");

    private ExternalResourceUrls() {
    }

    /**
     * Returns the trimmed URL when it is a safe absolute HTTP(S) URL, otherwise {@code null}.
     */
    public static String safeHttpUrl(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null || trimmed.length() > MAX_URL_LENGTH) {
            return null;
        }
        if (containsControlCharacter(trimmed)) {
            return null;
        }
        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException exception) {
            return null;
        }
        if (!uri.isAbsolute() || uri.isOpaque()) {
            return null;
        }
        String scheme = uri.getScheme();
        if (scheme == null
                || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            return null;
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            return null;
        }
        if (uri.getUserInfo() != null) {
            return null;
        }
        return trimmed;
    }

    /**
     * Validates a Google Meet live-session link. Only {@code https://meet.google.com/...} URLs are
     * accepted. A blank value clears the stored link.
     */
    public static String googleMeetUrl(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String safe = safeHttpUrl(trimmed);
        if (safe == null) {
            throw new IllegalArgumentException(
                    "Enter a secure Google Meet link such as https://meet.google.com/abc-defg-hij.");
        }
        URI uri;
        try {
            uri = new URI(safe);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("The Google Meet link is not a valid URL.", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !GOOGLE_MEET_HOST.equalsIgnoreCase(uri.getHost())) {
            throw new IllegalArgumentException(
                    "Enter a secure Google Meet link such as https://meet.google.com/abc-defg-hij.");
        }
        String path = uri.getPath();
        if (path == null || path.length() <= 1) {
            throw new IllegalArgumentException("The Google Meet link must include a meeting code.");
        }
        return safe;
    }

    public static boolean isGoogleMeetUrl(String value) {
        try {
            return googleMeetUrl(value) != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Validates a Jitsi room URL for the embedded development/demo live classroom.
     * Only a single room path on the official public Jitsi host is accepted.
     */
    public static String jitsiMeetUrl(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String safe = safeHttpUrl(trimmed);
        if (safe == null) {
            throw new IllegalArgumentException(
                    "Enter a secure Jitsi room URL such as https://meet.jit.si/your-unique-room-name.");
        }
        URI uri;
        try {
            uri = new URI(safe);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("The Jitsi room URL is not valid.", exception);
        }
        String path = uri.getPath();
        String roomName = path == null || !path.startsWith("/") ? "" : path.substring(1);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !JITSI_MEET_HOST.equalsIgnoreCase(uri.getHost())
                || uri.getQuery() != null
                || uri.getFragment() != null
                || !JITSI_ROOM_PATTERN.matcher(roomName).matches()) {
            throw new IllegalArgumentException(
                    "Enter a secure Jitsi room URL such as https://meet.jit.si/your-unique-room-name.");
        }
        return safe;
    }

    public static boolean isJitsiMeetUrl(String value) {
        try {
            return jitsiMeetUrl(value) != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Validates either a Google Meet URL (external tab) or a Jitsi room URL (embedded in LearnLoop).
     * A blank value clears the configured live session.
     */
    public static String meetingUrl(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        if (isGoogleMeetUrl(trimmed)) {
            return googleMeetUrl(trimmed);
        }
        if (isJitsiMeetUrl(trimmed)) {
            return jitsiMeetUrl(trimmed);
        }
        throw new IllegalArgumentException(
                "Enter a secure Google Meet URL or Jitsi room URL. For an in-platform class, generate a Jitsi room.");
    }

    public static String jitsiRoomName(String value) {
        String safe = jitsiMeetUrl(value);
        if (safe == null) {
            throw new IllegalArgumentException("A Jitsi room URL is required.");
        }
        try {
            return new URI(safe).getPath().substring(1);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("The Jitsi room URL is not valid.", exception);
        }
    }

    public static String hostOf(String value) {
        String safe = safeHttpUrl(value);
        if (safe == null) {
            return null;
        }
        try {
            return new URI(safe).getHost().toLowerCase(Locale.ROOT);
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private static boolean containsControlCharacter(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\r' || character == '\n' || character == '\t'
                    || Character.isISOControl(character)) {
                return true;
            }
        }
        return false;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
