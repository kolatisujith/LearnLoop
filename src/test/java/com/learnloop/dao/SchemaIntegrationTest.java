package com.learnloop.dao;

import com.learnloop.support.MySqlTestDatabase;
import com.learnloop.util.InvitationTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaIntegrationTest {

    private Database database;
    private long approvedTeacherId;
    private long studentId;
    private long mathSubjectId;
    private long physicsSubjectId;

    @BeforeEach
    void setUp() {
        database = MySqlTestDatabase.database();
        approvedTeacherId = MySqlTestDatabase.queryLong(database,
                "SELECT id FROM users WHERE email = ?", "teacher@learnloop.local");
        studentId = MySqlTestDatabase.queryLong(database,
                "SELECT id FROM users WHERE email = ?", "alice.student@learnloop.local");
        mathSubjectId = MySqlTestDatabase.queryLong(database,
                "SELECT id FROM subjects WHERE code = 'MATH'");
        physicsSubjectId = MySqlTestDatabase.queryLong(database,
                "SELECT id FROM subjects WHERE code = 'PHYS'");
    }

    @Test
    void schemaContainsAllPhase2Tables() {
        long tableCount = MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
                """);

        assertEquals(15L, tableCount);
    }

    @Test
    void classesTableContainsInvitationLifecycleColumns() {
        long columnCount = MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'classes'
                  AND column_name IN (
                      'invitation_token_created_at',
                      'invitation_token_expires_at',
                      'invitation_token_revoked_at'
                  )
                  AND data_type = 'datetime'
                  AND datetime_precision = 6
                  AND is_nullable = 'YES'
                """);

        assertEquals(3L, columnCount);
    }

    @Test
    void classroomTablesContainPhaseTenColumns() {
        long meetingUrlCount = MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'classes'
                  AND column_name = 'meeting_url'
                  AND data_type = 'varchar'
                  AND character_maximum_length = 1000
                  AND is_nullable = 'YES'
                """);
        long storedNameCount = MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'class_materials'
                  AND column_name = 'stored_name'
                  AND data_type = 'varchar'
                  AND character_maximum_length = 255
                  AND is_nullable = 'YES'
                """);

        assertEquals(1L, meetingUrlCount);
        assertEquals(1L, storedNameCount);
    }

    @Test
    void usersTableContainsPhaseFiveRejectionReason() {
        long columnCount = MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'users'
                  AND column_name = 'rejection_reason'
                  AND data_type = 'varchar'
                  AND character_maximum_length = 500
                  AND is_nullable = 'YES'
                """);

        assertEquals(1L, columnCount);
    }

    @Test
    void rejectsDuplicateEmail() {
        assertConstraintViolation(() -> MySqlTestDatabase.execute(database, """
                INSERT INTO users (email, password_hash, full_name, role, approval_status)
                SELECT email, password_hash, full_name, role, approval_status
                FROM users
                WHERE email = 'teacher@learnloop.local'
                """));
    }

    @Test
    void rejectsDuplicateTeacherSubjectAssignment() {
        assertConstraintViolation(() -> MySqlTestDatabase.execute(database, """
                INSERT INTO teacher_subject_assignments (
                    teacher_user_id, subject_id, assigned_by_user_id, is_active
                )
                SELECT teacher_user_id, subject_id, assigned_by_user_id, is_active
                FROM teacher_subject_assignments
                WHERE teacher_user_id = ? AND subject_id = ?
                """, approvedTeacherId, mathSubjectId));
    }

    @Test
    void rejectsDuplicateJoinRequest() {
        assertConstraintViolation(() -> MySqlTestDatabase.execute(database, """
                INSERT INTO class_join_requests (class_id, student_user_id, status)
                SELECT class_id, student_user_id, status
                FROM class_join_requests
                ORDER BY id
                LIMIT 1
                """));
    }

    @Test
    void rejectsDuplicateEnrollment() {
        assertConstraintViolation(() -> MySqlTestDatabase.execute(database, """
                INSERT INTO class_enrollments (class_id, student_user_id, status)
                SELECT class_id, student_user_id, status
                FROM class_enrollments
                ORDER BY id
                LIMIT 1
                """));
    }

    @Test
    void rejectsClassWhenTeacherIsNotAssignedToSubject() {
        String title = "Unassigned Database Class " + UUID.randomUUID();
        String tokenHash = InvitationTokens.sha256Hex(UUID.randomUUID().toString());
        try {
            assertConstraintViolation(() -> MySqlTestDatabase.execute(database, """
                    INSERT INTO classes (
                        subject_id, teacher_user_id, title, invitation_token_hash, status
                    )
                    VALUES (?, ?, ?, ?, 'DRAFT')
                    """, physicsSubjectId, approvedTeacherId, title, tokenHash));
        } finally {
            MySqlTestDatabase.execute(database, "DELETE FROM classes WHERE title = ?", title);
        }
    }

    @Test
    void rejectsAnswerOptionBelongingToAnotherQuestion() {
        long answerId = MySqlTestDatabase.queryLong(database,
                "SELECT id FROM assessment_answers ORDER BY id LIMIT 1");
        long answerQuestionId = MySqlTestDatabase.queryLong(database,
                "SELECT question_id FROM assessment_answers WHERE id = ?", answerId);
        long optionFromAnotherQuestion = MySqlTestDatabase.queryLong(database,
                "SELECT id FROM question_options WHERE question_id <> ? ORDER BY id LIMIT 1",
                answerQuestionId);

        assertConstraintViolation(() -> MySqlTestDatabase.execute(database, """
                INSERT INTO assessment_answer_options (answer_id, option_id, question_id)
                VALUES (?, ?, ?)
                """, answerId, optionFromAnotherQuestion, answerQuestionId));
    }

    @Test
    void deletingClassCascadesThroughContentAndAssessmentData() {
        String title = "Cascade Class " + UUID.randomUUID();
        String tokenHash = InvitationTokens.sha256Hex(UUID.randomUUID().toString());

        assertThrows(RollbackSignal.class, () -> database.inTransaction(connection -> {
            long classId = insertReturningId(connection, """
                    INSERT INTO classes (
                        subject_id, teacher_user_id, title, invitation_token_hash, status
                    )
                    VALUES (?, ?, ?, ?, 'DRAFT')
                    """, mathSubjectId, approvedTeacherId, title, tokenHash);

            insertReturningId(connection, """
                    INSERT INTO class_materials (
                        class_id, uploaded_by_user_id, title, material_type, content_text
                    )
                    VALUES (?, ?, 'Material', 'TEXT', 'Material body')
                    """, classId, approvedTeacherId);

            long joinRequestId = insertReturningId(connection, """
                    INSERT INTO class_join_requests (class_id, student_user_id, status)
                    VALUES (?, ?, 'PENDING')
                    """, classId, studentId);

            insertReturningId(connection, """
                    INSERT INTO class_enrollments (
                        class_id, student_user_id, join_request_id, status
                    )
                    VALUES (?, ?, ?, 'ACTIVE')
                    """, classId, studentId, joinRequestId);

            long assessmentId = insertReturningId(connection, """
                    INSERT INTO assessments (class_id, created_by_user_id, title, status, max_attempts)
                    VALUES (?, ?, 'Cascade Assessment', 'DRAFT', 1)
                    """, classId, approvedTeacherId);

            long questionId = insertReturningId(connection, """
                    INSERT INTO questions (assessment_id, question_type, prompt, points)
                    VALUES (?, 'SINGLE_CHOICE', 'Pick one', 1.00)
                    """, assessmentId);

            long correctOptionId = insertReturningId(connection, """
                    INSERT INTO question_options (question_id, option_text, is_correct, position)
                    VALUES (?, 'Correct', TRUE, 1)
                    """, questionId);

            long attemptId = insertReturningId(connection, """
                    INSERT INTO assessment_attempts (
                        assessment_id, student_user_id, attempt_number, status
                    )
                    VALUES (?, ?, 1, 'IN_PROGRESS')
                    """, assessmentId, studentId);

            long answerId = insertReturningId(connection, """
                    INSERT INTO assessment_answers (attempt_id, question_id, answer_text)
                    VALUES (?, ?, 'Correct')
                    """, attemptId, questionId);

            executeUpdate(connection, """
                    INSERT INTO assessment_answer_options (answer_id, option_id, question_id)
                    VALUES (?, ?, ?)
                    """, answerId, correctOptionId, questionId);

            executeUpdate(connection, "DELETE FROM classes WHERE id = ?", classId);

            assertEquals(0L, count(connection, "SELECT COUNT(*) FROM classes WHERE id = ?", classId));
            assertEquals(0L, count(connection,
                    "SELECT COUNT(*) FROM class_materials WHERE class_id = ?", classId));
            assertEquals(0L, count(connection,
                    "SELECT COUNT(*) FROM class_join_requests WHERE class_id = ?", classId));
            assertEquals(0L, count(connection,
                    "SELECT COUNT(*) FROM class_enrollments WHERE class_id = ?", classId));
            assertEquals(0L, count(connection,
                    "SELECT COUNT(*) FROM assessments WHERE class_id = ?", classId));
            assertEquals(0L, count(connection,
                    "SELECT COUNT(*) FROM questions WHERE assessment_id = ?", assessmentId));
            assertEquals(0L, count(connection,
                    "SELECT COUNT(*) FROM question_options WHERE question_id = ?", questionId));
            assertEquals(0L, count(connection,
                    "SELECT COUNT(*) FROM assessment_attempts WHERE assessment_id = ?", assessmentId));
            assertEquals(0L, count(connection,
                    "SELECT COUNT(*) FROM assessment_answers WHERE attempt_id = ?", attemptId));
            assertEquals(0L, count(connection,
                    "SELECT COUNT(*) FROM assessment_answer_options WHERE answer_id = ?", answerId));

            throw new RollbackSignal();
        }));
    }

    private static void assertConstraintViolation(Executable executable) {
        DataAccessException exception = assertThrows(DataAccessException.class, executable);
        assertNotNull(exception.getCause());
    }

    private static long insertReturningId(Connection connection, String sql, Object... parameters)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, parameters);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated key returned for: " + sql);
                }
                return keys.getLong(1);
            }
        }
    }

    private static void executeUpdate(Connection connection, String sql, Object... parameters)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            statement.executeUpdate();
        }
    }

    private static long count(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static void bind(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int index = 0; index < parameters.length; index++) {
            statement.setObject(index + 1, parameters[index]);
        }
    }

    private static final class RollbackSignal extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }
}
