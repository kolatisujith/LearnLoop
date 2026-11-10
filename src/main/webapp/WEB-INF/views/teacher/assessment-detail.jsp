<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<c:if test="${not empty assessmentDetail}">
    <c:set var="assessment" value="${assessmentDetail.assessment}"/>
    <c:set var="assessmentUrl" value="${ctx}/teacher/assessments/${assessment.id}"/>
    <c:set var="classUrl" value="${ctx}/teacher/classes/${assessment.classId}?tab=assessments"/>
    <c:set var="availableFromValue" value="${assessmentFormAvailableFrom}"/>
    <c:if test="${empty availableFromValue and not empty assessment.availableFrom}">
        <fmt:formatDate var="availableFromValue" value="${assessment.availableFrom}"
                        pattern="yyyy-MM-dd'T'HH:mm"/>
    </c:if>
    <c:set var="dueAtValue" value="${assessmentFormDueAt}"/>
    <c:if test="${empty dueAtValue and not empty assessment.dueAt}">
        <fmt:formatDate var="dueAtValue" value="${assessment.dueAt}" pattern="yyyy-MM-dd'T'HH:mm"/>
    </c:if>

    <section class="ll-panel ll-class-detail-hero" data-reveal>
        <div class="ll-panel-head">
            <div>
                <span class="ll-panel-kicker">
                    <code><c:out value="${assessment.subjectCode}"/></code>
                    <c:out value="${assessment.classTitle}"/>
                </span>
                <h2><c:out value="${assessment.title}"/></h2>
            </div>
            <div class="ll-class-detail-actions">
                <span class="ll-state ${assessment.statusCssClass}">
                    <c:out value="${assessment.statusLabel}"/>
                </span>
                <a class="btn ll-btn-outline btn-sm" href="${classUrl}">Back to class</a>
            </div>
        </div>
        <p class="ll-class-description">
            <c:choose>
                <c:when test="${not empty assessment.instructions}">
                    <c:out value="${assessment.instructions}"/>
                </c:when>
                <c:otherwise>
                    <span class="ll-muted">No instructions have been added.</span>
                </c:otherwise>
            </c:choose>
        </p>
        <div class="ll-class-facts">
            <div>
                <span class="ll-stat-label">Questions</span>
                <strong><c:out value="${assessment.questionCount}"/></strong>
            </div>
            <div>
                <span class="ll-stat-label">Total points</span>
                <strong><c:out value="${assessment.totalPointsText}"/></strong>
            </div>
            <div>
                <span class="ll-stat-label">Attempts</span>
                <strong>
                    <c:out value="${assessmentDetail.attemptCount}"/>
                    <c:if test="${assessmentDetail.awaitingGradingCount > 0}">
                        &middot; <c:out value="${assessmentDetail.awaitingGradingCount}"/> to grade
                    </c:if>
                </strong>
            </div>
        </div>
    </section>

    <section class="ll-panel" data-reveal>
        <div class="ll-panel-head">
            <div>
                <span class="ll-panel-kicker">Availability and rules</span>
                <h2>Attempt settings</h2>
            </div>
            <span class="ll-state ${assessment.open ? 'll-state-approved' : 'll-state-neutral'}">
                ${assessment.open ? 'Open now' : 'Not open'}
            </span>
        </div>
        <dl class="ll-class-meta ll-class-meta--wide">
            <div>
                <dt>Available from</dt>
                <dd>
                    <c:choose>
                        <c:when test="${not empty assessment.availableFrom}">
                            <fmt:formatDate value="${assessment.availableFrom}" pattern="d MMM yyyy, HH:mm"/>
                        </c:when>
                        <c:otherwise>Immediately after publishing</c:otherwise>
                    </c:choose>
                </dd>
            </div>
            <div>
                <dt>Due</dt>
                <dd>
                    <c:choose>
                        <c:when test="${not empty assessment.dueAt}">
                            <fmt:formatDate value="${assessment.dueAt}" pattern="d MMM yyyy, HH:mm"/>
                        </c:when>
                        <c:otherwise>No due date</c:otherwise>
                    </c:choose>
                </dd>
            </div>
            <div>
                <dt>Time limit</dt>
                <dd>
                    <c:choose>
                        <c:when test="${assessment.timeLimited}">
                            <c:out value="${assessment.timeLimitMinutes}"/> minutes per attempt
                        </c:when>
                        <c:otherwise>Untimed</c:otherwise>
                    </c:choose>
                </dd>
            </div>
            <div>
                <dt>Attempts allowed</dt>
                <dd><c:out value="${assessment.maxAttempts}"/></dd>
            </div>
            <div>
                <dt>Pass mark</dt>
                <dd>
                    <c:choose>
                        <c:when test="${assessment.passingConfigured}">
                            <c:out value="${assessment.passingScoreText}"/>%
                        </c:when>
                        <c:otherwise>Not configured</c:otherwise>
                    </c:choose>
                </dd>
            </div>
            <div>
                <dt>Correct answers</dt>
                <dd>${assessment.revealCorrectAnswers ? 'Shown after grading' : 'Hidden from students'}</dd>
            </div>
        </dl>

        <div class="ll-action-stack">
            <c:if test="${assessment.draft}">
                <form method="post" action="${assessmentUrl}">
                    <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                    <input type="hidden" name="action" value="publish">
                    <button class="btn ll-btn-primary" type="submit">Publish assessment</button>
                </form>
                <form method="post" action="${assessmentUrl}"
                      data-confirm="Delete this draft assessment permanently? This cannot be undone.">
                    <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                    <input type="hidden" name="action" value="delete">
                    <button class="btn btn-outline-danger" type="submit">Delete draft</button>
                </form>
            </c:if>
            <c:if test="${assessment.published}">
                <form method="post" action="${assessmentUrl}"
                      data-confirm="Close this assessment? Open attempts will be expired and no new attempts can start.">
                    <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                    <input type="hidden" name="action" value="close">
                    <button class="btn btn-outline-danger" type="submit">Close assessment</button>
                </form>
            </c:if>
        </div>
    </section>

    <c:if test="${assessment.draft}">
        <section class="ll-panel" data-reveal>
            <div class="ll-panel-head">
                <div>
                    <span class="ll-panel-kicker">Draft settings</span>
                    <h2>Edit assessment</h2>
                </div>
            </div>
            <form method="post" action="${assessmentUrl}" class="ll-stack-form">
                <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                <input type="hidden" name="action" value="update">

                <div>
                    <label class="form-label" for="assessmentTitle">Title</label>
                    <input class="form-control ${not empty fieldErrors.title ? 'is-invalid' : ''}"
                           id="assessmentTitle" name="title" maxlength="200" required
                           value="<c:out value='${assessmentFormTitle != null ? assessmentFormTitle : assessment.title}'/>">
                    <div class="invalid-feedback"><c:out value="${fieldErrors.title}"/></div>
                </div>

                <div>
                    <label class="form-label" for="assessmentInstructions">
                        Instructions <span class="ll-optional">optional</span>
                    </label>
                    <textarea class="form-control ${not empty fieldErrors.instructions ? 'is-invalid' : ''}"
                              id="assessmentInstructions" name="instructions" rows="5" maxlength="4000"><c:out value="${assessmentFormInstructions != null ? assessmentFormInstructions : assessment.instructions}"/></textarea>
                    <div class="invalid-feedback"><c:out value="${fieldErrors.instructions}"/></div>
                </div>

                <div class="row g-3">
                    <div class="col-md-4">
                        <label class="form-label" for="assessmentAvailableFrom">
                            Available from <span class="ll-optional">optional</span>
                        </label>
                        <input class="form-control ${not empty fieldErrors.availableFrom ? 'is-invalid' : ''}"
                               id="assessmentAvailableFrom" name="availableFrom" type="datetime-local"
                               value="<c:out value='${availableFromValue}'/>">
                        <div class="invalid-feedback"><c:out value="${fieldErrors.availableFrom}"/></div>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label" for="assessmentDueAt">
                            Due <span class="ll-optional">optional</span>
                        </label>
                        <input class="form-control ${not empty fieldErrors.dueAt ? 'is-invalid' : ''}"
                               id="assessmentDueAt" name="dueAt" type="datetime-local"
                               value="<c:out value='${dueAtValue}'/>">
                        <div class="invalid-feedback"><c:out value="${fieldErrors.dueAt}"/></div>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label" for="assessmentTimeLimit">
                            Time limit in minutes <span class="ll-optional">optional</span>
                        </label>
                        <input class="form-control ${not empty fieldErrors.timeLimitMinutes ? 'is-invalid' : ''}"
                               id="assessmentTimeLimit" name="timeLimitMinutes" type="number" min="1"
                               inputmode="numeric"
                               value="<c:out value='${assessmentFormTimeLimitMinutes != null ? assessmentFormTimeLimitMinutes : assessment.timeLimitMinutes}'/>"
                               placeholder="Untimed">
                        <div class="invalid-feedback"><c:out value="${fieldErrors.timeLimitMinutes}"/></div>
                    </div>
                </div>

                <div class="row g-3">
                    <div class="col-md-4">
                        <label class="form-label" for="assessmentMaxAttempts">Attempts allowed</label>
                        <input class="form-control ${not empty fieldErrors.maxAttempts ? 'is-invalid' : ''}"
                               id="assessmentMaxAttempts" name="maxAttempts" type="number" min="1"
                               inputmode="numeric" required
                               value="<c:out value='${assessmentFormMaxAttempts != null ? assessmentFormMaxAttempts : assessment.maxAttempts}'/>">
                        <div class="invalid-feedback"><c:out value="${fieldErrors.maxAttempts}"/></div>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label" for="assessmentPassingScore">
                            Pass mark % <span class="ll-optional">optional</span>
                        </label>
                        <input class="form-control ${not empty fieldErrors.passingScorePercent ? 'is-invalid' : ''}"
                               id="assessmentPassingScore" name="passingScorePercent" type="number"
                               min="0" max="100" step="0.01" inputmode="decimal"
                               value="<c:out value='${assessmentFormPassingScorePercent != null ? assessmentFormPassingScorePercent : assessment.passingScoreText}'/>"
                               placeholder="No pass mark">
                        <div class="invalid-feedback"><c:out value="${fieldErrors.passingScorePercent}"/></div>
                    </div>
                    <div class="col-md-4 d-flex align-items-end">
                        <div class="form-check">
                            <input class="form-check-input" id="assessmentReveal" name="revealCorrectAnswers"
                                   type="checkbox"
                                   ${(assessmentFormRevealCorrectAnswers != null ? assessmentFormRevealCorrectAnswers : assessment.revealCorrectAnswers) ? 'checked' : ''}>
                            <label class="form-check-label" for="assessmentReveal">
                                Show correct answers after grading
                            </label>
                        </div>
                    </div>
                </div>

                <button class="btn ll-btn-primary" type="submit">Save assessment settings</button>
            </form>
        </section>
    </c:if>

    <section class="ll-panel" data-reveal>
        <div class="ll-panel-head">
            <div>
                <span class="ll-panel-kicker">Question builder</span>
                <h2>Questions</h2>
            </div>
            <span class="ll-state ll-state-neutral">
                <c:out value="${assessment.questionCount}"/> question(s)
            </span>
        </div>

        <c:choose>
            <c:when test="${empty assessmentDetail.questions}">
                <div class="ll-empty-state">
                    <p>
                        No questions yet. Add at least one question before publishing.
                        <c:if test="${not assessment.draft}">Questions cannot be changed after publishing.</c:if>
                    </p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="ll-material-list">
                    <c:forEach items="${assessmentDetail.questions}" var="question">
                        <article class="ll-material-manage-card">
                            <div class="ll-material-manage-head">
                                <div>
                                    <span class="ll-material-type">
                                        <c:out value="${question.typeLabel}"/>
                                        &middot; <c:out value="${question.pointsText}"/> point(s)
                                        <c:if test="${question.required}">&middot; Required</c:if>
                                    </span>
                                    <h3>
                                        <c:out value="${question.position}"/>. <c:out value="${question.prompt}"/>
                                    </h3>
                                </div>
                                <c:if test="${assessment.draft}">
                                    <form method="post" action="${assessmentUrl}"
                                          data-confirm="Delete this question? This cannot be undone.">
                                        <input type="hidden" name="_csrf"
                                               value="<c:out value='${sessionScope.csrfToken}'/>">
                                        <input type="hidden" name="action" value="question-delete">
                                        <input type="hidden" name="questionId" value="${question.id}">
                                        <button class="btn btn-outline-danger btn-sm" type="submit">Delete</button>
                                    </form>
                                </c:if>
                            </div>
                            <c:choose>
                                <c:when test="${question.shortText}">
                                    <p class="ll-form-note">
                                        Students type a written response. You award points manually after
                                        submission.
                                    </p>
                                </c:when>
                                <c:otherwise>
                                    <ul class="ll-list">
                                        <c:forEach items="${question.options}" var="option">
                                            <li>
                                                <c:out value="${option.text}"/>
                                                <c:if test="${option.correct}">
                                                    <span class="ll-state ll-state-approved">Correct</span>
                                                </c:if>
                                            </li>
                                        </c:forEach>
                                    </ul>
                                </c:otherwise>
                            </c:choose>
                        </article>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>

        <c:if test="${assessment.draft and assessment.questionCount < 100}">
            <details class="ll-material-create" ${not empty fieldErrors ? 'open' : ''}>
                <summary>Add a question</summary>
                <form method="post" action="${assessmentUrl}" class="ll-stack-form">
                    <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                    <input type="hidden" name="action" value="question-create">
                    <c:set var="questionType"
                           value="${not empty questionFormType ? questionFormType : 'SINGLE_CHOICE'}"/>

                    <div class="row g-3">
                        <div class="col-md-8">
                            <label class="form-label" for="questionPrompt">Question prompt</label>
                            <textarea class="form-control ${not empty fieldErrors.prompt ? 'is-invalid' : ''}"
                                      id="questionPrompt" name="prompt" rows="3" maxlength="4000"
                                      required><c:out value="${questionFormPrompt}"/></textarea>
                            <div class="invalid-feedback"><c:out value="${fieldErrors.prompt}"/></div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="questionType">Question type</label>
                            <select class="form-select" id="questionType" name="questionType">
                                <option value="SINGLE_CHOICE"
                                    ${questionType == 'SINGLE_CHOICE' ? 'selected' : ''}>Single choice</option>
                                <option value="MULTIPLE_CHOICE"
                                    ${questionType == 'MULTIPLE_CHOICE' ? 'selected' : ''}>Multiple choice</option>
                                <option value="TRUE_FALSE"
                                    ${questionType == 'TRUE_FALSE' ? 'selected' : ''}>True or false</option>
                                <option value="SHORT_TEXT"
                                    ${questionType == 'SHORT_TEXT' ? 'selected' : ''}>Written response</option>
                            </select>
                        </div>
                    </div>

                    <div class="row g-3">
                        <div class="col-md-4">
                            <label class="form-label" for="questionPoints">Points</label>
                            <input class="form-control ${not empty fieldErrors.points ? 'is-invalid' : ''}"
                                   id="questionPoints" name="points" type="number" min="0.01" step="0.01"
                                   inputmode="decimal" required
                                   value="<c:out value='${not empty questionFormPoints ? questionFormPoints : "1"}'/>">
                            <div class="invalid-feedback"><c:out value="${fieldErrors.points}"/></div>
                        </div>
                        <div class="col-md-8 d-flex align-items-end">
                            <div class="form-check">
                                <input class="form-check-input" id="questionRequired" name="required"
                                       type="checkbox" ${questionFormRequired ? 'checked' : ''}>
                                <label class="form-check-label" for="questionRequired">
                                    Students must answer this question
                                </label>
                            </div>
                        </div>
                    </div>

                    <fieldset>
                        <legend class="ll-panel-kicker">Choice options</legend>
                        <p class="ll-form-note">
                            For single or multiple choice, fill at least two options and tick every correct
                            answer. For true or false, choose the correct answer below instead.
                        </p>
                        <c:forEach begin="0" end="3" var="index">
                            <c:set var="optionNumber" value="${index + 1}"/>
                            <div class="row g-2 align-items-end mb-2">
                                <div class="col-md-9">
                                    <label class="form-label" for="optionText${index}">
                                        Option <c:out value="${optionNumber}"/>
                                    </label>
                                    <input class="form-control" id="optionText${index}" name="optionText"
                                           maxlength="500"
                                           value="<c:out value='${not empty questionFormOptions and questionFormOptions.size() > index ? questionFormOptions[index] : ""}'/>">
                                </div>
                                <div class="col-md-3">
                                    <div class="form-check">
                                        <input class="form-check-input" id="correctOption${index}"
                                               name="correctOptions" type="checkbox" value="${optionNumber}"
                                               ${not empty questionFormCorrectOptions and questionFormCorrectOptions.contains(optionNumber.toString()) ? 'checked' : ''}>
                                        <label class="form-check-label" for="correctOption${index}">
                                            Correct
                                        </label>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                        <c:set var="trueFalseAnswer"
                               value="${not empty questionFormTrueFalseAnswer ? questionFormTrueFalseAnswer : 'TRUE'}"/>
                        <div class="row g-3">
                            <div class="col-md-4">
                                <label class="form-label" for="trueFalseAnswer">True or false answer</label>
                                <select class="form-select" id="trueFalseAnswer" name="trueFalseAnswer">
                                    <option value="TRUE" ${trueFalseAnswer == 'TRUE' ? 'selected' : ''}>True</option>
                                    <option value="FALSE" ${trueFalseAnswer == 'FALSE' ? 'selected' : ''}>False</option>
                                </select>
                            </div>
                        </div>
                    </fieldset>

                    <button class="btn ll-btn-primary" type="submit">Add question</button>
                </form>
            </details>
        </c:if>
    </section>

    <section class="ll-panel" data-reveal>
        <div class="ll-panel-head">
            <div>
                <span class="ll-panel-kicker">Student activity</span>
                <h2>Attempts and grading</h2>
            </div>
            <span class="ll-state ${assessmentDetail.awaitingGradingCount > 0 ? 'll-state-pending' : 'll-state-neutral'}">
                <c:out value="${assessmentDetail.awaitingGradingCount}"/> awaiting grading
            </span>
        </div>

        <c:choose>
            <c:when test="${empty assessmentDetail.attempts}">
                <div class="ll-empty-state">
                    <p>No student has started an attempt yet.</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table ll-table align-middle">
                        <thead>
                        <tr>
                            <th scope="col">Student</th>
                            <th scope="col">Attempt</th>
                            <th scope="col">Status</th>
                            <th scope="col">Score</th>
                            <th scope="col">Submitted</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${assessmentDetail.attempts}" var="attempt">
                            <tr>
                                <td>
                                    <strong><c:out value="${attempt.studentName}"/></strong>
                                    <span class="ll-cell-note"><c:out value="${attempt.studentEmail}"/></span>
                                </td>
                                <td><c:out value="${attempt.attemptNumber}"/></td>
                                <td>
                                    <span class="ll-state ${attempt.statusCssClass}">
                                        <c:out value="${attempt.statusLabel}"/>
                                    </span>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty attempt.score}">
                                            <c:out value="${attempt.scoreText}"/> /
                                            <c:out value="${attempt.totalPointsText}"/>
                                            <span class="ll-cell-note"><c:out value="${attempt.percentageText}"/>%</span>
                                        </c:when>
                                        <c:otherwise><span class="ll-cell-note">Not graded</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty attempt.submittedAt}">
                                            <fmt:formatDate value="${attempt.submittedAt}" pattern="d MMM yyyy, HH:mm"/>
                                        </c:when>
                                        <c:otherwise><span class="ll-cell-note">Not submitted</span></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>

        <c:forEach items="${assessmentDetail.attempts}" var="attempt">
            <c:if test="${attempt.awaitingGrading or attempt.graded}">
                <details class="ll-material-edit" ${attempt.awaitingGrading ? 'open' : ''}>
                    <summary>
                        <c:out value="${attempt.studentName}"/> &middot; attempt
                        <c:out value="${attempt.attemptNumber}"/> &middot;
                        <c:out value="${attempt.statusLabel}"/>
                    </summary>
                    <div class="ll-stack-form">
                        <c:if test="${attempt.awaitingGrading}">
                            <form method="post" action="${assessmentUrl}" class="ll-stack-form">
                                <input type="hidden" name="_csrf"
                                       value="<c:out value='${sessionScope.csrfToken}'/>">
                                <input type="hidden" name="action" value="attempt-grade">
                                <input type="hidden" name="attemptId" value="${attempt.id}">
                        </c:if>
                        <c:choose>
                            <c:when test="${empty attempt.answers}">
                                <p class="ll-form-note">No answers were saved for this attempt.</p>
                            </c:when>
                            <c:otherwise>
                                <c:forEach items="${attempt.answers}" var="answer">
                                    <article class="ll-material-manage-card">
                                        <h3><c:out value="${answer.prompt}"/></h3>
                                        <p class="ll-cell-note">
                                            <c:out value="${answer.maxPointsText}"/> point(s) available
                                        </p>
                                        <c:choose>
                                            <c:when test="${answer.shortText}">
                                                <div class="ll-material-text-preview"><c:out
                                                        value="${answer.answerText}"/></div>
                                                <c:if test="${attempt.awaitingGrading}">
                                                    <label class="form-label" for="points${answer.id}">
                                                        Points awarded
                                                    </label>
                                                    <c:set var="pointsErrorKey" value="points_${answer.id}"/>
                                                    <input class="form-control ${not empty fieldErrors[pointsErrorKey] ? 'is-invalid' : ''}"
                                                           id="points${answer.id}" name="points_${answer.id}"
                                                           type="number" min="0"
                                                           max="${answer.maxPointsText}" step="0.01"
                                                           inputmode="decimal"
                                                           value="<c:out value='${answer.pointsAwardedText}'/>">
                                                    <div class="invalid-feedback"><c:out
                                                            value="${fieldErrors[pointsErrorKey]}"/></div>
                                                </c:if>
                                                <c:if test="${not attempt.awaitingGrading}">
                                                    <p class="ll-form-note">
                                                        Awarded
                                                        <strong><c:out value="${answer.pointsAwardedText}"/></strong>
                                                        of <c:out value="${answer.maxPointsText}"/>.
                                                    </p>
                                                </c:if>
                                            </c:when>
                                            <c:otherwise>
                                                <ul class="ll-list">
                                                    <c:forEach items="${answer.options}" var="option">
                                                        <li>
                                                            <c:out value="${option.text}"/>
                                                            <c:if test="${option.selected}">
                                                                <span class="ll-state ll-state-pending">Selected</span>
                                                            </c:if>
                                                            <c:if test="${option.correct}">
                                                                <span class="ll-state ll-state-approved">Correct</span>
                                                            </c:if>
                                                        </li>
                                                    </c:forEach>
                                                </ul>
                                            </c:otherwise>
                                        </c:choose>
                                    </article>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>

                        <c:if test="${attempt.awaitingGrading}">
                                <button class="btn ll-btn-primary" type="submit">Save grade</button>
                                <p class="ll-form-note">
                                    Enter points for every written response above, then save the grade.
                                    Objective answers are already scored.
                                </p>
                            </form>
                        </c:if>
                    </div>
                </details>
            </c:if>
        </c:forEach>
    </section>
</c:if>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
