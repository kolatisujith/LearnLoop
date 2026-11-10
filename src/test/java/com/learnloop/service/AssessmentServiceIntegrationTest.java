package com.learnloop.service;

import com.learnloop.dao.Database;
import com.learnloop.dao.TeacherSubjectAssignmentDao;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.LearningClass;
import com.learnloop.model.NewUser;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;
import com.learnloop.service.AssessmentViews.Answer;
import com.learnloop.service.AssessmentViews.AssessmentDetail;
import com.learnloop.service.AssessmentViews.AttemptDetail;
import com.learnloop.service.AssessmentViews.AttemptPage;
import com.learnloop.service.AssessmentViews.Question;
import com.learnloop.service.AssessmentViews.StudentAssessmentDetail;
import com.learnloop.support.MySqlTestDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentServiceIntegrationTest {

    private static final ClientContext CLIENT = new ClientContext("127.0.0.1", "JUnit");
    private static final DateTimeFormatter INPUT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private Database database;
    private ClassService classService;
    private AssessmentService assessmentService;
    private final Set<Long> classIds = new LinkedHashSet<>();
    private final Set<Long> userIds = new LinkedHashSet<>();
    private long teacherId;
    private long otherTeacherId;
    private long studentId;
    private long otherStudentId;
    private long mathSubjectId;

    @BeforeEach
    void setUp() {
        database = MySqlTestDatabase.database();
        classService = new ClassService(database);
        assessmentService = new AssessmentService(database);
        mathSubjectId = MySqlTestDatabase.queryLong(
                database, "SELECT id FROM subjects WHERE code = ?", "MATH");
        teacherId = createUser(UserRole.TEACHER, ApprovalStatus.APPROVED);
        otherTeacherId = createUser(UserRole.TEACHER, ApprovalStatus.APPROVED);
        studentId = createUser(UserRole.STUDENT, ApprovalStatus.APPROVED);
        otherStudentId = createUser(UserRole.STUDENT, ApprovalStatus.APPROVED);
        assign(teacherId);
        assign(otherTeacherId);
    }

    @AfterEach
    void cleanUp() {
        for (long classId : classIds) {
            MySqlTestDatabase.deleteClass(database, classId);
        }
        for (long userId : userIds) {
            MySqlTestDatabase.execute(database, "DELETE FROM audit_logs WHERE actor_user_id = ?", userId);
            MySqlTestDatabase.execute(database,
                    "DELETE FROM teacher_subject_assignments WHERE teacher_user_id = ?", userId);
            MySqlTestDatabase.execute(database, "DELETE FROM users WHERE id = ?", userId);
        }
    }

    @Test
    void teacherBuildsAndPublishesAssessmentAndStudentAutoGradedAttemptIsPrivate() {
        long classId = createActiveClass(teacherId);
        enrol(classId, studentId);
        long assessmentId = createAssessment(
                teacherId, classId, "Auto graded quiz", "1", null, true, null, null, "50");
        long questionId = addSingleChoice(
                teacherId, assessmentId, "What is 2 + 2?", "2.00", List.of("3", "4", "5"), List.of("2"));

        assessmentService.publishAssessment(teacherId, assessmentId, CLIENT);

        AssessmentDetail teacherDetail = assessmentService.teacherAssessment(teacherId, assessmentId);
        assertEquals("PUBLISHED", teacherDetail.getAssessment().getStatus());
        assertEquals(1, teacherDetail.getQuestions().size());
        assertEquals(2, teacherDetail.getAssessment().getTotalPoints().intValue());
        assertThrows(AssessmentAccessException.class,
                () -> assessmentService.teacherAssessment(otherTeacherId, assessmentId));

        StudentAssessmentDetail studentDetail = assessmentService.studentAssessment(studentId, assessmentId);
        assertTrue(studentDetail.isCanStart());
        assertEquals(1, studentDetail.getRemainingAttempts());
        assertThrows(AssessmentAccessException.class,
                () -> assessmentService.studentAssessment(otherStudentId, assessmentId));

        long attemptId = assessmentService.startAttempt(studentId, assessmentId, CLIENT);
        assertEquals(attemptId, assessmentService.startAttempt(studentId, assessmentId, CLIENT));

        Question question = teacherDetail.getQuestions().get(0);
        long correctOptionId = question.getOptions().get(1).getId();
        assessmentService.submitAttempt(
                studentId,
                attemptId,
                Map.of(),
                Map.of(questionId, Set.of(correctOptionId)),
                CLIENT);

        AttemptPage page = assessmentService.studentAttempt(studentId, attemptId);
        assertTrue(page.isReview());
        assertTrue(page.isRevealCorrectAnswers());
        assertTrue(page.getAttempt().isGraded());
        assertEquals(new BigDecimal("2.00"), page.getAttempt().getScore());
        assertEquals(new BigDecimal("100.00"), page.getAttempt().getPercentage());
        assertEquals(Boolean.TRUE, page.getAttempt().getPassed());
        assertTrue(page.getAttempt().getAnswers().get(0).isCorrectAnswer());
        assertTrue(page.getAttempt().getAnswers().get(0).getOptions().stream()
                .anyMatch(option -> option.isCorrect() && option.isSelected()));

        StudentAssessmentDetail after = assessmentService.studentAssessment(studentId, assessmentId);
        assertFalse(after.isCanStart());
        assertEquals(1, after.getAttemptsUsed());
        assertEquals(0, after.getRemainingAttempts());
        assertThrows(ValidationException.class,
                () -> assessmentService.startAttempt(studentId, assessmentId, CLIENT));

        AssessmentDetail graded = assessmentService.teacherAssessment(teacherId, assessmentId);
        assertEquals(1, graded.getAttempts().size());
        assertEquals(new BigDecimal("2.00"), graded.getAttempts().get(0).getScore());
        assertEquals(0, graded.getAwaitingGradingCount());
        assertEquals(1, auditCount("ASSESSMENT_PUBLISHED", "ASSESSMENT", assessmentId));
        assertEquals(1, auditCount("ASSESSMENT_ATTEMPT_GRADED", "ASSESSMENT_ATTEMPT", attemptId));
    }

    @Test
    void shortTextAnswersRequireManualGradingAndRespectRevealSetting() {
        long classId = createActiveClass(teacherId);
        enrol(classId, studentId);
        long assessmentId = createAssessment(
                teacherId, classId, "Mixed assessment", "2", null, false, null, null, "60");
        long choiceId = addSingleChoice(
                teacherId, assessmentId, "Pick A", "2.00", List.of("A", "B"), List.of("1"));
        long shortTextId = addShortText(
                teacherId, assessmentId, "Explain your answer", "4.00", true);
        assessmentService.publishAssessment(teacherId, assessmentId, CLIENT);

        AssessmentDetail teacherDetail = assessmentService.teacherAssessment(teacherId, assessmentId);
        long choiceOptionId = teacherDetail.getQuestions().stream()
                .filter(question -> question.getId() == choiceId)
                .findFirst()
                .orElseThrow()
                .getOptions()
                .get(0)
                .getId();

        long attemptId = assessmentService.startAttempt(studentId, assessmentId, CLIENT);
        assessmentService.submitAttempt(
                studentId,
                attemptId,
                Map.of(shortTextId, "Because it is a test response."),
                Map.of(choiceId, Set.of(choiceOptionId)),
                CLIENT);

        AttemptPage awaiting = assessmentService.studentAttempt(studentId, attemptId);
        assertTrue(awaiting.isReview());
        assertTrue(awaiting.getAttempt().isAwaitingGrading());
        assertNull(awaiting.getAttempt().getScore());
        assertFalse(awaiting.isRevealCorrectAnswers());

        AssessmentDetail submitted = assessmentService.teacherAssessment(teacherId, assessmentId);
        AttemptDetail attempt = submitted.getAttempts().get(0);
        assertEquals(1, attempt.getShortTextAnswers().size());
        Answer shortAnswer = attempt.getShortTextAnswers().get(0);
        assertEquals("Because it is a test response.", shortAnswer.getAnswerText());

        assertThrows(ValidationException.class, () -> assessmentService.gradeAttempt(
                teacherId, assessmentId, attemptId,
                Map.of(shortAnswer.getId(), new BigDecimal("5.00")), CLIENT));
        assertEquals("SUBMITTED",
                MySqlTestDatabase.queryString(database,
                        "SELECT status FROM assessment_attempts WHERE id = ?", attemptId));

        assessmentService.gradeAttempt(
                teacherId,
                assessmentId,
                attemptId,
                Map.of(shortAnswer.getId(), new BigDecimal("2.00")),
                CLIENT);

        AttemptPage graded = assessmentService.studentAttempt(studentId, attemptId);
        assertTrue(graded.getAttempt().isGraded());
        assertFalse(graded.isRevealCorrectAnswers());
        assertEquals(new BigDecimal("4.00"), graded.getAttempt().getScore());
        assertEquals(new BigDecimal("66.67"), graded.getAttempt().getPercentage());
        assertEquals(Boolean.TRUE, graded.getAttempt().getPassed());
        assertTrue(graded.getAttempt().getAnswers().stream()
                .flatMap(answer -> answer.getOptions().stream())
                .noneMatch(AssessmentViews.AnswerOption::isCorrect));

        AssessmentDetail gradedTeacher = assessmentService.teacherAssessment(teacherId, assessmentId);
        Answer gradedAnswer = gradedTeacher.getAttempts().get(0).getShortTextAnswers().get(0);
        assertEquals(new BigDecimal("2.00"), gradedAnswer.getPointsAwarded());
        assertEquals(1, auditCount("ASSESSMENT_ATTEMPT_MANUALLY_GRADED", "ASSESSMENT_ATTEMPT", attemptId));
        assertThrows(ValidationException.class, () -> assessmentService.gradeAttempt(
                teacherId, assessmentId, attemptId,
                Map.of(shortAnswer.getId(), new BigDecimal("2.00")), CLIENT));
    }

    @Test
    void expiredAttemptIsCommittedAndCloseExpiresRemainingOpenAttempts() {
        long classId = createActiveClass(teacherId);
        enrol(classId, studentId);
        long assessmentId = createAssessment(
                teacherId, classId, "Timed assessment", "2", 1, true, null, null, null);
        addSingleChoice(teacherId, assessmentId, "Pick one", "1.00", List.of("A", "B"), List.of("1"));
        assessmentService.publishAssessment(teacherId, assessmentId, CLIENT);

        long expiredAttemptId = assessmentService.startAttempt(studentId, assessmentId, CLIENT);
        MySqlTestDatabase.execute(database, """
                UPDATE assessment_attempts
                SET started_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 MINUTE)
                WHERE id = ?
                """, expiredAttemptId);

        assertThrows(ValidationException.class, () -> assessmentService.submitAttempt(
                studentId, expiredAttemptId, Map.of(), Map.of(), CLIENT));
        assertEquals("EXPIRED",
                MySqlTestDatabase.queryString(database,
                        "SELECT status FROM assessment_attempts WHERE id = ?", expiredAttemptId));
        assertTrue(assessmentService.studentAttempt(studentId, expiredAttemptId).getAttempt().isExpired());

        long openAttemptId = assessmentService.startAttempt(studentId, assessmentId, CLIENT);
        assessmentService.closeAssessment(teacherId, assessmentId, CLIENT);
        assertEquals("CLOSED",
                MySqlTestDatabase.queryString(database,
                        "SELECT status FROM assessments WHERE id = ?", assessmentId));
        assertEquals("EXPIRED",
                MySqlTestDatabase.queryString(database,
                        "SELECT status FROM assessment_attempts WHERE id = ?", openAttemptId));
        assertThrows(ValidationException.class,
                () -> assessmentService.startAttempt(studentId, assessmentId, CLIENT));

        StudentAssessmentDetail closed = assessmentService.studentAssessment(studentId, assessmentId);
        assertEquals("CLOSED", closed.getAssessment().getStatus());
        assertFalse(closed.isCanStart());
        assertEquals(1, auditCount("ASSESSMENT_CLOSED", "ASSESSMENT", assessmentId));
    }

    @Test
    void futureAvailabilityPreventsAttemptsAndPastDueDateCannotBePublished() {
        long classId = createActiveClass(teacherId);
        enrol(classId, studentId);
        String future = LocalDateTime.now().plusDays(1).format(INPUT);
        long assessmentId = createAssessment(
                teacherId, classId, "Future assessment", "1", null, true, future, null, null);
        addSingleChoice(teacherId, assessmentId, "Question", "1.00", List.of("A", "B"), List.of("1"));
        assessmentService.publishAssessment(teacherId, assessmentId, CLIENT);

        StudentAssessmentDetail detail = assessmentService.studentAssessment(studentId, assessmentId);
        assertFalse(detail.isCanStart());
        assertThrows(ValidationException.class,
                () -> assessmentService.startAttempt(studentId, assessmentId, CLIENT));

        long pastDueAssessment = createAssessment(
                teacherId,
                classId,
                "Past due assessment",
                "1",
                null,
                true,
                null,
                LocalDateTime.now().minusDays(1).format(INPUT),
                null);
        addSingleChoice(
                teacherId, pastDueAssessment, "Question", "1.00", List.of("A", "B"), List.of("1"));
        assertThrows(ValidationException.class,
                () -> assessmentService.publishAssessment(teacherId, pastDueAssessment, CLIENT));
    }

    @Test
    void draftValidationBlocksInvalidQuestionsAndPublishedContentIsImmutable() {
        long classId = createActiveClass(teacherId);
        assertThrows(ValidationException.class, () -> assessmentService.createAssessment(
                teacherId,
                classId,
                new AssessmentService.AssessmentForm(
                        "Bad dates", null, "0", "0", "101", null, null, true),
                CLIENT));

        long assessmentId = createAssessment(
                teacherId, classId, "Validation", "1", null, true, null, null, null);
        assertThrows(ValidationException.class, () -> assessmentService.addQuestion(
                teacherId,
                assessmentId,
                new AssessmentService.QuestionForm(
                        "SINGLE_CHOICE",
                        "Two correct answers",
                        "1.00",
                        true,
                        List.of("A", "B"),
                        List.of("1", "2"),
                        null),
                CLIENT));
        assertThrows(ValidationException.class, () -> assessmentService.addQuestion(
                teacherId,
                assessmentId,
                new AssessmentService.QuestionForm(
                        "SHORT_TEXT", "Explain", "0", true, List.of(), List.of(), null),
                CLIENT));
        assertThrows(ValidationException.class,
                () -> assessmentService.publishAssessment(teacherId, assessmentId, CLIENT));

        long questionId = addSingleChoice(
                teacherId, assessmentId, "Question", "1.00", List.of("A", "B"), List.of("1"));
        assessmentService.publishAssessment(teacherId, assessmentId, CLIENT);
        assertThrows(ValidationException.class,
                () -> assessmentService.deleteQuestion(teacherId, assessmentId, questionId, CLIENT));
        assertThrows(ValidationException.class, () -> assessmentService.updateAssessment(
                teacherId,
                assessmentId,
                new AssessmentService.AssessmentForm(
                        "Changed", null, "1", null, null, null, null, true),
                CLIENT));
        assertThrows(ValidationException.class,
                () -> assessmentService.deleteAssessment(teacherId, assessmentId, CLIENT));
        assertThrows(AssessmentAccessException.class,
                () -> assessmentService.teacherAssessment(otherTeacherId, assessmentId));
    }

    private long createAssessment(
            long teacherUserId,
            long classId,
            String title,
            String maxAttempts,
            Integer timeLimitMinutes,
            boolean revealCorrectAnswers,
            String availableFrom,
            String dueAt,
            String passingScorePercent) {
        return assessmentService.createAssessment(
                teacherUserId,
                classId,
                new AssessmentService.AssessmentForm(
                        title,
                        "Assessment instructions",
                        maxAttempts,
                        timeLimitMinutes == null ? null : timeLimitMinutes.toString(),
                        passingScorePercent,
                        availableFrom,
                        dueAt,
                        revealCorrectAnswers),
                CLIENT);
    }

    private long addSingleChoice(
            long teacherUserId,
            long assessmentId,
            String prompt,
            String points,
            List<String> options,
            List<String> correctOptions) {
        return assessmentService.addQuestion(
                teacherUserId,
                assessmentId,
                new AssessmentService.QuestionForm(
                        "SINGLE_CHOICE",
                        prompt,
                        points,
                        true,
                        options,
                        correctOptions,
                        null),
                CLIENT);
    }

    private long addShortText(
            long teacherUserId,
            long assessmentId,
            String prompt,
            String points,
            boolean required) {
        return assessmentService.addQuestion(
                teacherUserId,
                assessmentId,
                new AssessmentService.QuestionForm(
                        "SHORT_TEXT", prompt, points, required, List.of(), List.of(), null),
                CLIENT);
    }

    private long createActiveClass(long owningTeacherId) {
        LearningClass created = classService.createManagedClass(new TeacherClassCreateRequest(
                owningTeacherId,
                mathSubjectId,
                "Phase 11 class " + UUID.randomUUID(),
                "Assessment integration test class.",
                25), CLIENT);
        classIds.add(created.id());
        ClassViews.ClassDetails details = classService.findOwnedClass(owningTeacherId, created.id());
        classService.updateClass(new TeacherClassUpdateRequest(
                owningTeacherId,
                created.id(),
                details.getSubjectId(),
                details.getTitle(),
                details.getDescription(),
                details.getMaxStudents(),
                "ACTIVE"), CLIENT);
        return created.id();
    }

    private long createUser(UserRole role, ApprovalStatus approvalStatus) {
        String email = "phase11-" + UUID.randomUUID() + "@learnloop.test";
        long userId = database.inTransaction(connection -> new UserDao().insert(connection, new NewUser(
                email,
                "integration-test-hash",
                "Phase 11 " + role.name(),
                role,
                approvalStatus,
                approvalStatus == ApprovalStatus.APPROVED ? Instant.now() : null,
                null)));
        userIds.add(userId);
        return userId;
    }

    private void assign(long teacherUserId) {
        database.inTransaction(connection -> {
            new TeacherSubjectAssignmentDao().upsertActive(
                    connection, teacherUserId, mathSubjectId, teacherUserId);
            return null;
        });
    }

    private void enrol(long classId, long studentUserId) {
        MySqlTestDatabase.execute(database, """
                INSERT INTO class_enrollments (class_id, student_user_id, status)
                VALUES (?, ?, 'ACTIVE')
                """, classId, studentUserId);
    }

    private long auditCount(String action, String entityType, long entityId) {
        return MySqlTestDatabase.queryLong(database, """
                SELECT COUNT(*) FROM audit_logs
                WHERE action = ? AND entity_type = ? AND entity_id = ?
                """, action, entityType, entityId);
    }
}
