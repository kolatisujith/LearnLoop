package com.learnloop.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public final class Database {

    private final ConnectionProvider connectionProvider;

    public Database(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
    }

    public static Database fromConfiguration() {
        return new Database(new DriverManagerConnectionProvider(
                com.learnloop.config.DatabaseConfig.load()));
    }

    public <T> T withConnection(ConnectionWork<T> work) {
        Objects.requireNonNull(work, "work");
        try (Connection connection = connectionProvider.getConnection()) {
            return work.execute(connection);
        } catch (SQLException exception) {
            throw new DataAccessException("Database operation failed.", exception);
        }
    }

    public <T> T inTransaction(TransactionWork<T> work) {
        Objects.requireNonNull(work, "work");
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                if (originalAutoCommit) {
                    connection.setAutoCommit(false);
                }
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw new DataAccessException("Database transaction failed and was rolled back.", exception);
            } catch (RuntimeException | Error exception) {
                rollback(connection, exception);
                throw exception;
            } finally {
                restoreAutoCommit(connection, originalAutoCommit);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not open or close a database connection.", exception);
        }
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    private static void restoreAutoCommit(Connection connection, boolean originalAutoCommit) {
        try {
            if (connection.getAutoCommit() != originalAutoCommit) {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException ignored) {
        }
    }
}
