package com.learnloop.service;

import com.learnloop.dao.ConnectionProvider;
import com.learnloop.dao.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassServiceValidationTest {

    private final ClassService service = new ClassService(new Database(failingConnectionProvider()));

    @Test
    void rejectsNullRequest() {
        assertThrows(NullPointerException.class, () -> service.createClass(null));
    }

    @Test
    void rejectsBlankTitleBeforeUsingTheDatabase() {
        assertInvalid(() -> service.createClass(request("   ", null, null, 10)));
    }

    @Test
    void rejectsTitleLongerThanDatabaseLimit() {
        assertInvalid(() -> service.createClass(request("x".repeat(161), null, null, 10)));
    }

    @Test
    void rejectsNonPositiveIdentifiersBeforeUsingTheDatabase() {
        assertInvalid(() -> service.createClass(new ClassCreationRequest(0, 1, "Class", null, null, 10)));
        assertInvalid(() -> service.createClass(new ClassCreationRequest(1, 0, "Class", null, null, 10)));
    }

    @Test
    void rejectsNonPositiveMaximumStudents() {
        assertInvalid(() -> service.createClass(request("Class", null, null, 0)));
    }

    @Test
    void rejectsPastInvitationExpiry() {
        Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);
        assertInvalid(() -> service.createClass(request("Class", null, past, 10)));
    }

    private static ClassCreationRequest request(
            String title, String description, Instant expiry, Integer maxStudents) {
        return new ClassCreationRequest(1, 1, title, description, expiry, maxStudents);
    }

    private static ConnectionProvider failingConnectionProvider() {
        return () -> {
            throw new AssertionError("The database must not be used for invalid requests.");
        };
    }

    private static void assertInvalid(Executable executable) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertNotNull(exception.getMessage());
        assertFalse(exception.getMessage().isBlank());
    }
}
