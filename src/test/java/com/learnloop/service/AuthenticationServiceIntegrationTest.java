package com.learnloop.service;

import com.learnloop.dao.AuditLogDao;
import com.learnloop.dao.Database;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.UserAccount;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;
import com.learnloop.security.LoginThrottle;
import com.learnloop.security.PasswordHasher;
import com.learnloop.support.MySqlTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationServiceIntegrationTest {

    private static final String PASSWORD = "LearnLoop#Test9";
    private static final ClientContext CLIENT = new ClientContext("127.0.0.1", "junit-integration");

    private Database database;
    private UserService userService;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        database = MySqlTestDatabase.database();
        userService = new UserService(database, new UserDao(), new AuditLogDao(), new PasswordHasher(4));
        authenticationService = new AuthenticationService(
                database,
                new UserDao(),
                new AuditLogDao(),
                new PasswordHasher(4),
                new LoginThrottle());
    }

    @Test
    void registersApprovedStudentHashesPasswordAndAllowsLogin() {
        String email = uniqueEmail("student");
        long userId = -1;
        try {
            UserAccount account = userService.register(new RegistrationRequest(
                    email, "Integration Student", "STUDENT", PASSWORD, PASSWORD), CLIENT);
            userId = account.id();

            assertEquals(UserRole.STUDENT, account.role());
            assertEquals(ApprovalStatus.APPROVED, account.approvalStatus());

            String storedHash = MySqlTestDatabase.queryString(
                    database, "SELECT password_hash FROM users WHERE id = ?", userId);
            assertNotEquals(PASSWORD, storedHash);
            assertTrue(storedHash.startsWith("$2"));
            assertTrue(new PasswordHasher(4).matches(PASSWORD, storedHash));

            LoginResult failed = authenticationService.authenticate(
                    new LoginAttempt(email, "Wrong#Password1", "127.0.0.1", "junit-integration"));
            assertEquals(LoginStatus.INVALID_CREDENTIALS, failed.status());

            LoginResult result = authenticationService.authenticate(
                    new LoginAttempt(email, PASSWORD, "127.0.0.1", "junit-integration"));
            assertEquals(LoginStatus.SUCCESS, result.status());
            assertEquals(userId, result.user().id());
            assertEquals(UserRole.STUDENT, result.user().role());

            assertEquals(1L, MySqlTestDatabase.queryLong(database,
                    "SELECT COUNT(*) FROM audit_logs WHERE action = 'ACCOUNT_REGISTERED' AND entity_id = ?",
                    userId));
            assertEquals(1L, MySqlTestDatabase.queryLong(database,
                    "SELECT COUNT(*) FROM audit_logs WHERE action = 'LOGIN_SUCCEEDED' AND actor_user_id = ?",
                    userId));
        } finally {
            deleteUser(userId);
        }
    }

    @Test
    void keepsTeacherPendingAndBlocksLoginUntilApproval() {
        String email = uniqueEmail("teacher");
        long userId = -1;
        try {
            UserAccount account = userService.register(new RegistrationRequest(
                    email, "Integration Teacher", "TEACHER", PASSWORD, PASSWORD), CLIENT);
            userId = account.id();

            assertEquals(UserRole.TEACHER, account.role());
            assertEquals(ApprovalStatus.PENDING, account.approvalStatus());
            assertEquals(0L, MySqlTestDatabase.queryLong(database,
                    "SELECT COUNT(*) FROM users WHERE id = ? AND approved_at IS NOT NULL", userId));

            LoginResult result = authenticationService.authenticate(
                    new LoginAttempt(email, PASSWORD, "127.0.0.1", "junit-integration"));

            assertEquals(LoginStatus.ACCOUNT_PENDING, result.status());
            assertEquals(1L, MySqlTestDatabase.queryLong(database,
                    "SELECT COUNT(*) FROM audit_logs WHERE action = 'LOGIN_BLOCKED' AND actor_user_id = ?",
                    userId));
        } finally {
            deleteUser(userId);
        }
    }

    @Test
    void rejectsDuplicateEmailAtTheServiceBoundary() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.register(new RegistrationRequest(
                        "alice.student@learnloop.local",
                        "Duplicate Alice",
                        "STUDENT",
                        PASSWORD,
                        PASSWORD), CLIENT));

        assertEquals("An account with this email address already exists.",
                exception.fieldErrors().get(RegistrationValidator.EMAIL));
    }

    @Test
    void rejectsUnknownAccountsWithTheSameGenericResultAsAWrongPassword() {
        try {
            LoginResult unknown = authenticationService.authenticate(new LoginAttempt(
                    uniqueEmail("missing"), PASSWORD, "127.0.0.1", "junit-integration"));

            assertEquals(LoginStatus.INVALID_CREDENTIALS, unknown.status());
        } finally {
            MySqlTestDatabase.execute(database,
                    "DELETE FROM audit_logs WHERE action = 'LOGIN_FAILED'"
                            + " AND actor_user_id IS NULL AND entity_id IS NULL"
                            + " ORDER BY id DESC LIMIT 1");
        }
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID() + "@example.com";
    }

    private void deleteUser(long userId) {
        if (userId <= 0) {
            return;
        }
        MySqlTestDatabase.execute(database,
                "DELETE FROM audit_logs WHERE actor_user_id = ? OR (entity_type = 'USER' AND entity_id = ?)",
                userId, userId);
        MySqlTestDatabase.execute(database, "DELETE FROM users WHERE id = ?", userId);
    }
}
