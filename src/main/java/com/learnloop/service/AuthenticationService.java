package com.learnloop.service;

import com.learnloop.dao.AuditLogDao;
import com.learnloop.dao.Database;
import com.learnloop.dao.UserDao;
import com.learnloop.model.UserCredentials;
import com.learnloop.security.ClientContext;
import com.learnloop.security.LoginThrottle;
import com.learnloop.security.PasswordHasher;
import com.learnloop.security.SessionUser;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationService.class.getName());
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final String USER_ENTITY_TYPE = "USER";
    private static final String LOGIN_SUCCEEDED = "LOGIN_SUCCEEDED";
    private static final String LOGIN_FAILED = "LOGIN_FAILED";
    private static final String LOGIN_BLOCKED = "LOGIN_BLOCKED";
    private static final String LOGOUT = "LOGOUT";

    private final Database database;
    private final UserDao userDao;
    private final AuditLogDao auditLogDao;
    private final PasswordHasher passwordHasher;
    private final LoginThrottle loginThrottle;

    public AuthenticationService(Database database) {
        this(database, new UserDao(), new AuditLogDao(), new PasswordHasher(), new LoginThrottle());
    }

    public AuthenticationService(
            Database database,
            UserDao userDao,
            AuditLogDao auditLogDao,
            PasswordHasher passwordHasher,
            LoginThrottle loginThrottle) {
        this.database = Objects.requireNonNull(database, "database");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.auditLogDao = Objects.requireNonNull(auditLogDao, "auditLogDao");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.loginThrottle = Objects.requireNonNull(loginThrottle, "loginThrottle");
    }

    public LoginResult authenticate(LoginAttempt attempt) {
        Objects.requireNonNull(attempt, "attempt");
        String email = normalizeEmail(attempt.email());
        String password = attempt.password();
        if (email == null
                || email.length() > MAX_EMAIL_LENGTH
                || password == null
                || password.isEmpty()
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return LoginResult.failure(LoginStatus.INVALID_CREDENTIALS);
        }

        ClientContext clientContext = new ClientContext(attempt.ipAddress(), attempt.userAgent());
        String throttleKey = throttleKey(email, attempt.ipAddress());
        if (loginThrottle.isBlocked(throttleKey)) {
            audit(null, LOGIN_BLOCKED, null, "{\"reason\":\"RATE_LIMIT\"}", clientContext);
            return LoginResult.failure(LoginStatus.RATE_LIMITED);
        }

        UserCredentials credentials = database.withConnection(connection ->
                userDao.findCredentialsByEmail(connection, email).orElse(null));

        boolean passwordMatches = credentials == null
                ? passwordHasher.matchesDummy(password)
                : passwordHasher.matches(password, credentials.passwordHash());

        if (!passwordMatches) {
            loginThrottle.recordFailure(throttleKey);
            audit(
                    credentials == null ? null : credentials.id(),
                    LOGIN_FAILED,
                    credentials == null ? null : credentials.id(),
                    "{\"reason\":\"INVALID_CREDENTIALS\"}",
                    clientContext);
            return LoginResult.failure(LoginStatus.INVALID_CREDENTIALS);
        }

        loginThrottle.reset(throttleKey);
        LoginStatus status = AccountAccessPolicy.evaluate(credentials.approvalStatus());
        if (!status.isSuccess()) {
            audit(
                    credentials.id(),
                    LOGIN_BLOCKED,
                    credentials.id(),
                    "{\"reason\":\"" + status.name() + "\"}",
                    clientContext);
            return LoginResult.failure(status);
        }

        SessionUser user = new SessionUser(
                credentials.id(),
                credentials.email(),
                credentials.fullName(),
                credentials.role(),
                credentials.approvalStatus());
        audit(credentials.id(), LOGIN_SUCCEEDED, credentials.id(), null, clientContext);
        return new LoginResult(LoginStatus.SUCCESS, user);
    }

    public void recordLogout(SessionUser user, ClientContext clientContext) {
        if (user == null) {
            return;
        }
        audit(user.id(), LOGOUT, user.id(), null, clientContext);
    }

    private void audit(
            Long actorUserId,
            String action,
            Long entityId,
            String detailsJson,
            ClientContext clientContext) {
        try {
            database.withConnection(connection -> {
                auditLogDao.insert(
                        connection,
                        actorUserId,
                        action,
                        USER_ENTITY_TYPE,
                        entityId,
                        detailsJson,
                        clientContext == null ? null : clientContext.ipAddress(),
                        clientContext == null ? null : clientContext.userAgent());
                return null;
            });
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Could not write an authentication audit event: " + action, exception);
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static String throttleKey(String email, String ipAddress) {
        String client = ipAddress == null ? "" : ipAddress.trim();
        return email + "|" + client;
    }
}
