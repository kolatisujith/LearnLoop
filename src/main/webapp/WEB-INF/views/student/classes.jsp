<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<section data-reveal>
    <div class="ll-section-head">
        <div>
            <span class="ll-panel-kicker">My learning</span>
            <h2>Enrolled classes</h2>
        </div>
        <a class="btn ll-btn-primary btn-sm" href="${ctx}/student/discover">Discover classes</a>
    </div>

    <c:choose>
        <c:when test="${not empty classes}">
            <div class="ll-card-grid">
                <c:forEach items="${classes}" var="learningClass">
                    <article class="ll-class-card">
                        <div class="ll-class-card-top">
                            <span class="ll-role-tag"><c:out value="${learningClass.subjectCode}"/></span>
                            <span class="ll-state ll-state-approved"><c:out value="${learningClass.enrollmentStatus}"/></span>
                        </div>
                        <h3><c:out value="${learningClass.title}"/></h3>
                        <p class="ll-class-teacher">With <c:out value="${learningClass.teacherName}"/></p>
                        <c:if test="${not empty learningClass.description}">
                            <p class="ll-class-description"><c:out value="${learningClass.description}"/></p>
                        </c:if>
                        <dl class="ll-class-meta">
                            <div><dt>Materials</dt><dd><c:out value="${learningClass.materialCount}"/></dd></div>
                            <div><dt>Assessments</dt><dd><c:out value="${learningClass.assessmentCount}"/></dd></div>
                            <div>
                                <dt>Classmates</dt>
                                <dd>
                                    <c:out value="${learningClass.enrolledCount}"/>
                                    <c:if test="${not empty learningClass.maxStudents}">
                                        <span class="ll-cell-note">of <c:out value="${learningClass.maxStudents}"/></span>
                                    </c:if>
                                </dd>
                            </div>
                        </dl>
                        <p class="ll-class-foot">Joined <fmt:formatDate value="${learningClass.enrolledAt}" pattern="d MMM yyyy"/></p>
                        <a class="btn ll-btn-outline btn-sm"
                           href="${ctx}/student/classes/${learningClass.id}">Open classroom</a>
                    </article>
                </c:forEach>
            </div>
        </c:when>
        <c:otherwise>
            <section class="ll-panel">
                <c:set var="emptyTitle" value="No enrolled classes"/>
                <c:set var="emptyText" value="Browse the catalogue to find an active class you would like to join."/>
                <c:set var="emptyCtaHref" value="${ctx}/student/discover"/>
                <c:set var="emptyCtaLabel" value="Discover classes"/>
                <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
            </section>
        </c:otherwise>
    </c:choose>
</section>

<section class="ll-panel mt-4" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Pending review</span>
            <h2>Join requests</h2>
        </div>
        <a class="btn ll-btn-outline btn-sm" href="${ctx}/student/requests">All requests</a>
    </div>
    <c:choose>
        <c:when test="${not empty pendingRequests}">
            <ul class="ll-record-list">
                <c:forEach items="${pendingRequests}" var="joinRequest">
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
        </c:when>
        <c:otherwise>
            <c:set var="emptyTitle" value="No requests awaiting review"/>
            <c:set var="emptyText" value="When you ask to join a class, its status will appear here and in your full request history."/>
            <c:set var="emptyCtaHref" value="${ctx}/student/requests"/>
            <c:set var="emptyCtaLabel" value="View request history"/>
            <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
