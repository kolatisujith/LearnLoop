package com.learnloop.service;

import com.learnloop.dao.AuditLogDao;
import com.learnloop.dao.DataAccessException;
import com.learnloop.dao.Database;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.NewUser;
import com.learnloop.model.UserAccount;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;
import com.learnloop.security.PasswordHasher;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class UserService {

    private static final String ACCOUNT_REGISTERED_ACTION = "ACCOUNT_REGISTERED";
    private static final String USER_ENTITY_TYPE = "USER";

    private final Database database;
    private final UserDao userDao;
    private final AuditLogDao auditLogDao;
    private final PasswordHasher passwordHasher;

    public UserService(Database database) {
        this(database, new UserDao(), new AuditLogDao(), new PasswordHasher());
    }

    public UserService(Database database, UserDao userDao, AuditLogDao auditLogDao, PasswordHasher passwordHasher) {
        this.database = Objects.requireNonNull(database, "database");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.auditLogDao = Objects.requireNonNull(auditLogDao, "auditLogDao");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
    }

    public UserAccount register(RegistrationRequest request) {
        return register(request, null);
    }

    public UserAccount register(RegistrationRequest request, ClientContext clientContext) {
        Map<String, String> validationErrors = RegistrationValidator.validate(request);
        if (!validationErrors.isEmpty()) {
            throw new ValidationException(validationErrors);
        }

        String email = RegistrationValidator.normalizeEmail(request.email());
        String fullName = RegistrationValidator.normalizeFullName(request.fullName());
        UserRole role = UserRole.valueOf(RegistrationValidator.normalizeRole(request.role()));
        ApprovalStatus approvalStatus = role == UserRole.TEACHER
                ? ApprovalStatus.PENDING
                : ApprovalStatus.APPROVED;
        Instant approvedAt = approvalStatus == ApprovalStatus.APPROVED ? Instant.now() : null;
        String passwordHash = passwordHasher.hash(request.password());

        NewUser newUser = new NewUser(
                email,
                passwordHash,
                fullName,
                role,
                approvalStatus,
                approvedAt,
                null);

        try {
            return database.inTransaction(connection -> {
                if (userDao.findByEmail(connection, email).isPresent()) {
                    throw duplicateEmail();
                }

                long userId = userDao.insert(connection, newUser);
                auditLogDao.insert(
                        connection,
                        userId,
                        ACCOUNT_REGISTERED_ACTION,
                        USER_ENTITY_TYPE,
                        userId,
                        "{\"role\":\"" + role.name() + "\",\"approvalStatus\":\""
                                + approvalStatus.name() + "\"}",
                        clientContext == null ? null : clientContext.ipAddress(),
                        clientContext == null ? null : clientContext.userAgent());

                return userDao.findById(connection, userId)
                        .orElseThrow(() -> new DataAccessException(
                                "The registered account could not be reloaded."));
            });
        } catch (DataAccessException exception) {
            if (isDuplicateKey(exception)) {
                throw duplicateEmail();
            }
            throw exception;
        }
    }

    private static ValidationException duplicateEmail() {
        return new ValidationException(
                RegistrationValidator.EMAIL,
                "An account with this email address already exists.");
    }

    private static boolean isDuplicateKey(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (current instanceof SQLException sqlException
                    && "23000".equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
