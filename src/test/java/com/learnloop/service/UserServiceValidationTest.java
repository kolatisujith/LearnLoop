package com.learnloop.service;

import com.learnloop.dao.AuditLogDao;
import com.learnloop.dao.ConnectionProvider;
import com.learnloop.dao.Database;
import com.learnloop.dao.UserDao;
import com.learnloop.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceValidationTest {

    private final UserService service = new UserService(
            new Database(failingConnectionProvider()),
            new UserDao(),
            new AuditLogDao(),
            new PasswordHasher(4));

    @Test
    void rejectsInvalidRegistrationBeforeUsingTheDatabase() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.register(new RegistrationRequest(
                        "invalid-email", "Student Name", "STUDENT", "weak", "weak")));

        assertEquals("Enter a valid email address.", exception.fieldErrors().get("email"));
    }

    @Test
    void rejectsAdminSelfRegistrationBeforeUsingTheDatabase() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.register(new RegistrationRequest(
                        "admin@example.com", "Admin Name", "ADMIN",
                        "LearnLoop#Test9", "LearnLoop#Test9")));

        assertEquals(
                "Administrator accounts cannot be created through public registration.",
                exception.fieldErrors().get("role"));
    }

    private static ConnectionProvider failingConnectionProvider() {
        return () -> {
            throw new AssertionError("The database must not be used for invalid registrations.");
        };
    }
}
