package com.learnloop.config;

public class PublicUrlConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PublicUrlConfigurationException(String message) {
        super(message);
    }

    public PublicUrlConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
