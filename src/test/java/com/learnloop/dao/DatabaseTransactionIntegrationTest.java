package com.learnloop.dao;

import com.learnloop.support.MySqlTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseTransactionIntegrationTest {

    private Database database;
    private long teacherId;

    @BeforeEach
    void setUp() {
        database = MySqlTestDatabase.database();
        teacherId = MySqlTestDatabase.queryLong(database,
                "SELECT id FROM users WHERE email = ?", "teacher@learnloop.local");
    }

    @Test
    void commitsSuccessfulTransaction() {
        String action = "TEST_COMMIT_" + UUID.randomUUID();
        try {
            String result = database.inTransaction(connection -> {
                new AuditLogDao().insert(connection, teacherId, action, "TEST", null, null, null, null);
                return action;
            });

            assertEquals(action, result);
            assertEquals(1L, countByAction(action));
        } finally {
            MySqlTestDatabase.execute(database, "DELETE FROM audit_logs WHERE action = ?", action);
        }
    }

    @Test
    void rollsBackWhenTransactionFails() {
        String action = "TEST_ROLLBACK_" + UUID.randomUUID();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> database.inTransaction(connection -> {
                    new AuditLogDao().insert(connection, teacherId, action, "TEST", null, null, null, null);
                    throw new IllegalStateException("forced failure");
                }));

        assertEquals("forced failure", exception.getMessage());
        assertEquals(0L, countByAction(action));
    }

    @Test
    void rollsBackWhenCommitTargetThrowsAnError() {
        String action = "TEST_ERROR_ROLLBACK_" + UUID.randomUUID();

        assertThrows(AssertionError.class,
                () -> database.inTransaction(connection -> {
                    new AuditLogDao().insert(connection, teacherId, action, "TEST", null, null, null, null);
                    throw new AssertionError("forced assertion failure");
                }));

        assertEquals(0L, countByAction(action));
    }

    @Test
    void wrapsSqlFailuresAndReportsTheCause() {
        DataAccessException exception = assertThrows(DataAccessException.class,
                () -> MySqlTestDatabase.queryLong(database, "SELECT missing_column FROM users"));

        assertNotNull(exception.getCause());
    }

    private long countByAction(String action) {
        return MySqlTestDatabase.queryLong(database,
                "SELECT COUNT(*) FROM audit_logs WHERE action = ?", action);
    }
}
