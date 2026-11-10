package com.learnloop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public final class NotificationDao {

    public void insert(
            Connection connection,
            long recipientUserId,
            Long actorUserId,
            String type,
            String title,
            String message,
            String linkUrl,
            String relatedEntityType,
            Long relatedEntityId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                INSERT INTO notifications (
                    recipient_user_id, actor_user_id, type, title, message, link_url, related_entity_type, related_entity_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, recipientUserId);
            if (actorUserId == null) {
                statement.setNull(2, Types.BIGINT);
            } else {
                statement.setLong(2, actorUserId);
            }
            statement.setString(3, type);
            statement.setString(4, title);
            statement.setString(5, message);
            if (linkUrl == null) {
                statement.setNull(6, Types.VARCHAR);
            } else {
                statement.setString(6, linkUrl);
            }
            if (relatedEntityType == null) {
                statement.setNull(7, Types.VARCHAR);
            } else {
                statement.setString(7, relatedEntityType);
            }
            if (relatedEntityId == null) {
                statement.setNull(8, Types.BIGINT);
            } else {
                statement.setLong(8, relatedEntityId);
            }
            statement.executeUpdate();
        }
    }
}
