package com.learnloop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public final class AuditLogDao {

    public void insert(
            Connection connection,
            Long actorUserId,
            String action,
            String entityType,
            Long entityId,
            String detailsJson,
            String ipAddress,
            String userAgent) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                INSERT INTO audit_logs (
                    actor_user_id, action, entity_type, entity_id, details, ip_address, user_agent
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setNullableLong(statement, 1, actorUserId);
            statement.setString(2, action);
            statement.setString(3, entityType);
            JdbcSupport.setNullableLong(statement, 4, entityId);
            if (detailsJson == null) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, detailsJson);
            }
            statement.setString(6, ipAddress);
            statement.setString(7, userAgent);
            statement.executeUpdate();
        }
    }
}
