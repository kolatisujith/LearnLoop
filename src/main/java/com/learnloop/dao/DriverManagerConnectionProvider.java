package com.learnloop.dao;

import com.learnloop.config.DatabaseConfig;
import com.learnloop.config.DatabaseConfigurationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public final class DriverManagerConnectionProvider implements ConnectionProvider {

    private final DatabaseConfig config;

    public DriverManagerConnectionProvider(DatabaseConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        try {
            Class.forName(config.driverClassName());
        } catch (ClassNotFoundException exception) {
            throw new DatabaseConfigurationException(
                    "JDBC driver was not found on the classpath: " + config.driverClassName(), exception);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", config.username());
        properties.setProperty("password", config.password());
        return DriverManager.getConnection(config.url(), properties);
    }

    public DatabaseConfig config() {
        return config;
    }
}
