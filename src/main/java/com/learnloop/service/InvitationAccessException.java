package com.learnloop.service;

public class InvitationAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Reason {
        MALFORMED,
        NOT_FOUND,
        NOT_ISSUED,
        EXPIRED,
        REVOKED,
        CLASS_INACTIVE
    }

    private final Reason reason;

    public InvitationAccessException(Reason reason) {
        super("This invitation link is not available.");
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
