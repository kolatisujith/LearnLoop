package com.learnloop.service;

import com.learnloop.security.SessionUser;

public record LoginResult(LoginStatus status, SessionUser user) {

    public static LoginResult failure(LoginStatus status) {
        return new LoginResult(status, null);
    }
}
