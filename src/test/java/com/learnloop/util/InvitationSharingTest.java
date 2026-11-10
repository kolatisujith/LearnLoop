package com.learnloop.util;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationSharingTest {

    @Test
    void buildsEncodedWhatsAppShareUrl() {
        String message = "Join \"Algebra Foundations\" on LearnLoop: "
                + "https://learn.example/learnloop/invite/abc_123?&source=share";

        String url = InvitationSharing.whatsappUrl(message);

        assertTrue(url.startsWith("https://wa.me/?text="));
        assertFalse(url.contains(" "));
        assertFalse(url.contains("\""));
        String encodedMessage = url.substring("https://wa.me/?text=".length());
        assertEquals(message, URLDecoder.decode(encodedMessage, StandardCharsets.UTF_8));
        assertEquals("https", URI.create(url).getScheme());
    }

    @Test
    void encodesUnicodeAndNewlines() {
        String message = "Join LearnLoop\nClass: \u0905\u0928\u0941\u092e\u0924";

        String url = InvitationSharing.whatsappUrl(message);

        assertEquals(
                message,
                URLDecoder.decode(
                        url.substring("https://wa.me/?text=".length()),
                        StandardCharsets.UTF_8));
    }

    @Test
    void rejectsNullMessage() {
        assertThrows(NullPointerException.class, () -> InvitationSharing.whatsappUrl(null));
    }
}
