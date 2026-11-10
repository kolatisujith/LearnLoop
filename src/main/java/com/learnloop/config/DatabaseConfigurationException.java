package com.learnloop.config;

public class DatabaseConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DatabaseConfigurationException(String message) {
        super(message);
    }

    public DatabaseConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
