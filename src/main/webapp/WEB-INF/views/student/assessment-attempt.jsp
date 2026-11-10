<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<c:if test="${not empty attemptPage}">
    <c:set var="assessment" value="${attemptPage.assessment}"/>
    <c:set var="attempt" value="${attemptPage.attempt}"/>
    <c:set var="assessmentUrl" value="${ctx}/student/assessments/${assessment.id}"/>
    <c:set var="attemptUrl" value="${ctx}/student/attempts/${attempt.id}"/>
    <c:set var="answers"
           value="${attemptPage.review ? attemptPage.attempt.answers : attemptPage.answers}"/>

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
                <span class="ll-state ${attempt.statusCssClass}">
                    <c:out value="${attempt.statusLabel}"/>
                </span>
                <a class="btn ll-btn-outline btn-sm" href="${assessmentUrl}">
                    Assessment details
                </a>
            </div>
        </div>
        <div class="ll-class-facts">
            <div>
                <span class="ll-stat-label">Attempt</span>
                <strong><c:out value="${attempt.attemptNumber}"/></strong>
            </div>
            <div>
                <span class="ll-stat-label">Started</span>
                <strong><fmt:formatDate value="${attempt.startedAt}" pattern="d MMM yyyy, HH:mm"/></strong>
            </div>
            <div>
                <span class="ll-stat-label">Time limit</span>
                <strong>
                    <c:choose>
                        <c:when test="${assessment.timeLimited}">
                            <c:out value="${assessment.timeLimitMinutes}"/> minutes
                        </c:when>
                        <c:otherwise>Untimed</c:otherwise>
                    </c:choose>
                </strong>
            </div>
        </div>
    </section>

    <c:if test="${attemptPage.review}">
        <section class="ll-panel" data-reveal>
            <div class="ll-panel-head">
                <div>
                    <span class="ll-panel-kicker">Result</span>
                    <h2>Attempt summary</h2>
                </div>
                <span class="ll-state ${attempt.statusCssClass}">
                    <c:out value="${attempt.statusLabel}"/>
                </span>
            </div>
            <c:choose>
                <c:when test="${attempt.graded}">
                    <div class="ll-stat-grid">
                        <div class="ll-stat-card ll-stat-card--static">
                            <span class="ll-stat-label">Score</span>
                            <strong class="ll-stat-value"><c:out value="${attempt.scoreText}"/></strong>
                            <span class="ll-stat-meta">
                                of <c:out value="${attempt.totalPointsText}"/> points
                            </span>
                        </div>
                        <div class="ll-stat-card ll-stat-card--static">
                            <span class="ll-stat-label">Percentage</span>
                            <strong class="ll-stat-value"><c:out value="${attempt.percentageText}"/>%</strong>
                            <span class="ll-stat-meta">Attempt result</span>
                        </div>
                        <div class="ll-stat-card ll-stat-card--static">
                            <span class="ll-stat-label">Outcome</span>
                            <strong class="ll-stat-value"><c:out value="${attempt.passLabel}"/></strong>
                            <span class="ll-stat-meta">
                                <c:choose>
                                    <c:when test="${assessment.passingConfigured}">
                                        Pass mark <c:out value="${assessment.passingScoreText}"/>%
                                    </c:when>
                                    <c:otherwise>No pass mark configured</c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                    </div>
                </c:when>
                <c:when test="${attempt.awaitingGrading}">
                    <div class="alert alert-info ll-alert" role="status">
                        Your objective answers were saved. Written responses are waiting for your teacher
                        to grade them.
                    </div>
                </c:when>
                <c:when test="${attempt.expired}">
                    <div class="alert alert-info ll-alert" role="status">
                        This attempt expired before it was submitted. Start a new attempt if you have one
                        remaining and the assessment is still open.
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="alert alert-info ll-alert" role="status">
                        This attempt is closed and can no longer be submitted.
                    </div>
                </c:otherwise>
            </c:choose>
            <c:if test="${attempt.graded and not attemptPage.revealCorrectAnswers}">
                <p class="ll-form-note">
                    Your teacher has chosen not to reveal the correct answers for this assessment.
                </p>
            </c:if>
        </section>
    </c:if>

    <section class="ll-panel" data-reveal>
        <div class="ll-panel-head">
            <div>
                <span class="ll-panel-kicker">
                    ${attemptPage.review ? 'Submitted answers' : 'Your answers'}
                </span>
                <h2>Questions</h2>
            </div>
            <span class="ll-state ll-state-neutral">
                <c:out value="${assessment.questionCount}"/> question(s)
            </span>
        </div>

        <c:if test="${not attemptPage.review}">
            <div class="alert alert-info ll-alert" role="status">
                <c:choose>
                    <c:when test="${assessment.timeLimited}">
                        Your attempt is timed. Submit before the time limit runs out; late submissions
                        are not accepted.
                    </c:when>
                    <c:otherwise>
                        Answer every required question, then submit the attempt. You cannot change
                        answers after submitting.
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <c:choose>
            <c:when test="${attemptPage.review}">
                <div class="ll-material-list">
                    <c:forEach items="${attemptPage.questions}" var="question" varStatus="status">
                        <c:set var="answer" value="${answers[status.index]}"/>
                        <article class="ll-material-manage-card">
                            <div class="ll-material-manage-head">
                                <div>
                                    <span class="ll-material-type">
                                        <c:out value="${question.typeLabel}"/>
                                        &middot; <c:out value="${question.pointsText}"/> point(s)
                                    </span>
                                    <h3>
                                        <c:out value="${status.index + 1}"/>.
                                        <c:out value="${question.prompt}"/>
                                    </h3>
                                </div>
                                <c:if test="${attempt.graded}">
                                    <span class="ll-state ll-state-neutral">
                                        <c:out value="${answer.pointsAwardedText}"/> /
                                        <c:out value="${answer.maxPointsText}"/>
                                    </span>
                                </c:if>
                            </div>

                            <c:choose>
                                <c:when test="${question.shortText}">
                                    <c:choose>
                                        <c:when test="${not empty answer.answerText}">
                                            <div class="ll-material-text-preview"><c:out
                                                    value="${answer.answerText}"/></div>
                                        </c:when>
                                        <c:otherwise>
                                            <p class="ll-form-note">No written response was submitted.</p>
                                        </c:otherwise>
                                    </c:choose>
                                </c:when>
                                <c:otherwise>
                                    <ul class="ll-list">
                                        <c:forEach items="${answer.options}" var="option">
                                            <li>
                                                <c:out value="${option.text}"/>
                                                <c:if test="${option.selected}">
                                                    <span class="ll-state ll-state-pending">Your answer</span>
                                                </c:if>
                                                <c:if test="${attemptPage.revealCorrectAnswers and option.correct}">
                                                    <span class="ll-state ll-state-approved">Correct answer</span>
                                                </c:if>
                                            </li>
                                        </c:forEach>
                                    </ul>
                                    <c:if test="${attempt.graded}">
                                        <p class="ll-form-note">
                                            <c:choose>
                                                <c:when test="${answer.correctAnswer}">
                                                    This answer was correct.
                                                </c:when>
                                                <c:when test="${answer.incorrectAnswer}">
                                                    This answer was not correct.
                                                </c:when>
                                            </c:choose>
                                        </p>
                                    </c:if>
                                </c:otherwise>
                            </c:choose>
                        </article>
                    </c:forEach>
                </div>
            </c:when>
            <c:otherwise>
                <form method="post" action="${attemptUrl}" class="ll-stack-form"
                      data-confirm="Submit this attempt? You cannot change your answers after submitting.">
                    <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                    <input type="hidden" name="action" value="submit">

                    <c:forEach items="${attemptPage.questions}" var="question" varStatus="status">
                        <c:set var="answer" value="${answers[status.index]}"/>
                        <fieldset class="ll-material-manage-card">
                            <legend>
                                <span class="ll-material-type">
                                    <c:out value="${question.typeLabel}"/>
                                    &middot; <c:out value="${question.pointsText}"/> point(s)
                                    <c:if test="${question.required}">&middot; Required</c:if>
                                </span>
                                <span class="ll-question-prompt">
                                    <c:out value="${status.index + 1}"/>.
                                    <c:out value="${question.prompt}"/>
                                </span>
                            </legend>

                            <c:choose>
                                <c:when test="${question.shortText}">
                                    <c:set var="questionErrorKey" value="question_${question.id}"/>
                                    <label class="form-label visually-hidden"
                                           for="answer${question.id}">Written response</label>
                                    <textarea class="form-control ${not empty fieldErrors[questionErrorKey] ? 'is-invalid' : ''}"
                                              id="answer${question.id}" name="answer_${question.id}"
                                              rows="5" maxlength="4000"
                                              ${question.required ? 'required' : ''}><c:out value="${answer.answerText}"/></textarea>
                                    <div class="invalid-feedback"><c:out
                                            value="${fieldErrors[questionErrorKey]}"/></div>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${question.options}" var="option">
                                        <div class="form-check">
                                            <c:set var="optionSelected" value="${false}"/>
                                            <c:if test="${not empty answer}">
                                                <c:forEach items="${answer.options}" var="answerOption">
                                                    <c:if test="${answerOption.id == option.id and answerOption.selected}">
                                                        <c:set var="optionSelected" value="${true}"/>
                                                    </c:if>
                                                </c:forEach>
                                            </c:if>
                                            <c:choose>
                                                <c:when test="${optionSelected}">
                                                    <input class="form-check-input"
                                                           id="option${question.id}_${option.id}"
                                                           name="option_${question.id}"
                                                           type="${question.multipleChoice ? 'checkbox' : 'radio'}"
                                                           value="${option.id}" checked="checked" />
                                                </c:when>
                                                <c:otherwise>
                                                    <input class="form-check-input"
                                                           id="option${question.id}_${option.id}"
                                                           name="option_${question.id}"
                                                           type="${question.multipleChoice ? 'checkbox' : 'radio'}"
                                                           value="${option.id}" />
                                                </c:otherwise>
                                            </c:choose>
                                            <label class="form-check-label"
                                                   for="option${question.id}_${option.id}">
                                                <c:out value="${option.text}"/>
                                            </label>
                                        </div>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </fieldset>
                    </c:forEach>

                    <button class="btn ll-btn-primary" type="submit">Submit attempt</button>
                </form>
            </c:otherwise>
        </c:choose>
    </section>

    <c:if test="${attemptPage.review}">
        <p class="ll-form-note">
            <a href="${assessmentUrl}">Return to assessment details</a>
        </p>
    </c:if>
</c:if>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
