<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<c:if test="${not empty overview}">
    <section class="ll-stat-grid" aria-label="Your learning totals" data-reveal>
        <a class="ll-stat-card" href="${ctx}/student/classes">
            <span class="ll-stat-label">Enrolled classes</span>
            <strong class="ll-stat-value"><c:out value="${overview.enrolledClassCount}"/></strong>
            <span class="ll-stat-meta">Active learning spaces</span>
        </a>
        <a class="ll-stat-card" href="${ctx}/student/requests">
            <span class="ll-stat-label">Pending requests</span>
            <strong class="ll-stat-value"><c:out value="${overview.pendingRequestCount}"/></strong>
            <span class="ll-stat-meta">View request history</span>
        </a>
        <a class="ll-stat-card" href="${ctx}/student/assessments">
            <span class="ll-stat-label">Available assessments</span>
            <strong class="ll-stat-value"><c:out value="${overview.availableAssessmentCount}"/></strong>
            <span class="ll-stat-meta">Within your enrolled classes</span>
        </a>
        <div class="ll-stat-card ll-stat-card--static">
            <span class="ll-stat-label">Recent results</span>
            <strong class="ll-stat-value"><c:out value="${overview.recentResultCount}"/></strong>
            <span class="ll-stat-meta">Graded attempts on record</span>
        </div>
    </section>

    <div class="row g-4 mt-1">
        <div class="col-xl-6">
            <section class="ll-panel h-100" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">My learning</span>
                        <h2>Your classes</h2>
                    </div>
                    <a class="btn ll-btn-outline btn-sm" href="${ctx}/student/classes">All classes</a>
                </div>
                <c:choose>
                    <c:when test="${not empty overview.classes}">
                        <ul class="ll-class-list">
                            <c:forEach items="${overview.classes}" var="learningClass">
                                <li class="ll-class-row">
                                    <div>
                                        <strong><c:out value="${learningClass.title}"/></strong>
                                        <span><c:out value="${learningClass.subjectCode}"/> &middot; <c:out value="${learningClass.teacherName}"/></span>
                                    </div>
                                    <span class="ll-state ll-state-approved"><c:out value="${learningClass.enrollmentStatus}"/></span>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:when>
                    <c:otherwise>
                        <c:set var="emptyTitle" value="You have not joined a class yet"/>
                        <c:set var="emptyText" value="Browse active classes and ask a teacher to join."/>
                        <c:set var="emptyCtaHref" value="${ctx}/student/discover"/>
                        <c:set var="emptyCtaLabel" value="Discover classes"/>
                        <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
                    </c:otherwise>
                </c:choose>
            </section>
        </div>

        <div class="col-xl-6">
            <section class="ll-panel h-100" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Upcoming work</span>
                        <h2>Available assessments</h2>
                    </div>
                    <a class="btn ll-btn-outline btn-sm" href="${ctx}/student/assessments">View all</a>
                </div>
                <c:choose>
                    <c:when test="${not empty overview.availableAssessments}">
                        <ul class="ll-record-list">
                            <c:forEach items="${overview.availableAssessments}" var="assessment">
                                <li class="ll-record">
                                    <div class="ll-record-main">
                                        <strong><c:out value="${assessment.title}"/></strong>
                                        <span><c:out value="${assessment.classTitle}"/></span>
                                    </div>
                                    <div class="ll-record-side">
                                        <c:choose>
                                            <c:when test="${not empty assessment.dueAt}">
                                                <span class="ll-cell-note">Due</span>
                                                <time><fmt:formatDate value="${assessment.dueAt}" pattern="d MMM yyyy"/></time>
                                            </c:when>
                                            <c:otherwise><span class="ll-cell-note">No due date</span></c:otherwise>
                                        </c:choose>
                                    </div>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:when>
                    <c:otherwise>
                        <c:set var="emptyTitle" value="No assessments available"/>
                        <c:set var="emptyText" value="Assessments from your enrolled classes will appear here."/>
                        <c:set var="emptyCtaHref" value=""/>
                        <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
                    </c:otherwise>
                </c:choose>
            </section>
        </div>
    </div>

    <c:if test="${not empty overview.pendingRequests}">
        <section class="ll-panel mt-4" data-reveal>
            <div class="ll-panel-head">
                <div>
                    <span class="ll-panel-kicker">Enrollment</span>
                    <h2>Pending join requests</h2>
                </div>
                <a class="btn ll-btn-outline btn-sm" href="${ctx}/student/requests">Request history</a>
            </div>
            <ul class="ll-record-list">
                <c:forEach items="${overview.pendingRequests}" var="joinRequest">
                    <li class="ll-record">
                        <div class="ll-record-main">
                            <strong><c:out value="${joinRequest.classTitle}"/></strong>
                            <span>
                                <c:out value="${joinRequest.subjectCode}"/> &middot;
                                <c:out value="${joinRequest.teacherName}"/>
                                &middot; requested
                                <fmt:formatDate value="${joinRequest.createdAt}" pattern="d MMM yyyy"/>
                            </span>
                        </div>
                        <div class="ll-record-side">
                            <span class="ll-state ${joinRequest.statusCssClass}">
                                <c:out value="${joinRequest.statusLabel}"/>
                            </span>
                        </div>
                    </li>
                </c:forEach>
            </ul>
        </section>
    </c:if>

    <section class="ll-panel mt-4" data-reveal>
        <div class="ll-panel-head">
            <div>
                <span class="ll-panel-kicker">Outcomes</span>
                <h2>Recent results</h2>
            </div>
        </div>
        <c:choose>
            <c:when test="${not empty overview.recentResults}">
                <div class="table-responsive">
                    <table class="table ll-table align-middle mb-0">
                        <thead>
                        <tr>
                            <th scope="col">Assessment</th>
                            <th scope="col">Class</th>
                            <th scope="col">Status</th>
                            <th scope="col">Score</th>
                            <th scope="col">Submitted</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${overview.recentResults}" var="result">
                            <tr>
                                <td><strong><c:out value="${result.assessmentTitle}"/></strong></td>
                                <td><c:out value="${result.classTitle}"/></td>
                                <td><span class="ll-state ll-state-neutral"><c:out value="${result.status}"/></span></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty result.score}">
                                            <strong><c:out value="${result.score}"/></strong>
                                            <c:if test="${not empty result.passingScore}">
                                                <span class="ll-cell-note">pass at <c:out value="${result.passingScore}"/></span>
                                            </c:if>
                                        </c:when>
                                        <c:otherwise><span class="ll-cell-note">Not graded</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><fmt:formatDate value="${result.submittedAt}" pattern="d MMM yyyy"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <c:set var="emptyTitle" value="No results yet"/>
                <c:set var="emptyText" value="Your graded assessment attempts will appear here."/>
                <c:set var="emptyCtaHref" value=""/>
                <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
            </c:otherwise>
        </c:choose>
    </section>
</c:if>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
