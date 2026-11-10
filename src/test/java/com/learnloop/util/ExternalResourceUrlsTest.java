package com.learnloop.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalResourceUrlsTest {

    @Test
    void acceptsAbsoluteHttpAndHttpsUrls() {
        assertEquals("https://example.com/path?q=1",
                ExternalResourceUrls.safeHttpUrl("  https://example.com/path?q=1  "));
        assertEquals("http://example.com", ExternalResourceUrls.safeHttpUrl("http://example.com"));
    }

    @Test
    void rejectsDangerousOrRelativeSchemes() {
        assertNull(ExternalResourceUrls.safeHttpUrl("javascript:alert(1)"));
        assertNull(ExternalResourceUrls.safeHttpUrl("data:text/html,<script>alert(1)</script>"));
        assertNull(ExternalResourceUrls.safeHttpUrl("file:///C:/secret.txt"));
        assertNull(ExternalResourceUrls.safeHttpUrl("/relative/path"));
        assertNull(ExternalResourceUrls.safeHttpUrl("//example.com/path"));
        assertNull(ExternalResourceUrls.safeHttpUrl(""));
        assertNull(ExternalResourceUrls.safeHttpUrl(null));
    }

    @Test
    void rejectsCredentialsAndControlCharacters() {
        assertNull(ExternalResourceUrls.safeHttpUrl("https://user:password@example.com/"));
        assertNull(ExternalResourceUrls.safeHttpUrl("https://example.com/\r\nLocation: https://evil.test"));
    }

    @Test
    void rejectsUrlsThatAreTooLong() {
        String prefix = "https://example.com/";
        String url = prefix + "a".repeat(ExternalResourceUrls.MAX_URL_LENGTH - prefix.length() + 1);
        assertNull(ExternalResourceUrls.safeHttpUrl(url));
    }

    @Test
    void acceptsGoogleMeetLinksOnly() {
        assertEquals("https://meet.google.com/abc-defg-hij",
                ExternalResourceUrls.googleMeetUrl("https://meet.google.com/abc-defg-hij"));
        assertNull(ExternalResourceUrls.googleMeetUrl("  "));
        assertThrows(IllegalArgumentException.class,
                () -> ExternalResourceUrls.googleMeetUrl("http://meet.google.com/abc-defg-hij"));
        assertThrows(IllegalArgumentException.class,
                () -> ExternalResourceUrls.googleMeetUrl("https://meet.google.com.evil.test/abc"));
        assertThrows(IllegalArgumentException.class,
                () -> ExternalResourceUrls.googleMeetUrl("https://example.com/abc-defg-hij"));
        assertThrows(IllegalArgumentException.class,
                () -> ExternalResourceUrls.googleMeetUrl("https://meet.google.com/"));
        assertThrows(IllegalArgumentException.class,
                () -> ExternalResourceUrls.googleMeetUrl("javascript:alert(1)"));
    }

    @Test
    void reportsGoogleMeetHostAndValidity() {
        assertTrue(ExternalResourceUrls.isGoogleMeetUrl("https://meet.google.com/abc-defg-hij"));
        assertFalse(ExternalResourceUrls.isGoogleMeetUrl("https://meet.google.com.evil.test/abc"));
        assertEquals("meet.google.com",
                ExternalResourceUrls.hostOf("https://meet.google.com/abc-defg-hij"));
    }

    @Test
    void acceptsJitsiRoomUrlsForInPlatformDemoMeetings() {
        assertEquals("https://meet.jit.si/learnloop-class-42-Abc_123-def",
                ExternalResourceUrls.jitsiMeetUrl("https://meet.jit.si/learnloop-class-42-Abc_123-def"));
        assertTrue(ExternalResourceUrls.isJitsiMeetUrl("https://meet.jit.si/learnloop-room-12345678"));
        assertEquals("learnloop-class-42-Abc_123-def",
                ExternalResourceUrls.jitsiRoomName("https://meet.jit.si/learnloop-class-42-Abc_123-def"));
    }

    @Test
    void rejectsUnsafeOrMalformedJitsiRooms() {
        assertFalse(ExternalResourceUrls.isJitsiMeetUrl("http://meet.jit.si/learnloop-room-12345678"));
        assertFalse(ExternalResourceUrls.isJitsiMeetUrl("https://meet.jit.si/room/extra-segment"));
        assertFalse(ExternalResourceUrls.isJitsiMeetUrl("https://meet.jit.si/short"));
        assertFalse(ExternalResourceUrls.isJitsiMeetUrl("https://meet.jit.si.evil.test/learnloop-room-12345678"));
        assertFalse(ExternalResourceUrls.isJitsiMeetUrl("https://meet.jit.si/learnloop-room-12345678?config.test=1"));
        assertThrows(IllegalArgumentException.class,
                () -> ExternalResourceUrls.jitsiMeetUrl("https://meet.jit.si/room/extra-segment"));
    }

    @Test
    void acceptsEitherGoogleMeetOrJitsiForLiveSessionSettings() {
        assertEquals("https://meet.google.com/abc-defg-hij",
                ExternalResourceUrls.meetingUrl("https://meet.google.com/abc-defg-hij"));
        assertEquals("https://meet.jit.si/learnloop-room-12345678",
                ExternalResourceUrls.meetingUrl("https://meet.jit.si/learnloop-room-12345678"));
        assertNull(ExternalResourceUrls.meetingUrl("  "));
        assertThrows(IllegalArgumentException.class,
                () -> ExternalResourceUrls.meetingUrl("https://example.com/not-a-meeting"));
    }
}
