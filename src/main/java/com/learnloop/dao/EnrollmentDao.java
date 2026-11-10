package com.learnloop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public final class EnrollmentDao {

    public boolean exists(Connection connection, long classId, long studentUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = "SELECT 1 FROM class_enrollments WHERE class_id = ? AND student_user_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public long insert(
            Connection connection,
            long classId,
            long studentUserId,
            Long joinRequestId,
            Long enrolledByUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                INSERT INTO class_enrollments (class_id, student_user_id, join_request_id, enrolled_by_user_id, status)
                VALUES (?, ?, ?, ?, 'ACTIVE')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, classId);
            statement.setLong(2, studentUserId);
            if (joinRequestId == null) {
                statement.setNull(3, Types.BIGINT);
            } else {
                statement.setLong(3, joinRequestId);
            }
            if (enrolledByUserId == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, enrolledByUserId);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("The database did not return a generated enrollment id.");
    }
}
