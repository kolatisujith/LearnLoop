<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<section class="ll-panel" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Your learning snapshot</span>
            <h2>Assessment outcomes</h2>
            <p class="ll-section-copy">Percentages compare your earned points with the total points available in each graded assessment.</p>
        </div>
        <a class="btn ll-btn-outline btn-sm" href="${ctx}/student/assessments">Open assessments</a>
    </div>
    <div class="row g-3">
        <div class="col-12 col-md-4">
            <div class="ll-stat-card h-100">
                <span class="ll-stat-label">Graded attempts</span>
                <strong class="ll-stat-value"><c:out value="${analytics.gradedAttempts}"/></strong>
                <span class="ll-cell-note">Only attempts marked graded are counted.</span>
            </div>
        </div>
        <div class="col-12 col-md-4">
            <div class="ll-stat-card h-100">
                <span class="ll-stat-label">Average percentage</span>
                <strong class="ll-stat-value">
                    <c:choose><c:when test="${not empty analytics.averagePercentage}"><c:out value="${analytics.averagePercentage}"/></c:when><c:otherwise>—</c:otherwise></c:choose>
                </strong>
                <span class="ll-cell-note">Across graded attempts in your active classes.</span>
            </div>
        </div>
        <div class="col-12 col-md-4">
            <div class="ll-stat-card h-100">
                <span class="ll-stat-label">Awaiting grading</span>
                <strong class="ll-stat-value"><c:out value="${analytics.awaitingGrading}"/></strong>
                <span class="ll-cell-note">Submitted attempts awaiting teacher review.</span>
            </div>
        </div>
    </div>
</section>

<section class="ll-panel mt-4" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Action plan</span>
            <h2>Recommended next steps</h2>
            <p class="ll-section-copy">Suggestions use your available assessments and class-level graded results; they are study prompts, not guarantees of future performance.</p>
        </div>
    </div>
    <div class="ll-material-grid">
        <c:forEach items="${analytics.recommendations}" var="recommendation">
            <article class="ll-material-card">
                <div class="ll-class-card-top">
                    <span class="ll-role-tag"><c:out value="${recommendation.category}"/></span>
                    <span class="ll-state ll-state-neutral"><c:out value="${recommendation.priorityLabel}"/></span>
                </div>
                <h3><c:out value="${recommendation.title}"/></h3>
                <p class="ll-class-description"><c:out value="${recommendation.detail}"/></p>
                <div class="ll-material-foot">
                    <a class="btn ll-btn-outline btn-sm" href="${ctx}${recommendation.href}">Open suggested activity</a>
                </div>
            </article>
        </c:forEach>
    </div>
</section>

<section class="ll-panel mt-4" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Class breakdown</span>
            <h2>Your class performance</h2>
        </div>
    </div>
    <c:choose>
        <c:when test="${not empty analytics.classes}">
            <div class="table-responsive">
                <table class="table ll-table align-middle mb-0">
                    <thead><tr><th>Class</th><th>Available assessments</th><th>Graded attempts</th><th>Average percentage</th></tr></thead>
                    <tbody>
                    <c:forEach items="${analytics.classes}" var="classStats">
                        <tr>
                            <td><strong><c:out value="${classStats.classTitle}"/></strong></td>
                            <td><c:out value="${classStats.availableAssessments}"/></td>
                            <td><c:out value="${classStats.gradedAttempts}"/></td>
                            <td><c:choose><c:when test="${not empty classStats.averagePercentage}"><strong><c:out value="${classStats.averagePercentage}"/></strong></c:when><c:otherwise><span class="ll-cell-note">No graded attempts</span></c:otherwise></c:choose></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:otherwise>
            <c:set var="emptyTitle" value="No enrolled classes yet"/>
            <c:set var="emptyText" value="Join a class to see class analytics and recommendations based on your assessment activity."/>
            <c:set var="emptyCtaHref" value="${ctx}/student/discover"/>
            <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
        </c:otherwise>
    </c:choose>
</section>

<section class="ll-panel mt-4" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Recent activity</span>
            <h2>Recent assessment results</h2>
        </div>
    </div>
    <c:choose>
        <c:when test="${not empty analytics.recentResults}">
            <div class="table-responsive">
                <table class="table ll-table align-middle mb-0">
                    <thead><tr><th>Assessment</th><th>Class</th><th>Status</th><th>Score</th><th>Outcome</th><th></th></tr></thead>
                    <tbody>
                    <c:forEach items="${analytics.recentResults}" var="result">
                        <tr>
                            <td><strong><c:out value="${result.assessmentTitle}"/></strong></td>
                            <td><c:out value="${result.classTitle}"/></td>
                            <td><c:out value="${result.statusLabel}"/></td>
                            <td><c:choose><c:when test="${result.hasPercentage}"><c:out value="${result.percentage}"/></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                            <td><c:out value="${result.outcomeLabel}"/></td>
                            <td><a class="btn ll-btn-outline btn-sm" href="${ctx}/student/attempts/${result.attemptId}">Review</a></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:otherwise>
            <c:set var="emptyTitle" value="No submitted assessment results"/>
            <c:set var="emptyText" value="When you submit an assessment, its grading status and result will appear here."/>
            <c:set var="emptyCtaHref" value="${ctx}/student/assessments"/>
            <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
