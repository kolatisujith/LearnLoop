package com.learnloop.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class AssessmentViews {

    private AssessmentViews() {
    }

    public record AssessmentSummary(
            long id,
            long classId,
            long teacherUserId,
            String classTitle,
            String classStatus,
            String subjectCode,
            String teacherName,
            String title,
            String instructions,
            String status,
            Date availableFrom,
            Date dueAt,
            Integer timeLimitMinutes,
            int maxAttempts,
            BigDecimal passingScorePercent,
            boolean revealCorrectAnswers,
            long questionCount,
            BigDecimal totalPoints,
            Date createdAt,
            Date updatedAt,
            boolean open
    ) {

        public long getId() {
            return id;
        }

        public long getClassId() {
            return classId;
        }

        public long getTeacherUserId() {
            return teacherUserId;
        }

        public String getClassTitle() {
            return classTitle;
        }

        public String getClassStatus() {
            return classStatus;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public String getTitle() {
            return title;
        }

        public String getInstructions() {
            return instructions;
        }

        public String getStatus() {
            return status;
        }

        public Date getAvailableFrom() {
            return availableFrom;
        }

        public Date getDueAt() {
            return dueAt;
        }

        public Integer getTimeLimitMinutes() {
            return timeLimitMinutes;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public BigDecimal getPassingScorePercent() {
            return passingScorePercent;
        }

        public boolean isRevealCorrectAnswers() {
            return revealCorrectAnswers;
        }

        public long getQuestionCount() {
            return questionCount;
        }

        public BigDecimal getTotalPoints() {
            return totalPoints;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public Date getUpdatedAt() {
            return updatedAt;
        }

        public boolean isOpen() {
            return open;
        }

        public boolean isDraft() {
            return "DRAFT".equals(status);
        }

        public boolean isPublished() {
            return "PUBLISHED".equals(status);
        }

        public boolean isClosed() {
            return "CLOSED".equals(status);
        }

        public boolean isArchived() {
            return "ARCHIVED".equals(status);
        }

        public boolean isEditable() {
            return isDraft();
        }

        public boolean isTimeLimited() {
            return timeLimitMinutes != null;
        }

        public boolean isPassingConfigured() {
            return passingScorePercent != null;
        }

        public String getPassingScoreText() {
            return decimalText(passingScorePercent);
        }

        public String getTotalPointsText() {
            return decimalText(totalPoints);
        }

        public String getStatusLabel() {
            return switch (status) {
                case "DRAFT" -> "Draft";
                case "PUBLISHED" -> "Published";
                case "CLOSED" -> "Closed";
                case "ARCHIVED" -> "Archived";
                default -> status;
            };
        }

        public String statusLabel() {
            return getStatusLabel();
        }

        public String getStatusCssClass() {
            return switch (status) {
                case "PUBLISHED" -> "ll-state-approved";
                case "DRAFT" -> "ll-state-pending";
                case "CLOSED", "ARCHIVED" -> "ll-state-neutral";
                default -> "ll-state-neutral";
            };
        }

        public String statusCssClass() {
            return getStatusCssClass();
        }

        public boolean draft() { return isDraft(); }
        public boolean published() { return isPublished(); }
        public boolean closed() { return isClosed(); }
        public boolean archived() { return isArchived(); }
        public boolean editable() { return isEditable(); }
        public boolean timeLimited() { return isTimeLimited(); }
        public boolean passingConfigured() { return isPassingConfigured(); }
        public String passingScoreText() { return getPassingScoreText(); }
        public String totalPointsText() { return getTotalPointsText(); }
    }

    public record Question(
            long id,
            long assessmentId,
            String type,
            String prompt,
            BigDecimal points,
            int position,
            boolean required,
            List<Option> options
    ) {

        public Question {
            options = List.copyOf(options);
        }

        public long getId() {
            return id;
        }

        public long getAssessmentId() {
            return assessmentId;
        }

        public String getType() {
            return type;
        }

        public String getPrompt() {
            return prompt;
        }

        public BigDecimal getPoints() {
            return points;
        }

        public int getPosition() {
            return position;
        }

        public boolean isRequired() {
            return required;
        }

        public List<Option> getOptions() {
            return options;
        }

        public boolean isSingleChoice() {
            return "SINGLE_CHOICE".equals(type);
        }

        public boolean isMultipleChoice() {
            return "MULTIPLE_CHOICE".equals(type);
        }

        public boolean multipleChoice() {
            return isMultipleChoice();
        }

        public boolean isTrueFalse() {
            return "TRUE_FALSE".equals(type);
        }

        public boolean isShortText() {
            return "SHORT_TEXT".equals(type);
        }

        public boolean shortText() {
            return isShortText();
        }

        public boolean isChoice() {
            return !isShortText();
        }

        public boolean isAutoGraded() {
            return isChoice();
        }

        public List<Option> getCorrectOptions() {
            return options.stream().filter(Option::correct).toList();
        }

        public String getTypeLabel() {
            return switch (type) {
                case "SINGLE_CHOICE" -> "Single choice";
                case "MULTIPLE_CHOICE" -> "Multiple choice";
                case "TRUE_FALSE" -> "True or false";
                case "SHORT_TEXT" -> "Written response";
                default -> type;
            };
        }

        public String typeLabel() {
            return getTypeLabel();
        }

        public String getPointsText() {
            return decimalText(points);
        }

        public String pointsText() {
            return getPointsText();
        }
    }

    public record Option(long id, String text, boolean correct, int position) {

        public long getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public boolean isCorrect() {
            return correct;
        }

        public int getPosition() {
            return position;
        }
    }

    public record Answer(
            long id,
            long questionId,
            String prompt,
            String type,
            BigDecimal maxPoints,
            String answerText,
            Boolean correct,
            BigDecimal pointsAwarded,
            List<AnswerOption> options
    ) {

        public Answer {
            options = List.copyOf(options);
        }

        public long getId() {
            return id;
        }

        public long getQuestionId() {
            return questionId;
        }

        public String getPrompt() {
            return prompt;
        }

        public String getType() {
            return type;
        }

        public BigDecimal getMaxPoints() {
            return maxPoints;
        }

        public String getAnswerText() {
            return answerText;
        }

        public Boolean getCorrect() {
            return correct;
        }

        public BigDecimal getPointsAwarded() {
            return pointsAwarded;
        }

        public List<AnswerOption> getOptions() {
            return options;
        }

        public boolean isShortText() {
            return "SHORT_TEXT".equals(type);
        }

        public boolean shortText() {
            return isShortText();
        }

        public boolean isChoice() {
            return !isShortText();
        }

        public boolean isCorrectAnswer() {
            return Boolean.TRUE.equals(correct);
        }

        public boolean correctAnswer() {
            return isCorrectAnswer();
        }

        public boolean isIncorrectAnswer() {
            return Boolean.FALSE.equals(correct);
        }

        public boolean incorrectAnswer() {
            return isIncorrectAnswer();
        }

        public boolean isAnswered() {
            return (answerText != null && !answerText.isBlank())
                    || options.stream().anyMatch(AnswerOption::selected);
        }

        public boolean answered() {
            return isAnswered();
        }

        public String getMaxPointsText() {
            return decimalText(maxPoints);
        }

        public String maxPointsText() {
            return getMaxPointsText();
        }

        public String getPointsAwardedText() {
            return decimalText(pointsAwarded);
        }

        public String pointsAwardedText() {
            return getPointsAwardedText();
        }
    }

    public record AnswerOption(
            long id,
            String text,
            boolean selected,
            boolean correct,
            int position
    ) {

        public long getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public boolean isSelected() {
            return selected;
        }

        public boolean isCorrect() {
            return correct;
        }

        public int getPosition() {
            return position;
        }
    }

    public record AttemptDetail(
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
            BigDecimal totalPoints,
            BigDecimal percentage,
            Boolean passed,
            List<Answer> answers
    ) {

        public AttemptDetail {
            answers = List.copyOf(answers);
        }

        public long getId() {
            return id;
        }

        public long getAssessmentId() {
            return assessmentId;
        }

        public long getStudentUserId() {
            return studentUserId;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getStudentEmail() {
            return studentEmail;
        }

        public int getAttemptNumber() {
            return attemptNumber;
        }

        public String getStatus() {
            return status;
        }

        public Date getStartedAt() {
            return startedAt;
        }

        public Date getSubmittedAt() {
            return submittedAt;
        }

        public Date getGradedAt() {
            return gradedAt;
        }

        public BigDecimal getScore() {
            return score;
        }

        public BigDecimal getTotalPoints() {
            return totalPoints;
        }

        public BigDecimal getPercentage() {
            return percentage;
        }

        public Boolean getPassed() {
            return passed;
        }

        public List<Answer> getAnswers() {
            return answers;
        }

        public boolean isInProgress() {
            return "IN_PROGRESS".equals(status);
        }

        public boolean inProgress() { return isInProgress(); }

        public boolean isSubmitted() {
            return "SUBMITTED".equals(status);
        }

        public boolean submitted() { return isSubmitted(); }

        public boolean isGraded() {
            return "GRADED".equals(status);
        }

        public boolean graded() { return isGraded(); }

        public boolean isExpired() {
            return "EXPIRED".equals(status);
        }

        public boolean expired() { return isExpired(); }

        public boolean isAwaitingGrading() {
            return isSubmitted();
        }

        public boolean awaitingGrading() { return isAwaitingGrading(); }

        public List<Answer> getShortTextAnswers() {
            return answers.stream().filter(Answer::isShortText).toList();
        }

        public String getStatusLabel() {
            return switch (status) {
                case "IN_PROGRESS" -> "In progress";
                case "SUBMITTED" -> "Awaiting grading";
                case "GRADED" -> "Graded";
                case "EXPIRED" -> "Expired";
                case "ABANDONED" -> "Abandoned";
                default -> status;
            };
        }

        public String statusLabel() { return getStatusLabel(); }

        public String getStatusCssClass() {
            return switch (status) {
                case "GRADED" -> "ll-state-approved";
                case "SUBMITTED", "IN_PROGRESS" -> "ll-state-pending";
                default -> "ll-state-neutral";
            };
        }

        public String statusCssClass() { return getStatusCssClass(); }

        public String getScoreText() {
            return decimalText(score);
        }

        public String scoreText() { return getScoreText(); }

        public String getTotalPointsText() {
            return decimalText(totalPoints);
        }

        public String totalPointsText() { return getTotalPointsText(); }

        public String getPercentageText() {
            return decimalText(percentage);
        }

        public String percentageText() { return getPercentageText(); }

        public String getPassLabel() {
            if (passed == null) {
                return "Not graded";
            }
            return passed ? "Passed" : "Not passed";
        }

        public String passLabel() { return getPassLabel(); }
    }

    public record AssessmentDetail(
            AssessmentSummary assessment,
            List<Question> questions,
            List<AttemptDetail> attempts
    ) {

        public AssessmentDetail {
            questions = List.copyOf(questions);
            attempts = List.copyOf(attempts);
        }

        public AssessmentSummary getAssessment() {
            return assessment;
        }

        public List<Question> getQuestions() {
            return questions;
        }

        public List<AttemptDetail> getAttempts() {
            return attempts;
        }

        public int getAttemptCount() {
            return attempts.size();
        }

        public int attemptCount() { return getAttemptCount(); }

        public long getAwaitingGradingCount() {
            return attempts.stream().filter(AttemptDetail::isAwaitingGrading).count();
        }

        public long awaitingGradingCount() { return getAwaitingGradingCount(); }
    }

    public record StudentAssessmentDetail(
            AssessmentSummary assessment,
            AttemptDetail inProgress,
            List<AttemptDetail> attempts,
            boolean canStart
    ) {

        public StudentAssessmentDetail {
            attempts = List.copyOf(attempts);
        }

        public AssessmentSummary getAssessment() {
            return assessment;
        }

        public AttemptDetail getInProgress() {
            return inProgress;
        }

        public List<AttemptDetail> getAttempts() {
            return attempts;
        }

        public boolean isCanStart() {
            return canStart;
        }

        public boolean isHasInProgress() {
            return inProgress != null;
        }

        public boolean hasInProgress() { return isHasInProgress(); }

        public long getAttemptsUsed() {
            return attempts.size();
        }

        public long attemptsUsed() { return getAttemptsUsed(); }

        public long getRemainingAttempts() {
            return Math.max(0, assessment.maxAttempts() - attempts.size());
        }

        public long remainingAttempts() { return getRemainingAttempts(); }
    }

    public record AttemptPage(
            AssessmentSummary assessment,
            AttemptDetail attempt,
            List<Question> questions,
            List<Answer> answers,
            boolean review,
            boolean revealCorrectAnswers
    ) {

        public AttemptPage {
            questions = List.copyOf(questions);
            answers = List.copyOf(answers);
        }

        public AssessmentSummary getAssessment() {
            return assessment;
        }

        public AttemptDetail getAttempt() {
            return attempt;
        }

        public List<Question> getQuestions() {
            return questions;
        }

        public List<Answer> getAnswers() {
            return answers;
        }

        public boolean isReview() {
            return review;
        }

        public boolean isRevealCorrectAnswers() {
            return revealCorrectAnswers;
        }
    }

    public static String decimalText(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }

    public static String uppercase(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
