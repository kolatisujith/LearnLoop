package com.learnloop.service;

import com.learnloop.dao.AuditLogDao;
import com.learnloop.dao.DataAccessException;
import com.learnloop.dao.Database;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.NewUser;
import com.learnloop.model.UserRole;
import com.learnloop.security.PasswordHasher;

import java.time.Instant;
import java.util.Objects;

public final class AdminBootstrapService {

    private final Database database;
    private final UserDao userDao;
    private final AuditLogDao auditLogDao;
    private final PasswordHasher passwordHasher;

    public AdminBootstrapService(Database database) {
        this(database, new UserDao(), new AuditLogDao(), new PasswordHasher());
    }

    public AdminBootstrapService(
            Database database,
            UserDao userDao,
            AuditLogDao auditLogDao,
            PasswordHasher passwordHasher) {
        this.database = Objects.requireNonNull(database, "database");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.auditLogDao = Objects.requireNonNull(auditLogDao, "auditLogDao");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
    }

    public long bootstrap(String email, String fullName, String password) {
        RegistrationRequest validationRequest = new RegistrationRequest(
                email, fullName, UserRole.TEACHER.name(), password, password);
        var errors = new java.util.LinkedHashMap<>(RegistrationValidator.validate(validationRequest));
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        String normalizedEmail = RegistrationValidator.normalizeEmail(email);
        String normalizedName = RegistrationValidator.normalizeFullName(fullName);
        String passwordHash = passwordHasher.hash(password);
        Instant now = Instant.now();

        try {
            return database.inTransaction(connection -> {
                if (!acquireBootstrapLock(connection)) {
                    throw new IllegalStateException(
                            "Could not acquire the administrator bootstrap lock. Try again shortly.");
                }
                try {
                    if (userDao.findFirstAdminForUpdate(connection).isPresent()) {
                        throw new IllegalStateException(
                                "An administrator already exists. The one-time bootstrap command only creates the first admin.");
                    }
                    if (userDao.findByEmail(connection, normalizedEmail).isPresent()) {
                        throw new ValidationException(
                                "email", "An account with this email address already exists.");
                    }

                    long userId = userDao.insert(connection, new NewUser(
                            normalizedEmail,
                            passwordHash,
                            normalizedName,
                            UserRole.ADMIN,
                            ApprovalStatus.APPROVED,
                            now,
                            null));
                    auditLogDao.insert(
                            connection,
                            userId,
                            "ADMIN_BOOTSTRAPPED",
                            "USER",
                            userId,
                            "{\"role\":\"ADMIN\",\"approvalStatus\":\"APPROVED\"}",
                            null,
                            "learnloop-admin-bootstrap");
                    return userId;
                } finally {
                    releaseBootstrapLock(connection);
                }
            });
        } catch (DataAccessException exception) {
            if (isDuplicateKey(exception)) {
                throw new ValidationException(
                        "email", "An account with this email address already exists.");
            }
            throw exception;
        }
    }

    private static boolean acquireBootstrapLock(java.sql.Connection connection) throws java.sql.SQLException {
        try (java.sql.PreparedStatement statement =
                     connection.prepareStatement("SELECT GET_LOCK('learnloop_admin_bootstrap', 10)")) {
            try (java.sql.ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) == 1;
            }
        }
    }

    private static void releaseBootstrapLock(java.sql.Connection connection) {
        try (java.sql.PreparedStatement statement =
                     connection.prepareStatement("SELECT RELEASE_LOCK('learnloop_admin_bootstrap')");
             java.sql.ResultSet ignored = statement.executeQuery()) {
        } catch (java.sql.SQLException ignored) {
        }
    }

    private static boolean isDuplicateKey(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof java.sql.SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (current instanceof java.sql.SQLException sqlException
                    && "23000".equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
