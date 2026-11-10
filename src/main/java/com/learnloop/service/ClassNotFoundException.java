package com.learnloop.service;

public class ClassNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClassNotFoundException(String message) {
        super(message);
    }
}
