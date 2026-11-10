<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<c:if test="${not empty assessmentDetail}">
    <c:set var="assessment" value="${assessmentDetail.assessment}"/>
    <c:set var="assessmentUrl" value="${ctx}/student/assessments/${assessment.id}"/>
    <c:set var="classUrl" value="${ctx}/student/classes/${assessment.classId}?tab=assessments"/>

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
                    <span class="ll-muted">No instructions were provided.</span>
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
                <span class="ll-stat-label">Attempts remaining</span>
                <strong><c:out value="${assessmentDetail.remainingAttempts}"/></strong>
            </div>
        </div>
    </section>

    <section class="ll-panel" data-reveal>
        <div class="ll-panel-head">
            <div>
                <span class="ll-panel-kicker">Rules and availability</span>
                <h2>Before you begin</h2>
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
                        <c:otherwise>Immediately</c:otherwise>
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
                            <c:out value="${assessment.timeLimitMinutes}"/> minutes
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
                        <c:otherwise>No pass mark</c:otherwise>
                    </c:choose>
                </dd>
            </div>
        </dl>

        <c:choose>
            <c:when test="${assessmentDetail.hasInProgress}">
                <div class="alert alert-info ll-alert" role="status">
                    You have an attempt in progress. Continue it before starting another.
                </div>
                <a class="btn ll-btn-primary"
                   href="${ctx}/student/attempts/${assessmentDetail.inProgress.id}">
                    Continue attempt
                </a>
            </c:when>
            <c:when test="${assessmentDetail.canStart}">
                <form method="post" action="${assessmentUrl}" class="ll-inline-form"
                      data-confirm="Start this attempt now? Your time limit begins immediately.">
                    <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                    <input type="hidden" name="action" value="start">
                    <button class="btn ll-btn-primary" type="submit">Start attempt</button>
                </form>
            </c:when>
            <c:when test="${assessmentDetail.remainingAttempts == 0}">
                <div class="alert alert-info ll-alert" role="status">
                    You have used all available attempts for this assessment.
                </div>
            </c:when>
            <c:when test="${assessment.published and not assessment.open}">
                <div class="alert alert-info ll-alert" role="status">
                    This assessment is outside its availability window.
                </div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-info ll-alert" role="status">
                    This assessment is not currently accepting attempts.
                </div>
            </c:otherwise>
        </c:choose>
    </section>

    <section class="ll-panel" data-reveal>
        <div class="ll-panel-head">
            <div>
                <span class="ll-panel-kicker">Your history</span>
                <h2>Attempts</h2>
            </div>
            <span class="ll-state ll-state-neutral">
                <c:out value="${assessmentDetail.attemptsUsed}"/> of
                <c:out value="${assessment.maxAttempts}"/> used
            </span>
        </div>

        <c:choose>
            <c:when test="${empty assessmentDetail.attempts}">
                <div class="ll-empty-state">
                    <p>You have not started this assessment yet.</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table ll-table align-middle">
                        <thead>
                        <tr>
                            <th scope="col">Attempt</th>
                            <th scope="col">Status</th>
                            <th scope="col">Score</th>
                            <th scope="col">Result</th>
                            <th scope="col">Started</th>
                            <th scope="col"></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${assessmentDetail.attempts}" var="attempt">
                            <tr>
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
                                        <c:when test="${attempt.graded}">
                                            <span class="ll-state ${attempt.passed ? 'll-state-approved' : 'll-state-neutral'}">
                                                <c:out value="${attempt.passLabel}"/>
                                            </span>
                                        </c:when>
                                        <c:otherwise><span class="ll-cell-note">Pending</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="ll-table-time">
                                    <fmt:formatDate value="${attempt.startedAt}" pattern="d MMM yyyy, HH:mm"/>
                                </td>
                                <td>
                                    <c:if test="${attempt.inProgress or not attempt.expired}">
                                        <a class="btn ll-btn-outline btn-sm"
                                           href="${ctx}/student/attempts/${attempt.id}">
                                            ${attempt.inProgress ? 'Continue' : 'Review'}
                                        </a>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</c:if>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
