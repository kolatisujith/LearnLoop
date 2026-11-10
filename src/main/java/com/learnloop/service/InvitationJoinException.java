package com.learnloop.service;

public class InvitationJoinException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Reason {
        NOT_ELIGIBLE,
        ALREADY_ENROLLED,
        ALREADY_REQUESTED,
        REQUEST_REVIEWED,
        FULL
    }

    private final Reason reason;

    public InvitationJoinException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public boolean isAuthorizationFailure() {
        return reason == Reason.NOT_ELIGIBLE;
    }
}
