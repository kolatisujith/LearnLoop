package com.learnloop.dao;

import com.learnloop.service.AssessmentViews.AssessmentSummary;
import com.learnloop.service.AssessmentViews.Option;
import com.learnloop.service.AssessmentViews.Question;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class AssessmentDao {

    private static final String ASSESSMENT_SELECT = """
            SELECT
                a.id,
                a.class_id,
                c.teacher_user_id,
                c.title AS class_title,
                c.status AS class_status,
                s.code AS subject_code,
                u.full_name AS teacher_name,
                a.title,
                a.instructions,
                a.status,
                a.available_from,
                a.due_at,
                a.time_limit_minutes,
                a.max_attempts,
                a.passing_score_percent,
                a.reveal_correct_answers,
                a.created_at,
                a.updated_at,
                (SELECT COUNT(*) FROM questions q WHERE q.assessment_id = a.id) AS question_count,
                (SELECT COALESCE(SUM(q.points), 0) FROM questions q
                 WHERE q.assessment_id = a.id) AS total_points
            FROM assessments a
            JOIN classes c ON c.id = a.class_id
            JOIN subjects s ON s.id = c.subject_id
            JOIN users u ON u.id = c.teacher_user_id
            """;

    private static final String ATTEMPT_SELECT = """
            SELECT
                aa.id,
                aa.assessment_id,
                aa.student_user_id,
                u.full_name AS student_name,
                u.email AS student_email,
                aa.attempt_number,
                aa.status,
                aa.started_at,
                aa.submitted_at,
                aa.graded_at,
                aa.score
            FROM assessment_attempts aa
            JOIN users u ON u.id = aa.student_user_id
            """;

    public Optional<AssessmentSummary> findAssessment(Connection connection, long assessmentId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = ASSESSMENT_SELECT + "WHERE a.id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapAssessment(resultSet)) : Optional.empty();
            }
        }
    }

    public boolean lockAssessment(Connection connection, long assessmentId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = "SELECT id FROM assessments WHERE id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public List<Question> findQuestions(Connection connection, long assessmentId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT id, assessment_id, question_type, prompt, points, position, is_required
                FROM questions
                WHERE assessment_id = ?
                ORDER BY position ASC, id ASC
                """;
        List<Question> questions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long questionId = resultSet.getLong("id");
                    questions.add(new Question(
                            questionId,
                            resultSet.getLong("assessment_id"),
                            resultSet.getString("question_type"),
                            resultSet.getString("prompt"),
                            resultSet.getBigDecimal("points"),
                            resultSet.getInt("position"),
                            resultSet.getBoolean("is_required"),
                            findOptions(connection, questionId)));
                }
            }
        }
        return List.copyOf(questions);
    }

    public List<AttemptRecord> findAttempts(
            Connection connection, long assessmentId, Long studentUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = ATTEMPT_SELECT + """
                WHERE aa.assessment_id = ?
                """ + (studentUserId == null ? "" : "  AND aa.student_user_id = ?\n")
                + "ORDER BY aa.started_at DESC, aa.id DESC";
        List<AttemptRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            if (studentUserId != null) {
                statement.setLong(2, studentUserId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapAttemptRow(resultSet));
                }
            }
        }
        List<AttemptRecord> attempts = new ArrayList<>();
        for (AttemptRow row : rows) {
            attempts.add(toAttemptRecord(connection, row));
        }
        return List.copyOf(attempts);
    }

    public Optional<AttemptRecord> findAttempt(Connection connection, long attemptId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        AttemptRow row = findAttemptRow(connection, attemptId, false);
        return row == null ? Optional.empty() : Optional.of(toAttemptRecord(connection, row));
    }

    public Optional<AttemptRecord> findAttemptForUpdate(Connection connection, long attemptId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        AttemptRow row = findAttemptRow(connection, attemptId, true);
        return row == null ? Optional.empty() : Optional.of(toAttemptRecord(connection, row));
    }

    public Optional<AttemptRecord> findInProgressAttemptForUpdate(
            Connection connection, long assessmentId, long studentUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = ATTEMPT_SELECT + """
                WHERE aa.assessment_id = ?
                  AND aa.student_user_id = ?
                  AND aa.status = 'IN_PROGRESS'
                ORDER BY aa.attempt_number DESC
                LIMIT 1
                FOR UPDATE
                """;
        AttemptRow row = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    row = mapAttemptRow(resultSet);
                }
            }
        }
        return row == null ? Optional.empty() : Optional.of(toAttemptRecord(connection, row));
    }

    public long countAttempts(Connection connection, long assessmentId, long studentUserId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT COUNT(*)
                FROM assessment_attempts
                WHERE assessment_id = ? AND student_user_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0;
            }
        }
    }

    public long insertAssessment(Connection connection, NewAssessment assessment) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(assessment, "assessment");
        String sql = """
                INSERT INTO assessments (
                    class_id, created_by_user_id, title, instructions, status,
                    available_from, due_at, time_limit_minutes, max_attempts,
                    passing_score_percent, reveal_correct_answers
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, assessment.classId());
            JdbcSupport.setNullableLong(statement, 2, assessment.createdByUserId());
            statement.setString(3, assessment.title());
            statement.setString(4, assessment.instructions());
            statement.setString(5, assessment.status());
            JdbcSupport.setInstant(statement, 6, assessment.availableFrom());
            JdbcSupport.setInstant(statement, 7, assessment.dueAt());
            JdbcSupport.setNullableInteger(statement, 8, assessment.timeLimitMinutes());
            statement.setInt(9, assessment.maxAttempts());
            setNullableDecimal(statement, 10, assessment.passingScorePercent());
            statement.setBoolean(11, assessment.revealCorrectAnswers());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("The database did not return a generated assessment id.");
    }

    public int updateAssessment(Connection connection, long assessmentId, AssessmentUpdate update)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(update, "update");
        String sql = """
                UPDATE assessments
                SET title = ?, instructions = ?, available_from = ?, due_at = ?,
                    time_limit_minutes = ?, max_attempts = ?, passing_score_percent = ?,
                    reveal_correct_answers = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, update.title());
            statement.setString(2, update.instructions());
            JdbcSupport.setInstant(statement, 3, update.availableFrom());
            JdbcSupport.setInstant(statement, 4, update.dueAt());
            JdbcSupport.setNullableInteger(statement, 5, update.timeLimitMinutes());
            statement.setInt(6, update.maxAttempts());
            setNullableDecimal(statement, 7, update.passingScorePercent());
            statement.setBoolean(8, update.revealCorrectAnswers());
            statement.setLong(9, assessmentId);
            return statement.executeUpdate();
        }
    }

    public int updateAssessmentStatus(Connection connection, long assessmentId, String status)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = "UPDATE assessments SET status = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, assessmentId);
            return statement.executeUpdate();
        }
    }

    public int expireOpenAttempts(Connection connection, long assessmentId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE assessment_attempts
                SET status = 'EXPIRED'
                WHERE assessment_id = ? AND status = 'IN_PROGRESS'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            return statement.executeUpdate();
        }
    }

    public int deleteAssessment(Connection connection, long assessmentId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = "DELETE FROM assessments WHERE id = ? AND status = 'DRAFT'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            return statement.executeUpdate();
        }
    }

    public int nextQuestionPosition(Connection connection, long assessmentId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                SELECT COALESCE(MAX(position), 0) + 1
                FROM questions
                WHERE assessment_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 1;
            }
        }
    }

    public long insertQuestion(Connection connection, long assessmentId, NewQuestion question)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(question, "question");
        String sql = """
                INSERT INTO questions (
                    assessment_id, question_type, prompt, points, position, is_required
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, assessmentId);
            statement.setString(2, question.type());
            statement.setString(3, question.prompt());
            statement.setBigDecimal(4, question.points());
            statement.setInt(5, question.position());
            statement.setBoolean(6, question.required());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("The database did not return a generated question id.");
    }

    public void insertOption(
            Connection connection,
            long questionId,
            String text,
            boolean correct,
            int position) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                INSERT INTO question_options (question_id, option_text, is_correct, position)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, questionId);
            statement.setString(2, text);
            statement.setBoolean(3, correct);
            statement.setInt(4, position);
            statement.executeUpdate();
        }
    }

    public int deleteQuestion(Connection connection, long assessmentId, long questionId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = "DELETE FROM questions WHERE id = ? AND assessment_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, questionId);
            statement.setLong(2, assessmentId);
            return statement.executeUpdate();
        }
    }

    public long insertAttempt(
            Connection connection,
            long assessmentId,
            long studentUserId,
            int attemptNumber,
            Instant startedAt) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                INSERT INTO assessment_attempts (
                    assessment_id, student_user_id, attempt_number, status, started_at
                )
                VALUES (?, ?, ?, 'IN_PROGRESS', ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, assessmentId);
            statement.setLong(2, studentUserId);
            statement.setInt(3, attemptNumber);
            JdbcSupport.setInstant(statement, 4, startedAt);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("The database did not return a generated attempt id.");
    }

    public int expireAttempt(Connection connection, long attemptId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE assessment_attempts
                SET status = 'EXPIRED'
                WHERE id = ? AND status = 'IN_PROGRESS'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attemptId);
            return statement.executeUpdate();
        }
    }

    public long insertAnswer(
            Connection connection,
            long attemptId,
            long questionId,
            String answerText,
            Boolean correct,
            BigDecimal pointsAwarded) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                INSERT INTO assessment_answers (
                    attempt_id, question_id, answer_text, is_correct, points_awarded
                )
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, attemptId);
            statement.setLong(2, questionId);
            statement.setString(3, answerText);
            setNullableBoolean(statement, 4, correct);
            setNullableDecimal(statement, 5, pointsAwarded);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("The database did not return a generated answer id.");
    }

    public void insertAnswerOption(
            Connection connection, long answerId, long questionId, long optionId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                INSERT INTO assessment_answer_options (answer_id, option_id, question_id)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, answerId);
            statement.setLong(2, optionId);
            statement.setLong(3, questionId);
            statement.executeUpdate();
        }
    }

    public int updateAnswerGrade(
            Connection connection,
            long answerId,
            long attemptId,
            Boolean correct,
            BigDecimal pointsAwarded) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE assessment_answers
                SET is_correct = ?, points_awarded = ?
                WHERE id = ? AND attempt_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableBoolean(statement, 1, correct);
            setNullableDecimal(statement, 2, pointsAwarded);
            statement.setLong(3, answerId);
            statement.setLong(4, attemptId);
            return statement.executeUpdate();
        }
    }

    public int completeAttempt(
            Connection connection,
            long attemptId,
            String status,
            Instant submittedAt,
            Instant gradedAt,
            BigDecimal score,
            Long gradedByUserId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String sql = """
                UPDATE assessment_attempts
                SET status = ?,
                    submitted_at = COALESCE(?, submitted_at),
                    graded_at = ?,
                    score = ?,
                    graded_by_user_id = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            JdbcSupport.setInstant(statement, 2, submittedAt);
            JdbcSupport.setInstant(statement, 3, gradedAt);
            setNullableDecimal(statement, 4, score);
            JdbcSupport.setNullableLong(statement, 5, gradedByUserId);
            statement.setLong(6, attemptId);
            return statement.executeUpdate();
        }
    }

    private AttemptRow findAttemptRow(Connection connection, long attemptId, boolean forUpdate)
            throws SQLException {
        String sql = ATTEMPT_SELECT + "WHERE aa.id = ?" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attemptId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapAttemptRow(resultSet) : null;
            }
        }
    }

    private List<Option> findOptions(Connection connection, long questionId) throws SQLException {
        String sql = """
                SELECT id, option_text, is_correct, position
                FROM question_options
                WHERE question_id = ?
                ORDER BY position ASC, id ASC
                """;
        List<Option> options = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, questionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    options.add(new Option(
                            resultSet.getLong("id"),
                            resultSet.getString("option_text"),
                            resultSet.getBoolean("is_correct"),
                            resultSet.getInt("position")));
                }
            }
        }
        return List.copyOf(options);
    }

    private AttemptRecord toAttemptRecord(Connection connection, AttemptRow row) throws SQLException {
        return new AttemptRecord(
                row.id(),
                row.assessmentId(),
                row.studentUserId(),
                row.studentName(),
                row.studentEmail(),
                row.attemptNumber(),
                row.status(),
                row.startedAt(),
                row.submittedAt(),
                row.gradedAt(),
                row.score(),
                findAnswers(connection, row.id()));
    }

    private List<AnswerRecord> findAnswers(Connection connection, long attemptId) throws SQLException {
        String sql = """
                SELECT id, question_id, answer_text, is_correct, points_awarded
                FROM assessment_answers
                WHERE attempt_id = ?
                ORDER BY id ASC
                """;
        List<AnswerRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attemptId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new AnswerRow(
                            resultSet.getLong("id"),
                            resultSet.getLong("question_id"),
                            resultSet.getString("answer_text"),
                            nullableBoolean(resultSet, "is_correct"),
                            resultSet.getBigDecimal("points_awarded")));
                }
            }
        }
        List<AnswerRecord> answers = new ArrayList<>();
        for (AnswerRow row : rows) {
            answers.add(new AnswerRecord(
                    row.id(),
                    row.questionId(),
                    row.answerText(),
                    row.correct(),
                    row.pointsAwarded(),
                    findSelectedOptionIds(connection, row.id())));
        }
        return List.copyOf(answers);
    }

    private Set<Long> findSelectedOptionIds(Connection connection, long answerId) throws SQLException {
        String sql = """
                SELECT option_id
                FROM assessment_answer_options
                WHERE answer_id = ?
                ORDER BY option_id
                """;
        Set<Long> optionIds = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, answerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    optionIds.add(resultSet.getLong(1));
                }
            }
        }
        return Set.copyOf(optionIds);
    }

    private static AssessmentSummary mapAssessment(ResultSet resultSet) throws SQLException {
        Date availableFrom = JdbcSupport.getDate(resultSet, "available_from");
        Date dueAt = JdbcSupport.getDate(resultSet, "due_at");
        String status = resultSet.getString("status");
        Timestamp now = new Timestamp(System.currentTimeMillis());
        boolean open = "PUBLISHED".equals(status)
                && (availableFrom == null || !availableFrom.after(now))
                && (dueAt == null || !dueAt.before(now));
        return new AssessmentSummary(
                resultSet.getLong("id"),
                resultSet.getLong("class_id"),
                resultSet.getLong("teacher_user_id"),
                resultSet.getString("class_title"),
                resultSet.getString("class_status"),
                resultSet.getString("subject_code"),
                resultSet.getString("teacher_name"),
                resultSet.getString("title"),
                resultSet.getString("instructions"),
                status,
                availableFrom,
                dueAt,
                JdbcSupport.getNullableInteger(resultSet, "time_limit_minutes"),
                resultSet.getInt("max_attempts"),
                resultSet.getBigDecimal("passing_score_percent"),
                resultSet.getBoolean("reveal_correct_answers"),
                resultSet.getLong("question_count"),
                resultSet.getBigDecimal("total_points"),
                JdbcSupport.getDate(resultSet, "created_at"),
                JdbcSupport.getDate(resultSet, "updated_at"),
                open);
    }

    private static AttemptRow mapAttemptRow(ResultSet resultSet) throws SQLException {
        return new AttemptRow(
                resultSet.getLong("id"),
                resultSet.getLong("assessment_id"),
                resultSet.getLong("student_user_id"),
                resultSet.getString("student_name"),
                resultSet.getString("student_email"),
                resultSet.getInt("attempt_number"),
                resultSet.getString("status"),
                JdbcSupport.getDate(resultSet, "started_at"),
                JdbcSupport.getDate(resultSet, "submitted_at"),
                JdbcSupport.getDate(resultSet, "graded_at"),
                resultSet.getBigDecimal("score"));
    }

    private static Boolean nullableBoolean(ResultSet resultSet, String column) throws SQLException {
        boolean value = resultSet.getBoolean(column);
        return resultSet.wasNull() ? null : value;
    }

    private static void setNullableBoolean(PreparedStatement statement, int index, Boolean value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BOOLEAN);
        } else {
            statement.setBoolean(index, value);
        }
    }

    private static void setNullableDecimal(PreparedStatement statement, int index, BigDecimal value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DECIMAL);
        } else {
            statement.setBigDecimal(index, value);
        }
    }

    public record NewAssessment(
            long classId,
            Long createdByUserId,
            String title,
            String instructions,
            String status,
            Instant availableFrom,
            Instant dueAt,
            Integer timeLimitMinutes,
            int maxAttempts,
            BigDecimal passingScorePercent,
            boolean revealCorrectAnswers
    ) {
    }

    public record AssessmentUpdate(
            String title,
            String instructions,
            Instant availableFrom,
            Instant dueAt,
            Integer timeLimitMinutes,
            int maxAttempts,
            BigDecimal passingScorePercent,
            boolean revealCorrectAnswers
    ) {
    }

    public record NewQuestion(
            String type,
            String prompt,
            BigDecimal points,
            int position,
            boolean required
    ) {
    }

    public record AttemptRecord(
            long id,
            long assessmentId,
            long studentUserId,
            String studentName,
            String studentEmail,
            int attemptNumber,
            String status,
            Date startedAt,
            Date submittedAt,
            Date gradedAt,
            BigDecimal score,
            List<AnswerRecord> answers
    ) {

        public AttemptRecord {
            answers = List.copyOf(answers);
        }
    }

    public record AnswerRecord(
            long id,
            long questionId,
            String answerText,
            Boolean correct,
            BigDecimal pointsAwarded,
            Set<Long> selectedOptionIds
    ) {

        public AnswerRecord {
            selectedOptionIds = Set.copyOf(selectedOptionIds);
        }
    }

    private record AttemptRow(
            long id,
            long assessmentId,
            long studentUserId,
            String studentName,
            String studentEmail,
            int attemptNumber,
            String status,
            Date startedAt,
            Date submittedAt,
            Date gradedAt,
            BigDecimal score
    ) {
    }

    private record AnswerRow(
            long id,
            long questionId,
            String answerText,
            Boolean correct,
            BigDecimal pointsAwarded
    ) {
    }
}
