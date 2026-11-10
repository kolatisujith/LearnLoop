package com.learnloop.dao;

import com.learnloop.service.ClassroomViews.Assessment;
import com.learnloop.service.ClassroomViews.Classroom;
import com.learnloop.service.ClassroomViews.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ClassroomDao {

    private static final String CLASSROOM_SELECT = """
            SELECT
                c.id,
                c.subject_id,
                s.code AS subject_code,
                s.name AS subject_name,
                c.title,
                c.description,
                c.status,
                c.teacher_user_id,
                u.full_name AS teacher_name,
                c.max_students,
                c.meeting_url,
                c.created_at,
                c.updated_at,
                (SELECT COUNT(*) FROM class_enrollments e
                 WHERE e.class_id = c.id AND e.status = 'ACTIVE') AS enrolled_count
            FROM classes c
            JOIN subjects s ON s.id = c.subject_id
            JOIN users u ON u.id = c.teacher_user_id
            WHERE c.id = ?
            """;

    private static final String MATERIAL_SELECT = """
            SELECT
                m.id,
                m.class_id,
                c.title AS class_title,
                s.code AS subject_code,
                u.full_name AS teacher_name,
                c.teacher_user_id,
                c.status AS class_status,
                m.title,
                m.description,
                m.material_type,
                m.resource_url,
                m.content_text,
                m.file_name,
                m.stored_name,
                m.mime_type,
                m.file_size_bytes,
                m.position,
                m.is_published,
                m.published_at,
                m.created_at,
                m.updated_at
            FROM class_materials m
            JOIN classes c ON c.id = m.class_id
            JOIN subjects s ON s.id = c.subject_id
            JOIN users u ON u.id = c.teacher_user_id
            """;

    public Optional<Classroom> findClassroom(Connection connection, long classId, boolean publishedOnly)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        ClassroomBase base = null;
        try (PreparedStatement statement = connection.prepareStatement(CLASSROOM_SELECT)) {
            statement.setLong(1, classId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    base = mapClassroomBase(resultSet);
                }
            }
        }
        if (base == null) {
            return Optional.empty();
        }
        return Optional.of(new Classroom(
                base.id(),
                base.subjectId(),
                base.subjectCode(),
                base.subjectName(),
                base.title(),
                base.description(),
                base.status(),
                base.teacherUserId(),
                base.teacherName(),
                base.maxStudents(),
                base.enrolledCount(),
                base.meetingUrl(),
                base.createdAt(),
                base.updatedAt(),
                findMaterials(connection, classId, publishedOnly),
                findAssessments(connection, classId, publishedOnly)));
    }

    public List<Material> findMaterials(Connection connection, long classId, boolean publishedOnly)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = MATERIAL_SELECT + """
                WHERE m.class_id = ?
                """ + (publishedOnly ? "  AND m.is_published = TRUE\n" : "")
                + "ORDER BY m.position ASC, m.created_at ASC, m.id ASC";
        List<Material> materials = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    materials.add(mapMaterial(resultSet));
                }
            }
        }
        return List.copyOf(materials);
    }

    public List<Assessment> findAssessments(Connection connection, long classId, boolean publishedOnly)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT id, class_id, title, instructions, status, available_from, due_at,
                       time_limit_minutes, max_attempts
                FROM assessments
                WHERE class_id = ?
                """ + (publishedOnly ? "  AND status = 'PUBLISHED'\n" : "")
                + "ORDER BY due_at IS NULL, due_at ASC, created_at ASC, id ASC";
        List<Assessment> assessments = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    assessments.add(mapAssessment(resultSet));
                }
            }
        }
        return List.copyOf(assessments);
    }

    public Optional<Material> findMaterial(Connection connection, long materialId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = MATERIAL_SELECT + "WHERE m.id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, materialId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapMaterial(resultSet)) : Optional.empty();
            }
        }
    }

    public int nextPosition(Connection connection, long classId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = "SELECT COALESCE(MAX(position), 0) + 1 FROM class_materials WHERE class_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 1;
            }
        }
    }

    public long insertMaterial(Connection connection, NewMaterial material) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(material, "material");
        String sql = """
                INSERT INTO class_materials (
                    class_id, uploaded_by_user_id, title, description, material_type,
                    resource_url, content_text, file_name, stored_name, mime_type,
                    file_size_bytes, position, is_published, published_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, material.classId());
            JdbcSupport.setNullableLong(statement, 2, material.uploadedByUserId());
            statement.setString(3, material.title());
            statement.setString(4, material.description());
            statement.setString(5, material.materialType());
            statement.setString(6, material.resourceUrl());
            statement.setString(7, material.contentText());
            statement.setString(8, material.fileName());
            statement.setString(9, material.storedName());
            statement.setString(10, material.mimeType());
            JdbcSupport.setNullableLong(statement, 11, material.fileSizeBytes());
            statement.setInt(12, material.position());
            statement.setBoolean(13, material.published());
            JdbcSupport.setInstant(statement, 14,
                    material.published() ? (material.publishedAt() == null ? Instant.now() : material.publishedAt())
                            : null);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("The database did not return a generated material id.");
    }

    public int updateMaterial(Connection connection, long materialId, long classId, MaterialUpdate update)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(update, "update");
        String sql = """
                UPDATE class_materials
                SET title = ?, description = ?, material_type = ?, resource_url = ?,
                    content_text = ?, file_name = ?, stored_name = ?, mime_type = ?,
                    file_size_bytes = ?
                WHERE id = ? AND class_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, update.title());
            statement.setString(2, update.description());
            statement.setString(3, update.materialType());
            statement.setString(4, update.resourceUrl());
            statement.setString(5, update.contentText());
            statement.setString(6, update.fileName());
            statement.setString(7, update.storedName());
            statement.setString(8, update.mimeType());
            JdbcSupport.setNullableLong(statement, 9, update.fileSizeBytes());
            statement.setLong(10, materialId);
            statement.setLong(11, classId);
            return statement.executeUpdate();
        }
    }

    public int setPublished(Connection connection, long materialId, long classId, boolean published)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE class_materials
                SET is_published = ?, published_at = ?
                WHERE id = ? AND class_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, published);
            if (published) {
                statement.setTimestamp(2, Timestamp.from(Instant.now()));
            } else {
                statement.setNull(2, Types.TIMESTAMP);
            }
            statement.setLong(3, materialId);
            statement.setLong(4, classId);
            return statement.executeUpdate();
        }
    }

    public int deleteMaterial(Connection connection, long materialId, long classId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = "DELETE FROM class_materials WHERE id = ? AND class_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, materialId);
            statement.setLong(2, classId);
            return statement.executeUpdate();
        }
    }

    public int updateMeetingUrl(Connection connection, long classId, long teacherUserId, String meetingUrl)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = "UPDATE classes SET meeting_url = ? WHERE id = ? AND teacher_user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, meetingUrl);
            statement.setLong(2, classId);
            statement.setLong(3, teacherUserId);
            return statement.executeUpdate();
        }
    }

    private static ClassroomBase mapClassroomBase(ResultSet resultSet) throws SQLException {
        return new ClassroomBase(
                resultSet.getLong("id"),
                resultSet.getLong("subject_id"),
                resultSet.getString("subject_code"),
                resultSet.getString("subject_name"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("status"),
                resultSet.getLong("teacher_user_id"),
                resultSet.getString("teacher_name"),
                JdbcSupport.getNullableInteger(resultSet, "max_students"),
                resultSet.getLong("enrolled_count"),
                resultSet.getString("meeting_url"),
                JdbcSupport.getDate(resultSet, "created_at"),
                JdbcSupport.getDate(resultSet, "updated_at"));
    }

    private static Material mapMaterial(ResultSet resultSet) throws SQLException {
        return new Material(
                resultSet.getLong("id"),
                resultSet.getLong("class_id"),
                resultSet.getString("class_title"),
                resultSet.getString("subject_code"),
                resultSet.getString("teacher_name"),
                resultSet.getLong("teacher_user_id"),
                resultSet.getString("class_status"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("material_type"),
                resultSet.getString("resource_url"),
                resultSet.getString("content_text"),
                resultSet.getString("file_name"),
                resultSet.getString("stored_name"),
                resultSet.getString("mime_type"),
                JdbcSupport.getNullableLong(resultSet, "file_size_bytes"),
                resultSet.getInt("position"),
                resultSet.getBoolean("is_published"),
                JdbcSupport.getDate(resultSet, "published_at"),
                JdbcSupport.getDate(resultSet, "created_at"),
                JdbcSupport.getDate(resultSet, "updated_at"));
    }

    private static Assessment mapAssessment(ResultSet resultSet) throws SQLException {
        String status = resultSet.getString("status");
        return new Assessment(
                resultSet.getLong("id"),
                resultSet.getLong("class_id"),
                resultSet.getString("title"),
                resultSet.getString("instructions"),
                status,
                JdbcSupport.getDate(resultSet, "available_from"),
                JdbcSupport.getDate(resultSet, "due_at"),
                JdbcSupport.getNullableInteger(resultSet, "time_limit_minutes"),
                resultSet.getInt("max_attempts"),
                "PUBLISHED".equals(status));
    }

    private record ClassroomBase(
            long id,
            long subjectId,
            String subjectCode,
            String subjectName,
            String title,
            String description,
            String status,
            long teacherUserId,
            String teacherName,
            Integer maxStudents,
            long enrolledCount,
            String meetingUrl,
            java.util.Date createdAt,
            java.util.Date updatedAt
    ) {
    }

    public record NewMaterial(
            long classId,
            Long uploadedByUserId,
            String title,
            String description,
            String materialType,
            String resourceUrl,
            String contentText,
            String fileName,
            String storedName,
            String mimeType,
            Long fileSizeBytes,
            int position,
            boolean published,
            Instant publishedAt
    ) {
    }

    public record MaterialUpdate(
            String title,
            String description,
            String materialType,
            String resourceUrl,
            String contentText,
            String fileName,
            String storedName,
            String mimeType,
            Long fileSizeBytes
    ) {
    }
}
