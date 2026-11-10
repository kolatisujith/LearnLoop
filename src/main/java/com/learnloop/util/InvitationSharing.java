package com.learnloop.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class InvitationSharing {

    private static final String WHATSAPP_BASE_URL = "https://wa.me/?text=";

    private InvitationSharing() {
    }

    public static String whatsappUrl(String message) {
        Objects.requireNonNull(message, "message");
        return WHATSAPP_BASE_URL + URLEncoder.encode(message, StandardCharsets.UTF_8);
    }
}
