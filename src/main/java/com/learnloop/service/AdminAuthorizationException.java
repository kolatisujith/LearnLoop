package com.learnloop.service;

public class AdminAuthorizationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AdminAuthorizationException(String message) {
        super(message);
    }
}
