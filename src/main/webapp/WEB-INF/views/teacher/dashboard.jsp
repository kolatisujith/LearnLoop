<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<c:if test="${not empty overview}">
    <section class="ll-stat-grid" aria-label="Teacher totals" data-reveal>
        <div class="ll-stat-card ll-stat-card--static">
            <span class="ll-stat-label">Account status</span>
            <strong class="ll-stat-value ll-stat-value--text">
                <span class="ll-state ${overview.approvalStatus == 'APPROVED' ? 'll-state-approved' : 'll-state-pending'}">
                    <c:out value="${overview.approvalStatus}"/>
                </span>
            </strong>
            <span class="ll-stat-meta">Required for classroom access</span>
        </div>
        <div class="ll-stat-card ll-stat-card--static">
            <span class="ll-stat-label">Assigned subjects</span>
            <strong class="ll-stat-value"><c:out value="${overview.subjectCount}"/></strong>
            <span class="ll-stat-meta">Active subject assignments</span>
        </div>
        <a class="ll-stat-card" href="${ctx}/teacher/classes">
            <span class="ll-stat-label">Your classes</span>
            <strong class="ll-stat-value"><c:out value="${overview.classCount}"/></strong>
            <span class="ll-stat-meta">Classes you created</span>
        </a>
        <a class="ll-stat-card" href="${ctx}/teacher/requests">
            <span class="ll-stat-label">Pending requests</span>
            <strong class="ll-stat-value"><c:out value="${overview.pendingRequestCount}"/></strong>
            <span class="ll-stat-meta">Students waiting for review</span>
        </a>
        <a class="ll-stat-card" href="${ctx}/teacher/classes">
            <span class="ll-stat-label">Enrolled students</span>
            <strong class="ll-stat-value"><c:out value="${overview.totalEnrolledStudents}"/></strong>
            <span class="ll-stat-meta">Active enrollments in your classes</span>
        </a>
    </section>

    <div class="row g-4 mt-1">
        <div class="col-xl-5">
            <section class="ll-panel h-100" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Teaching scope</span>
                        <h2>Assigned subjects</h2>
                    </div>
                </div>
                <c:choose>
                    <c:when test="${not empty overview.subjects}">
                        <ul class="ll-chip-list">
                            <c:forEach items="${overview.subjects}" var="subject">
                                <li class="ll-chip">
                                    <code><c:out value="${subject.code}"/></code>
                                    <span><c:out value="${subject.name}"/></span>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:when>
                    <c:otherwise>
                        <c:set var="emptyTitle" value="No active subject assignments"/>
                        <c:set var="emptyText" value="An administrator must assign at least one subject before new classes can be created."/>
                        <c:set var="emptyCtaHref" value=""/>
                        <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
                    </c:otherwise>
                </c:choose>
            </section>
        </div>

        <div class="col-xl-7">
            <section class="ll-panel h-100" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Requests</span>
                        <h2>Students waiting for review</h2>
                    </div>
                    <a class="btn ll-btn-outline btn-sm" href="${ctx}/teacher/requests">Open requests</a>
                </div>
                <c:choose>
                    <c:when test="${not empty overview.pendingRequests}">
                        <ul class="ll-record-list">
                            <c:forEach items="${overview.pendingRequests}" var="joinRequest">
                                <li class="ll-record">
                                    <div class="ll-record-main">
                                        <strong><c:out value="${joinRequest.studentName}"/></strong>
                                        <span><c:out value="${joinRequest.classTitle}"/></span>
                                    </div>
                                    <div class="ll-record-side">
                                        <span class="ll-state ll-state-pending">PENDING</span>
                                        <time><fmt:formatDate value="${joinRequest.createdAt}" pattern="d MMM yyyy"/></time>
                                    </div>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:when>
                    <c:otherwise>
                        <c:set var="emptyTitle" value="No pending join requests"/>
                        <c:set var="emptyText" value="New student requests for your classes will appear here."/>
                        <c:set var="emptyCtaHref" value=""/>
                        <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
                    </c:otherwise>
                </c:choose>
            </section>
        </div>
    </div>

    <section class="ll-panel mt-4" data-reveal>
        <div class="ll-panel-head">
            <div>
                <span class="ll-panel-kicker">Classroom</span>
                <h2>Your classes at a glance</h2>
            </div>
            <a class="btn ll-btn-outline btn-sm" href="${ctx}/teacher/classes">All classes</a>
        </div>
        <c:choose>
            <c:when test="${not empty overview.classes}">
                <div class="table-responsive">
                    <table class="table ll-table align-middle mb-0">
                        <thead>
                        <tr>
                            <th scope="col">Class</th>
                            <th scope="col">Subject</th>
                            <th scope="col">Status</th>
                            <th scope="col">Enrolled</th>
                            <th scope="col">Pending requests</th>
                            <th scope="col">Created</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${overview.classes}" var="learningClass">
                            <tr>
                                <td><strong><c:out value="${learningClass.title}"/></strong></td>
                                <td><code><c:out value="${learningClass.subjectCode}"/></code></td>
                                <td><span class="ll-state ll-state-neutral"><c:out value="${learningClass.status}"/></span></td>
                                <td>
                                    <c:out value="${learningClass.enrolledCount}"/>
                                    <c:if test="${not empty learningClass.maxStudents}">
                                        <span class="ll-cell-note">of <c:out value="${learningClass.maxStudents}"/></span>
                                    </c:if>
                                </td>
                                <td><c:out value="${learningClass.pendingRequestCount}"/></td>
                                <td><fmt:formatDate value="${learningClass.createdAt}" pattern="d MMM yyyy"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <c:set var="emptyTitle" value="No classes yet"/>
                <c:set var="emptyText" value="Classes you create will appear here with enrollment and request totals."/>
                <c:set var="emptyCtaHref" value=""/>
                <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
            </c:otherwise>
        </c:choose>
    </section>
</c:if>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
