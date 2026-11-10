<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<section data-reveal>
    <div class="ll-section-head">
        <div>
            <span class="ll-panel-kicker">Catalogue</span>
            <h2>Discover active classes</h2>
            <p class="ll-section-copy">Classes you already belong to are excluded. Join requests are reviewed by the class teacher.</p>
        </div>
        <a class="btn ll-btn-outline btn-sm" href="${ctx}/student/classes">My classes</a>
    </div>

    <c:choose>
        <c:when test="${not empty classes}">
            <div class="ll-card-grid">
                <c:forEach items="${classes}" var="learningClass">
                    <article class="ll-class-card">
                        <div class="ll-class-card-top">
                            <span class="ll-role-tag"><c:out value="${learningClass.subjectCode}"/></span>
                            <c:choose>
                                <c:when test="${learningClass.requestStatus == 'PENDING'}">
                                    <span class="ll-state ll-state-pending">REQUEST PENDING</span>
                                </c:when>
                                <c:when test="${not empty learningClass.requestStatus}">
                                    <span class="ll-state ll-state-neutral"><c:out value="${learningClass.requestStatus}"/></span>
                                </c:when>
                                <c:otherwise>
                                    <span class="ll-state ll-state-approved">OPEN</span>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <h3><c:out value="${learningClass.title}"/></h3>
                        <p class="ll-class-teacher">With <c:out value="${learningClass.teacherName}"/></p>
                        <c:if test="${not empty learningClass.description}">
                            <p class="ll-class-description"><c:out value="${learningClass.description}"/></p>
                        </c:if>
                        <dl class="ll-class-meta">
                            <div><dt>Subject</dt><dd><c:out value="${learningClass.subjectName}"/></dd></div>
                            <div>
                                <dt>Places</dt>
                                <dd>
                                    <c:out value="${learningClass.enrolledCount}"/>
                                    <c:if test="${not empty learningClass.maxStudents}">
                                        <span class="ll-cell-note">of <c:out value="${learningClass.maxStudents}"/></span>
                                    </c:if>
                                </dd>
                            </div>
                        </dl>
                        <c:if test="${learningClass.requestStatus == 'PENDING'}">
                            <p class="ll-class-foot">Your request is awaiting teacher approval.</p>
                        </c:if>
                    </article>
                </c:forEach>
            </div>
        </c:when>
        <c:otherwise>
            <section class="ll-panel">
                <c:set var="emptyTitle" value="No classes available to discover"/>
                <c:set var="emptyText" value="There are no active classes outside your current enrollments right now."/>
                <c:set var="emptyCtaHref" value="${ctx}/student/classes"/>
                <c:set var="emptyCtaLabel" value="View my classes"/>
                <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
            </section>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
