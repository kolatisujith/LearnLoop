package com.learnloop.service;

/**
 * Raised whenever a classroom, material or live-session resource is missing or the current user is
 * not allowed to see it. Callers should respond with 404 so that private resources are not
 * disclosed.
 */
public class ClassroomAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClassroomAccessException(String message) {
        super(message);
    }
}
