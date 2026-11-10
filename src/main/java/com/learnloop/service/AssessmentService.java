package com.learnloop.service;

import com.learnloop.dao.AssessmentDao;
import com.learnloop.dao.AssessmentDao.AnswerRecord;
import com.learnloop.dao.AssessmentDao.AssessmentUpdate;
import com.learnloop.dao.AssessmentDao.AttemptRecord;
import com.learnloop.dao.AssessmentDao.NewAssessment;
import com.learnloop.dao.AssessmentDao.NewQuestion;
import com.learnloop.dao.AuditLogDao;
import com.learnloop.dao.ClassDao;
import com.learnloop.dao.Database;
import com.learnloop.dao.EnrollmentDao;
import com.learnloop.dao.JdbcSupport;
import com.learnloop.dao.UserDao;
import com.learnloop.model.ApprovalStatus;
import com.learnloop.model.LearningClass;
import com.learnloop.model.UserAccount;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;
import com.learnloop.service.AssessmentViews.Answer;
import com.learnloop.service.AssessmentViews.AnswerOption;
import com.learnloop.service.AssessmentViews.AssessmentDetail;
import com.learnloop.service.AssessmentViews.AssessmentSummary;
import com.learnloop.service.AssessmentViews.AttemptDetail;
import com.learnloop.service.AssessmentViews.AttemptPage;
import com.learnloop.service.AssessmentViews.Option;
import com.learnloop.service.AssessmentViews.Question;
import com.learnloop.service.AssessmentViews.StudentAssessmentDetail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AssessmentService {

    private static final int MAX_TITLE_LENGTH = 160;
    private static final int MAX_INSTRUCTIONS_LENGTH = 4000;
    private static final int MAX_PROMPT_LENGTH = 4000;
    private static final int MAX_OPTION_LENGTH = 500;
    private static final int MAX_TEXT_ANSWER_LENGTH = 20_000;
    private static final int MAX_QUESTIONS = 100;
    private static final int MAX_OPTIONS = 4;
    private static final int MAX_ATTEMPTS = 20;
    private static final int MAX_TIME_LIMIT_MINUTES = 1440;
    private static final BigDecimal MAX_POINTS = new BigDecimal("9999.99");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final Set<String> QUESTION_TYPES = Set.of(
            "SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_TEXT");
    private static final Set<String> EDITABLE_CLASS_STATUSES = Set.of("DRAFT", "ACTIVE");
    private static final DateTimeFormatter DATE_TIME_INPUT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final String ASSESSMENT_ENTITY = "ASSESSMENT";
    private static final String ATTEMPT_ENTITY = "ASSESSMENT_ATTEMPT";

    private final Database database;
    private final AssessmentDao assessmentDao;
    private final ClassDao classDao;
    private final EnrollmentDao enrollmentDao;
    private final UserDao userDao;
    private final AuditLogDao auditLogDao;

    public AssessmentService(Database database) {
        this(database, new AssessmentDao(), new ClassDao(), new EnrollmentDao(),
                new UserDao(), new AuditLogDao());
    }

    public AssessmentService(
            Database database,
            AssessmentDao assessmentDao,
            ClassDao classDao,
            EnrollmentDao enrollmentDao,
            UserDao userDao,
            AuditLogDao auditLogDao) {
        this.database = Objects.requireNonNull(database, "database");
        this.assessmentDao = Objects.requireNonNull(assessmentDao, "assessmentDao");
        this.classDao = Objects.requireNonNull(classDao, "classDao");
        this.enrollmentDao = Objects.requireNonNull(enrollmentDao, "enrollmentDao");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.auditLogDao = Objects.requireNonNull(auditLogDao, "auditLogDao");
    }

    public AssessmentDetail teacherAssessment(long teacherUserId, long assessmentId) {
        requirePositive(teacherUserId, "Teacher authorization could not be verified.");
        requirePositive(assessmentId, "Assessment was not found.");
        return database.withConnection(connection -> {
            requireApprovedUser(connection, teacherUserId, UserRole.TEACHER);
            AssessmentSummary assessment = requireOwnedAssessment(connection, teacherUserId, assessmentId);
            List<Question> questions = assessmentDao.findQuestions(connection, assessmentId);
            List<AttemptDetail> attempts = assessmentDao.findAttempts(connection, assessmentId, null)
                    .stream()
                    .map(attempt -> toAttemptDetail(attempt, questions, assessment, true))
                    .toList();
            return new AssessmentDetail(assessment, questions, attempts);
        });
    }

    public StudentAssessmentDetail studentAssessment(long studentUserId, long assessmentId) {
        requirePositive(studentUserId, "Student authorization could not be verified.");
        requirePositive(assessmentId, "Assessment was not found.");
        return database.inTransaction(connection -> {
            requireApprovedUser(connection, studentUserId, UserRole.STUDENT);
            AssessmentSummary assessment = requireStudentAssessment(connection, studentUserId, assessmentId);
            List<Question> questions = assessmentDao.findQuestions(connection, assessmentId);
            List<AttemptRecord> records = assessmentDao.findAttempts(connection, assessmentId, studentUserId);
            AttemptRecord inProgress = records.stream()
                    .filter(attempt -> "IN_PROGRESS".equals(attempt.status()))
                    .findFirst()
                    .orElse(null);
            if (inProgress != null && isAttemptExpired(inProgress, assessment)) {
                assessmentDao.expireAttempt(connection, inProgress.id());
                records = assessmentDao.findAttempts(connection, assessmentId, studentUserId);
                inProgress = null;
            }
            AttemptDetail inProgressView = inProgress == null
                    ? null
                    : toAttemptDetail(inProgress, questions, assessment, false);
            List<AttemptDetail> attempts = records.stream()
                    .map(attempt -> toAttemptDetail(
                            attempt,
                            questions,
                            assessment,
                            "GRADED".equals(attempt.status()) && assessment.revealCorrectAnswers()))
                    .toList();
            boolean canStart = assessment.isOpen()
                    && "PUBLISHED".equals(assessment.status())
                    && inProgressView == null
                    && records.size() < assessment.maxAttempts();
            return new StudentAssessmentDetail(assessment, inProgressView, attempts, canStart);
        });
    }

    public AttemptPage studentAttempt(long studentUserId, long attemptId) {
        return loadAttemptPage(studentUserId, attemptId, Map.of(), Map.of(), false);
    }

    public AttemptPage studentAttempt(
            long studentUserId,
            long attemptId,
            Map<Long, String> textAnswers,
            Map<Long, Set<Long>> selectedOptions) {
        return loadAttemptPage(studentUserId, attemptId, textAnswers, selectedOptions, true);
    }

    public long createAssessment(
            long teacherUserId,
            long classId,
            AssessmentForm form,
            ClientContext clientContext) {
        requirePositive(teacherUserId, "Teacher authorization could not be verified.");
        requirePositive(classId, "Class was not found.");
        Objects.requireNonNull(form, "form");
        ValidatedAssessment values = validateAssessmentForm(form);
        return database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            LearningClass learningClass = classDao.findByIdAndTeacherForUpdate(connection, classId, teacherUserId)
                    .orElseThrow(() -> new AssessmentAccessException("Class was not found."));
            if (!EDITABLE_CLASS_STATUSES.contains(learningClass.status())) {
                throw new ValidationException("class", "Assessments cannot be added to an archived class.");
            }
            long assessmentId = assessmentDao.insertAssessment(connection, new NewAssessment(
                    learningClass.id(),
                    teacherUserId,
                    values.title(),
                    values.instructions(),
                    "DRAFT",
                    values.availableFrom(),
                    values.dueAt(),
                    values.timeLimitMinutes(),
                    values.maxAttempts(),
                    values.passingScorePercent(),
                    values.revealCorrectAnswers()));
            audit(connection, teacherUserId, "ASSESSMENT_CREATED", ASSESSMENT_ENTITY, assessmentId,
                    "{\"classId\":" + learningClass.id() + "}", clientContext);
            return assessmentId;
        });
    }

    public void updateAssessment(
            long teacherUserId,
            long assessmentId,
            AssessmentForm form,
            ClientContext clientContext) {
        requirePositive(teacherUserId, "Teacher authorization could not be verified.");
        requirePositive(assessmentId, "Assessment was not found.");
        Objects.requireNonNull(form, "form");
        ValidatedAssessment values = validateAssessmentForm(form);
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            AssessmentSummary assessment = lockOwnedAssessment(connection, teacherUserId, assessmentId);
            if (!assessment.isDraft()) {
                throw new ValidationException("assessment", "Published or closed assessments cannot be edited.");
            }
            int updated = assessmentDao.updateAssessment(connection, assessmentId, new AssessmentUpdate(
                    values.title(),
                    values.instructions(),
                    values.availableFrom(),
                    values.dueAt(),
                    values.timeLimitMinutes(),
                    values.maxAttempts(),
                    values.passingScorePercent(),
                    values.revealCorrectAnswers()));
            if (updated != 1) {
                throw new AssessmentAccessException("Assessment was not found.");
            }
            audit(connection, teacherUserId, "ASSESSMENT_UPDATED", ASSESSMENT_ENTITY, assessmentId,
                    "{\"status\":\"DRAFT\"}", clientContext);
            return null;
        });
    }

    public void publishAssessment(
            long teacherUserId, long assessmentId, ClientContext clientContext) {
        requirePositive(teacherUserId, "Teacher authorization could not be verified.");
        requirePositive(assessmentId, "Assessment was not found.");
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            AssessmentSummary assessment = lockOwnedAssessment(connection, teacherUserId, assessmentId);
            if (!assessment.isDraft()) {
                throw new ValidationException("assessment", "Only draft assessments can be published.");
            }
            if (!"ACTIVE".equals(assessment.classStatus())) {
                throw new ValidationException("class", "Activate the class before publishing its assessments.");
            }
            List<Question> questions = assessmentDao.findQuestions(connection, assessmentId);
            validateForPublishing(questions);
            if (assessment.dueAt() != null && assessment.dueAt().toInstant().isBefore(Instant.now())) {
                throw new ValidationException("dueAt", "The due date must be in the future before publishing.");
            }
            if (assessmentDao.updateAssessmentStatus(connection, assessmentId, "PUBLISHED") != 1) {
                throw new AssessmentAccessException("Assessment was not found.");
            }
            audit(connection, teacherUserId, "ASSESSMENT_PUBLISHED", ASSESSMENT_ENTITY, assessmentId,
                    "{\"questionCount\":" + questions.size() + "}", clientContext);
            return null;
        });
    }

    public void closeAssessment(
            long teacherUserId, long assessmentId, ClientContext clientContext) {
        requirePositive(teacherUserId, "Teacher authorization could not be verified.");
        requirePositive(assessmentId, "Assessment was not found.");
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            AssessmentSummary assessment = lockOwnedAssessment(connection, teacherUserId, assessmentId);
            if (!assessment.isPublished()) {
                throw new ValidationException("assessment", "Only published assessments can be closed.");
            }
            if (assessmentDao.updateAssessmentStatus(connection, assessmentId, "CLOSED") != 1) {
                throw new AssessmentAccessException("Assessment was not found.");
            }
            int expired = assessmentDao.expireOpenAttempts(connection, assessmentId);
            audit(connection, teacherUserId, "ASSESSMENT_CLOSED", ASSESSMENT_ENTITY, assessmentId,
                    "{\"expiredAttempts\":" + expired + "}", clientContext);
            return null;
        });
    }

    public void deleteAssessment(
            long teacherUserId, long assessmentId, ClientContext clientContext) {
        requirePositive(teacherUserId, "Teacher authorization could not be verified.");
        requirePositive(assessmentId, "Assessment was not found.");
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            AssessmentSummary assessment = lockOwnedAssessment(connection, teacherUserId, assessmentId);
            if (!assessment.isDraft()) {
                throw new ValidationException("assessment", "Only draft assessments can be deleted.");
            }
            if (assessmentDao.deleteAssessment(connection, assessmentId) != 1) {
                throw new AssessmentAccessException("Assessment was not found.");
            }
            audit(connection, teacherUserId, "ASSESSMENT_DELETED", ASSESSMENT_ENTITY, assessmentId,
                    "{\"title\":\"" + escape(assessment.title()) + "\"}", clientContext);
            return null;
        });
    }

    public long addQuestion(
            long teacherUserId,
            long assessmentId,
            QuestionForm form,
            ClientContext clientContext) {
        requirePositive(teacherUserId, "Teacher authorization was not verified.");
        requirePositive(assessmentId, "Assessment was not found.");
        Objects.requireNonNull(form, "form");
        ValidatedQuestion values = validateQuestionForm(form);
        return database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            AssessmentSummary assessment = lockOwnedAssessment(connection, teacherUserId, assessmentId);
            if (!assessment.isDraft()) {
                throw new ValidationException("assessment", "Questions can only be changed while the assessment is a draft.");
            }
            if (assessment.questionCount() >= MAX_QUESTIONS) {
                throw new ValidationException("question", "An assessment can contain at most " + MAX_QUESTIONS + " questions.");
            }
            int position = assessmentDao.nextQuestionPosition(connection, assessmentId);
            long questionId = assessmentDao.insertQuestion(connection, assessmentId, new NewQuestion(
                    values.type(),
                    values.prompt(),
                    values.points(),
                    position,
                    values.required()));
            int optionPosition = 1;
            for (ValidatedOption option : values.options()) {
                assessmentDao.insertOption(
                        connection, questionId, option.text(), option.correct(), optionPosition++);
            }
            audit(connection, teacherUserId, "ASSESSMENT_QUESTION_ADDED", ASSESSMENT_ENTITY, assessmentId,
                    "{\"questionId\":" + questionId + ",\"type\":\"" + values.type() + "\"}", clientContext);
            return questionId;
        });
    }

    public void deleteQuestion(
            long teacherUserId,
            long assessmentId,
            long questionId,
            ClientContext clientContext) {
        requirePositive(teacherUserId, "Teacher authorization was not verified.");
        requirePositive(assessmentId, "Assessment was not found.");
        requirePositive(questionId, "Question was not found.");
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            AssessmentSummary assessment = lockOwnedAssessment(connection, teacherUserId, assessmentId);
            if (!assessment.isDraft()) {
                throw new ValidationException("assessment", "Questions can only be changed while the assessment is a draft.");
            }
            if (assessmentDao.deleteQuestion(connection, assessmentId, questionId) != 1) {
                throw new AssessmentAccessException("Question was not found.");
            }
            audit(connection, teacherUserId, "ASSESSMENT_QUESTION_DELETED", ASSESSMENT_ENTITY, assessmentId,
                    "{\"questionId\":" + questionId + "}", clientContext);
            return null;
        });
    }

    public long startAttempt(
            long studentUserId, long assessmentId, ClientContext clientContext) {
        requirePositive(studentUserId, "Student authorization could not be verified.");
        requirePositive(assessmentId, "Assessment was not found.");
        return database.inTransaction(connection -> {
            requireApprovedStudentForUpdate(connection, studentUserId);
            if (!assessmentDao.lockAssessment(connection, assessmentId)) {
                throw new AssessmentAccessException("Assessment was not found.");
            }
            AssessmentSummary assessment = requireStudentAssessment(connection, studentUserId, assessmentId);
            if (!assessment.isPublished() || !assessment.isOpen()) {
                throw new ValidationException("assessment", "This assessment is not open for attempts.");
            }
            AttemptRecord inProgress = assessmentDao
                    .findInProgressAttemptForUpdate(connection, assessmentId, studentUserId)
                    .orElse(null);
            if (inProgress != null) {
                if (!isAttemptExpired(inProgress, assessment)) {
                    return inProgress.id();
                }
                assessmentDao.expireAttempt(connection, inProgress.id());
            }
            long attemptsUsed = assessmentDao.countAttempts(connection, assessmentId, studentUserId);
            if (attemptsUsed >= assessment.maxAttempts()) {
                throw new ValidationException("attempt", "You have used all available attempts for this assessment.");
            }
            long attemptId = assessmentDao.insertAttempt(
                    connection,
                    assessmentId,
                    studentUserId,
                    (int) attemptsUsed + 1,
                    Instant.now());
            audit(connection, studentUserId, "ASSESSMENT_ATTEMPT_STARTED", ATTEMPT_ENTITY, attemptId,
                    "{\"assessmentId\":" + assessmentId + "}", clientContext);
            return attemptId;
        });
    }

    public void submitAttempt(
            long studentUserId,
            long attemptId,
            Map<Long, String> textAnswers,
            Map<Long, Set<Long>> selectedOptions,
            ClientContext clientContext) {
        requirePositive(studentUserId, "Student authorization could not be verified.");
        requirePositive(attemptId, "Attempt was not found.");
        SubmitOutcome outcome = database.inTransaction(connection -> {
            requireApprovedStudentForUpdate(connection, studentUserId);
            AttemptRecord attempt = assessmentDao.findAttemptForUpdate(connection, attemptId)
                    .orElseThrow(() -> new AssessmentAccessException("Attempt was not found."));
            if (attempt.studentUserId() != studentUserId) {
                throw new AssessmentAccessException("Attempt was not found.");
            }
            if (!"IN_PROGRESS".equals(attempt.status())) {
                throw new ValidationException("attempt", "This attempt has already been submitted or closed.");
            }
            AssessmentSummary assessment = assessmentDao.findAssessment(connection, attempt.assessmentId())
                    .orElseThrow(() -> new AssessmentAccessException("Assessment was not found."));
            if (isAttemptExpired(attempt, assessment)) {
                assessmentDao.expireAttempt(connection, attemptId);
                return new SubmitOutcome(0, "The time limit for this attempt has expired. It has been closed.");
            }
            List<Question> questions = assessmentDao.findQuestions(connection, attempt.assessmentId());
            Map<Long, SubmittedAnswer> submitted = validateSubmittedAnswers(
                    questions, textAnswers, selectedOptions);
            Instant now = Instant.now();
            BigDecimal score = BigDecimal.ZERO;
            boolean manualGradingRequired = false;
            for (Question question : questions) {
                SubmittedAnswer answer = submitted.get(question.id());
                if (answer == null || !answer.answered()) {
                    continue;
                }
                Boolean correct = answer.correct();
                BigDecimal pointsAwarded = answer.pointsAwarded();
                if (question.isShortText()) {
                    manualGradingRequired = true;
                    correct = null;
                    pointsAwarded = null;
                } else if (correct) {
                    score = score.add(question.points());
                }
                long answerId = assessmentDao.insertAnswer(
                        connection,
                        attemptId,
                        question.id(),
                        answer.text(),
                        correct,
                        pointsAwarded);
                for (Long optionId : answer.selectedOptionIds()) {
                    assessmentDao.insertAnswerOption(connection, answerId, question.id(), optionId);
                }
            }
            if (manualGradingRequired) {
                assessmentDao.completeAttempt(
                        connection, attemptId, "SUBMITTED", now, null, null, null);
                audit(connection, studentUserId, "ASSESSMENT_ATTEMPT_SUBMITTED", ATTEMPT_ENTITY, attemptId,
                        "{\"assessmentId\":" + attempt.assessmentId() + ",\"grading\":\"MANUAL\"}",
                        clientContext);
            } else {
                assessmentDao.completeAttempt(
                        connection, attemptId, "GRADED", now, now, score, null);
                audit(connection, studentUserId, "ASSESSMENT_ATTEMPT_GRADED", ATTEMPT_ENTITY, attemptId,
                        "{\"assessmentId\":" + attempt.assessmentId() + ",\"grading\":\"AUTOMATIC\"}",
                        clientContext);
            }
            return new SubmitOutcome(attemptId, null);
        });
        if (outcome.error() != null) {
            throw new ValidationException("attempt", outcome.error());
        }
    }

    public void gradeAttempt(
            long teacherUserId,
            long assessmentId,
            long attemptId,
            Map<Long, BigDecimal> pointsByAnswerId,
            ClientContext clientContext) {
        requirePositive(teacherUserId, "Teacher authorization could not be verified.");
        requirePositive(assessmentId, "Assessment was not found.");
        requirePositive(attemptId, "Attempt was not found.");
        Objects.requireNonNull(pointsByAnswerId, "pointsByAnswerId");
        database.inTransaction(connection -> {
            requireApprovedTeacherForUpdate(connection, teacherUserId);
            AssessmentSummary assessment = lockOwnedAssessment(connection, teacherUserId, assessmentId);
            AttemptRecord attempt = assessmentDao.findAttemptForUpdate(connection, attemptId)
                    .orElseThrow(() -> new AssessmentAccessException("Attempt was not found."));
            if (attempt.assessmentId() != assessmentId) {
                throw new AssessmentAccessException("Attempt was not found.");
            }
            if (!"SUBMITTED".equals(attempt.status())) {
                throw new ValidationException("attempt", "Only submitted attempts can be graded.");
            }
            Map<Long, Question> questions = questionsById(
                    assessmentDao.findQuestions(connection, assessmentId));
            BigDecimal total = BigDecimal.ZERO;
            Map<String, String> errors = new LinkedHashMap<>();
            int manualAnswers = 0;
            for (AnswerRecord answer : attempt.answers()) {
                Question question = questions.get(answer.questionId());
                if (question == null) {
                    throw new AssessmentAccessException("Attempt was not found.");
                }
                if (question.isShortText()) {
                    manualAnswers++;
                    BigDecimal points = pointsByAnswerId.get(answer.id());
                    if (points == null) {
                        errors.put("points_" + answer.id(), "Award points for this response.");
                    } else if (points.signum() < 0 || points.compareTo(question.points()) > 0) {
                        errors.put("points_" + answer.id(),
                                "Award between 0 and " + AssessmentViews.decimalText(question.points()) + " points.");
                    } else {
                        total = total.add(points);
                    }
                } else if (answer.pointsAwarded() != null) {
                    total = total.add(answer.pointsAwarded());
                }
            }
            if (manualAnswers == 0) {
                throw new ValidationException("attempt", "This attempt has no written responses to grade.");
            }
            if (!errors.isEmpty()) {
                throw new ValidationException(errors);
            }
            for (AnswerRecord answer : attempt.answers()) {
                Question question = questions.get(answer.questionId());
                if (question != null && question.isShortText()) {
                    BigDecimal points = pointsByAnswerId.get(answer.id());
                    boolean fullCredit = points.compareTo(question.points()) == 0;
                    assessmentDao.updateAnswerGrade(
                            connection, answer.id(), attemptId, fullCredit, points);
                }
            }
            if (assessmentDao.completeAttempt(
                    connection, attemptId, "GRADED", null, Instant.now(), total, teacherUserId) != 1) {
                throw new AssessmentAccessException("Attempt was not found.");
            }
            audit(connection, teacherUserId, "ASSESSMENT_ATTEMPT_MANUALLY_GRADED", ATTEMPT_ENTITY, attemptId,
                    "{\"assessmentId\":" + assessmentId + ",\"score\":" + total.toPlainString() + "}",
                    clientContext);
            return null;
        });
    }

    private AttemptPage loadAttemptPage(
            long studentUserId,
            long attemptId,
            Map<Long, String> textAnswers,
            Map<Long, Set<Long>> selectedOptions,
            boolean preserveSubmittedAnswers) {
        requirePositive(studentUserId, "Student authorization could not be verified.");
        requirePositive(attemptId, "Attempt was not found.");
        return database.inTransaction(connection -> {
            requireApprovedUser(connection, studentUserId, UserRole.STUDENT);
            AttemptRecord attempt = assessmentDao.findAttempt(connection, attemptId)
                    .orElseThrow(() -> new AssessmentAccessException("Attempt was not found."));
            if (attempt.studentUserId() != studentUserId) {
                throw new AssessmentAccessException("Attempt was not found.");
            }
            AssessmentSummary assessment = assessmentDao.findAssessment(connection, attempt.assessmentId())
                    .orElseThrow(() -> new AssessmentAccessException("Assessment was not found."));
            if ("IN_PROGRESS".equals(attempt.status()) && isAttemptExpired(attempt, assessment)) {
                assessmentDao.expireAttempt(connection, attemptId);
                attempt = assessmentDao.findAttempt(connection, attemptId)
                        .orElseThrow(() -> new AssessmentAccessException("Attempt was not found."));
            }
            List<Question> questions = assessmentDao.findQuestions(connection, attempt.assessmentId());
            boolean review = !"IN_PROGRESS".equals(attempt.status());
            boolean reveal = review
                    && "GRADED".equals(attempt.status())
                    && assessment.revealCorrectAnswers();
            AttemptDetail detail = toAttemptDetail(attempt, questions, assessment, reveal);
            List<Answer> answers;
            if (review) {
                answers = detail.getAnswers();
            } else {
                answers = buildAnswerResponses(
                        questions,
                        attempt.answers(),
                        preserveSubmittedAnswers ? textAnswers : Map.of(),
                        preserveSubmittedAnswers ? selectedOptions : Map.of(),
                        false);
            }
            return new AttemptPage(assessment, detail, questions, answers, review, reveal);
        });
    }

    private AssessmentSummary requireOwnedAssessment(
            Connection connection, long teacherUserId, long assessmentId) throws SQLException {
        AssessmentSummary assessment = assessmentDao.findAssessment(connection, assessmentId)
                .orElseThrow(() -> new AssessmentAccessException("Assessment was not found."));
        if (assessment.teacherUserId() != teacherUserId) {
            throw new AssessmentAccessException("Assessment was not found.");
        }
        return assessment;
    }

    private AssessmentSummary lockOwnedAssessment(
            Connection connection, long teacherUserId, long assessmentId) throws SQLException {
        if (!assessmentDao.lockAssessment(connection, assessmentId)) {
            throw new AssessmentAccessException("Assessment was not found.");
        }
        return requireOwnedAssessment(connection, teacherUserId, assessmentId);
    }

    private AssessmentSummary requireStudentAssessment(
            Connection connection, long studentUserId, long assessmentId) throws SQLException {
        AssessmentSummary assessment = assessmentDao.findAssessment(connection, assessmentId)
                .orElseThrow(() -> new AssessmentAccessException("Assessment was not found."));
        boolean visibleStatus = assessment.isPublished() || assessment.isClosed();
        if (!visibleStatus
                || !"ACTIVE".equals(assessment.classStatus())
                || !enrollmentDao.exists(connection, assessment.classId(), studentUserId)) {
            throw new AssessmentAccessException("Assessment was not found.");
        }
        return assessment;
    }

    private UserAccount requireApprovedUser(Connection connection, long userId, UserRole role)
            throws SQLException {
        UserAccount user = userDao.findById(connection, userId)
                .orElseThrow(() -> new AssessmentAccessException("Authorization could not be verified."));
        if (user.role() != role || user.approvalStatus() != ApprovalStatus.APPROVED) {
            throw new AssessmentAccessException("Authorization could not be verified.");
        }
        return user;
    }

    private UserAccount requireApprovedTeacherForUpdate(Connection connection, long teacherUserId)
            throws SQLException {
        UserAccount user = userDao.findByIdForUpdate(connection, teacherUserId)
                .orElseThrow(() -> new AssessmentAccessException("Authorization could not be verified."));
        if (user.role() != UserRole.TEACHER || user.approvalStatus() != ApprovalStatus.APPROVED) {
            throw new AssessmentAccessException("An approved teacher account is required.");
        }
        return user;
    }

    private UserAccount requireApprovedStudentForUpdate(Connection connection, long studentUserId)
            throws SQLException {
        UserAccount user = userDao.findByIdForUpdate(connection, studentUserId)
                .orElseThrow(() -> new AssessmentAccessException("Authorization could not be verified."));
        if (user.role() != UserRole.STUDENT || user.approvalStatus() != ApprovalStatus.APPROVED) {
            throw new AssessmentAccessException("An approved student account is required.");
        }
        return user;
    }

    private static AttemptDetail toAttemptDetail(
            AttemptRecord attempt,
            List<Question> questions,
            AssessmentSummary assessment,
            boolean showCorrect) {
        List<Answer> answers = buildAnswerResponses(
                questions,
                attempt.answers(),
                Map.of(),
                Map.of(),
                showCorrect);
        BigDecimal score = attempt.score();
        BigDecimal percentage = score == null
                ? null
                : percent(score, assessment.totalPoints());
        Boolean passed = percentage == null || assessment.passingScorePercent() == null
                ? null
                : percentage.compareTo(assessment.passingScorePercent()) >= 0;
        return new AttemptDetail(
                attempt.id(),
                attempt.assessmentId(),
                attempt.studentUserId(),
                attempt.studentName(),
                attempt.studentEmail(),
                attempt.attemptNumber(),
                attempt.status(),
                attempt.startedAt(),
                attempt.submittedAt(),
                attempt.gradedAt(),
                score,
                assessment.totalPoints(),
                percentage,
                passed,
                answers);
    }

    private static List<Answer> buildAnswerResponses(
            List<Question> questions,
            List<AnswerRecord> savedAnswers,
            Map<Long, String> submittedText,
            Map<Long, Set<Long>> submittedOptions,
            boolean showCorrect) {
        Map<Long, AnswerRecord> saved = new LinkedHashMap<>();
        for (AnswerRecord answer : savedAnswers) {
            saved.put(answer.questionId(), answer);
        }
        List<Answer> answers = new ArrayList<>();
        for (Question question : questions) {
            AnswerRecord record = saved.get(question.id());
            String answerText = record == null ? submittedText.get(question.id()) : record.answerText();
            Set<Long> selected = record == null
                    ? submittedOptions.getOrDefault(question.id(), Set.of())
                    : record.selectedOptionIds();
            List<AnswerOption> options = question.options().stream()
                    .map(option -> new AnswerOption(
                            option.id(),
                            option.text(),
                            selected.contains(option.id()),
                            showCorrect && option.correct(),
                            option.position()))
                    .toList();
            answers.add(new Answer(
                    record == null ? 0 : record.id(),
                    question.id(),
                    question.prompt(),
                    question.type(),
                    question.points(),
                    answerText,
                    record == null ? null : record.correct(),
                    record == null ? null : record.pointsAwarded(),
                    options));
        }
        return List.copyOf(answers);
    }

    private static Map<Long, SubmittedAnswer> validateSubmittedAnswers(
            List<Question> questions,
            Map<Long, String> textAnswers,
            Map<Long, Set<Long>> selectedOptions) {
        Map<String, String> errors = new LinkedHashMap<>();
        Map<Long, SubmittedAnswer> answers = new LinkedHashMap<>();
        for (Question question : questions) {
            if (question.isShortText()) {
                String text = JdbcSupport.trimToNull(textAnswers.get(question.id()));
                if (text != null && text.length() > MAX_TEXT_ANSWER_LENGTH) {
                    errors.put("question_" + question.id(),
                            "Keep this response to " + MAX_TEXT_ANSWER_LENGTH + " characters or fewer.");
                }
                if (question.required() && text == null) {
                    errors.put("question_" + question.id(), "This response is required.");
                }
                answers.put(question.id(), new SubmittedAnswer(
                        text, Set.of(), null, null, text != null));
                continue;
            }
            Set<Long> selected = new LinkedHashSet<>(
                    selectedOptions.getOrDefault(question.id(), Set.of()));
            Set<Long> allowed = question.options().stream()
                    .map(Option::id)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (!allowed.containsAll(selected)) {
                errors.put("question_" + question.id(), "Choose one of the available answers.");
            }
            if ((question.isSingleChoice() || question.isTrueFalse()) && selected.size() > 1) {
                errors.put("question_" + question.id(), "Choose exactly one answer.");
            }
            if (question.required() && selected.isEmpty()) {
                errors.put("question_" + question.id(), "This question is required.");
            }
            Set<Long> correctIds = question.getCorrectOptions().stream()
                    .map(Option::id)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            boolean correct = !selected.isEmpty() && selected.equals(correctIds);
            answers.put(question.id(), new SubmittedAnswer(
                    null, Set.copyOf(selected), correct, correct ? question.points() : BigDecimal.ZERO,
                    !selected.isEmpty()));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return answers;
    }

    private static ValidatedAssessment validateAssessmentForm(AssessmentForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        String title = JdbcSupport.trimToNull(form.title());
        if (title == null) {
            errors.put("title", "Assessment title is required.");
        } else if (title.length() > MAX_TITLE_LENGTH) {
            errors.put("title", "Assessment title must be " + MAX_TITLE_LENGTH + " characters or fewer.");
        }
        String instructions = JdbcSupport.trimToNull(form.instructions());
        if (instructions != null && instructions.length() > MAX_INSTRUCTIONS_LENGTH) {
            errors.put("instructions",
                    "Instructions must be " + MAX_INSTRUCTIONS_LENGTH + " characters or fewer.");
        }
        int maxAttempts = positiveInt(form.maxAttempts(), 1, MAX_ATTEMPTS, "maxAttempts",
                "Attempts must be between 1 and " + MAX_ATTEMPTS + ".", errors);
        Integer timeLimit = optionalPositiveInt(form.timeLimitMinutes(), MAX_TIME_LIMIT_MINUTES,
                "timeLimitMinutes",
                "The time limit must be between 1 and " + MAX_TIME_LIMIT_MINUTES + " minutes.", errors);
        BigDecimal passingScore = optionalPercent(form.passingScorePercent(), errors);
        Instant availableFrom = optionalDateTime(form.availableFrom(), "availableFrom",
                "Enter a valid availability date and time.", errors);
        Instant dueAt = optionalDateTime(form.dueAt(), "dueAt",
                "Enter a valid due date and time.", errors);
        if (availableFrom != null && dueAt != null && dueAt.isBefore(availableFrom)) {
            errors.put("dueAt", "The due date must be on or after the availability date.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new ValidatedAssessment(
                title,
                instructions,
                availableFrom,
                dueAt,
                timeLimit,
                maxAttempts,
                passingScore,
                form.revealCorrectAnswers());
    }

    private static ValidatedQuestion validateQuestionForm(QuestionForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        String type = form.type() == null ? "" : form.type().trim().toUpperCase(Locale.ROOT);
        if (!QUESTION_TYPES.contains(type)) {
            errors.put("type", "Choose a valid question type.");
        }
        String prompt = JdbcSupport.trimToNull(form.prompt());
        if (prompt == null) {
            errors.put("prompt", "Question text is required.");
        } else if (prompt.length() > MAX_PROMPT_LENGTH) {
            errors.put("prompt", "Question text must be " + MAX_PROMPT_LENGTH + " characters or fewer.");
        }
        BigDecimal points = optionalDecimal(form.points(), "points",
                "Points must be greater than zero.", errors);
        if (points != null && points.compareTo(MAX_POINTS) > 0) {
            errors.put("points", "Points must be " + MAX_POINTS.toPlainString() + " or fewer.");
        }
        List<ValidatedOption> options = new ArrayList<>();
        if ("TRUE_FALSE".equals(type)) {
            String value = form.trueFalseAnswer() == null
                    ? ""
                    : form.trueFalseAnswer().trim().toUpperCase(Locale.ROOT);
            if (!"TRUE".equals(value) && !"FALSE".equals(value)) {
                errors.put("trueFalseAnswer", "Choose the correct true or false answer.");
            }
            options.add(new ValidatedOption("True", "TRUE".equals(value)));
            options.add(new ValidatedOption("False", "FALSE".equals(value)));
        } else if ("SINGLE_CHOICE".equals(type) || "MULTIPLE_CHOICE".equals(type)) {
            List<String> optionTexts = form.options() == null ? List.of() : form.options();
            List<String> trimmed = new ArrayList<>();
            for (int index = 0; index < Math.min(MAX_OPTIONS, optionTexts.size()); index++) {
                String text = JdbcSupport.trimToNull(optionTexts.get(index));
                if (text != null && text.length() > MAX_OPTION_LENGTH) {
                    errors.put("option" + (index + 1),
                            "Answers must be " + MAX_OPTION_LENGTH + " characters or fewer.");
                }
                trimmed.add(text);
            }
            long filled = trimmed.stream().filter(Objects::nonNull).count();
            if (filled < 2) {
                errors.put("options", "Provide at least two answer choices.");
            }
            Set<Integer> correctIndexes = new LinkedHashSet<>();
            List<String> correctValues = form.correctOptions() == null ? List.of() : form.correctOptions();
            for (String value : correctValues) {
                Integer index = parseIndex(value);
                if (index == null || index < 1 || index > trimmed.size()
                        || trimmed.get(index - 1) == null) {
                    errors.put("correctOptions", "Choose a correct answer from the filled choices.");
                    break;
                }
                correctIndexes.add(index);
            }
            if ("SINGLE_CHOICE".equals(type) && correctIndexes.size() != 1) {
                errors.put("correctOptions", "Choose exactly one correct answer.");
            }
            if ("MULTIPLE_CHOICE".equals(type) && correctIndexes.isEmpty()) {
                errors.put("correctOptions", "Choose at least one correct answer.");
            }
            for (int index = 0; index < trimmed.size(); index++) {
                String text = trimmed.get(index);
                if (text != null) {
                    options.add(new ValidatedOption(text, correctIndexes.contains(index + 1)));
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new ValidatedQuestion(type, prompt, points, form.required(), options);
    }

    private static void validateForPublishing(List<Question> questions) {
        if (questions.isEmpty()) {
            throw new ValidationException("questions", "Add at least one question before publishing.");
        }
        for (Question question : questions) {
            if (question.isShortText()) {
                continue;
            }
            if (question.options().size() < 2) {
                throw new ValidationException("questions",
                        "Every choice question needs at least two answer options.");
            }
            int correct = question.getCorrectOptions().size();
            if ((question.isSingleChoice() || question.isTrueFalse()) && correct != 1) {
                throw new ValidationException("questions",
                        "Every single-choice and true/false question needs exactly one correct answer.");
            }
            if (question.isMultipleChoice() && correct < 1) {
                throw new ValidationException("questions",
                        "Every multiple-choice question needs at least one correct answer.");
            }
        }
    }

    private static Map<Long, Question> questionsById(List<Question> questions) {
        Map<Long, Question> byId = new LinkedHashMap<>();
        for (Question question : questions) {
            byId.put(question.id(), question);
        }
        return byId;
    }

    private static int positiveInt(
            String value,
            int minimum,
            int maximum,
            String field,
            String message,
            Map<String, String> errors) {
        Integer parsed = parseInteger(value);
        if (parsed == null || parsed < minimum || parsed > maximum) {
            errors.put(field, message);
            return minimum;
        }
        return parsed;
    }

    private static Integer optionalPositiveInt(
            String value,
            int maximum,
            String field,
            String message,
            Map<String, String> errors) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Integer parsed = parseInteger(value);
        if (parsed == null || parsed < 1 || parsed > maximum) {
            errors.put(field, message);
            return null;
        }
        return parsed;
    }

    private static BigDecimal optionalPercent(String value, Map<String, String> errors) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.signum() < 0 || parsed.compareTo(ONE_HUNDRED) > 0) {
                errors.put("passingScorePercent", "Passing score must be between 0 and 100.");
                return null;
            }
            return parsed.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            errors.put("passingScorePercent", "Enter a valid passing score percentage.");
            return null;
        }
    }

    private static BigDecimal optionalDecimal(
            String value, String field, String message, Map<String, String> errors) {
        if (value == null || value.isBlank()) {
            errors.put(field, message);
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.signum() <= 0) {
                errors.put(field, message);
                return null;
            }
            return parsed.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            errors.put(field, message);
            return null;
        }
    }

    private static Instant optionalDateTime(
            String value,
            String field,
            String message,
            Map<String, String> errors) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            LocalDateTime local = LocalDateTime.parse(value.trim(), DATE_TIME_INPUT);
            return local.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException exception) {
            errors.put(field, message);
            return null;
        }
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer parseIndex(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static BigDecimal percent(BigDecimal score, BigDecimal totalPoints) {
        if (totalPoints == null || totalPoints.signum() <= 0) {
            return null;
        }
        return score.multiply(ONE_HUNDRED).divide(totalPoints, 2, RoundingMode.HALF_UP);
    }

    private static boolean isAttemptExpired(AttemptRecord attempt, AssessmentSummary assessment) {
        if (assessment.timeLimitMinutes() == null || attempt.startedAt() == null) {
            return false;
        }
        Instant deadline = attempt.startedAt().toInstant()
                .plusSeconds(assessment.timeLimitMinutes() * 60L);
        return !Instant.now().isBefore(deadline);
    }

    private void audit(
            Connection connection,
            long actorUserId,
            String action,
            String entityType,
            long entityId,
            String detailsJson,
            ClientContext clientContext) throws SQLException {
        auditLogDao.insert(
                connection,
                actorUserId,
                action,
                entityType,
                entityId,
                detailsJson,
                clientContext == null ? null : clientContext.ipAddress(),
                clientContext == null ? null : clientContext.userAgent());
    }

    private static void requirePositive(long value, String message) {
        if (value <= 0) {
            throw new AssessmentAccessException(message);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record AssessmentForm(
            String title,
            String instructions,
            String maxAttempts,
            String timeLimitMinutes,
            String passingScorePercent,
            String availableFrom,
            String dueAt,
            boolean revealCorrectAnswers
    ) {
    }

    public record QuestionForm(
            String type,
            String prompt,
            String points,
            boolean required,
            List<String> options,
            List<String> correctOptions,
            String trueFalseAnswer
    ) {
    }

    private record ValidatedAssessment(
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

    private record ValidatedQuestion(
            String type,
            String prompt,
            BigDecimal points,
            boolean required,
            List<ValidatedOption> options
    ) {
    }

    private record ValidatedOption(String text, boolean correct) {
    }

    private record SubmittedAnswer(
            String text,
            Set<Long> selectedOptionIds,
            Boolean correct,
            BigDecimal pointsAwarded,
            boolean answered
    ) {
    }

    private record SubmitOutcome(long attemptId, String error) {
    }
}
