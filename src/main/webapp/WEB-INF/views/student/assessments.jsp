<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<section class="ll-panel" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Assessment schedule</span>
            <h2>Available assessments</h2>
        </div>
        <a class="btn ll-btn-outline btn-sm" href="${ctx}/student/classes">My classes</a>
    </div>

    <c:choose>
        <c:when test="${not empty assessments}">
            <div class="table-responsive">
                <table class="table ll-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th scope="col">Assessment</th>
                        <th scope="col">Class</th>
                        <th scope="col">Availability</th>
                        <th scope="col">Time limit</th>
                        <th scope="col">Attempts</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${assessments}" var="assessment">
                        <tr>
                            <td>
                                <strong>
                                    <a href="${ctx}/student/assessments/${assessment.id}"><c:out
                                            value="${assessment.title}"/></a>
                                </strong>
                            </td>
                            <td>
                                <c:out value="${assessment.classTitle}"/>
                                <span class="ll-cell-note"><c:out value="${assessment.subjectCode}"/></span>
                            </td>
                            <td class="ll-table-time">
                                <c:choose>
                                    <c:when test="${not empty assessment.availableFrom}">
                                        <span>From <fmt:formatDate value="${assessment.availableFrom}" pattern="d MMM yyyy, HH:mm"/></span>
                                    </c:when>
                                    <c:otherwise><span class="ll-cell-note">Available immediately</span></c:otherwise>
                                </c:choose>
                                <c:choose>
                                    <c:when test="${not empty assessment.dueAt}">
                                        <span>Due <fmt:formatDate value="${assessment.dueAt}" pattern="d MMM yyyy, HH:mm"/></span>
                                    </c:when>
                                    <c:otherwise><span class="ll-cell-note">No due date</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${not empty assessment.timeLimitMinutes}">
                                        <c:out value="${assessment.timeLimitMinutes}"/> min
                                    </c:when>
                                    <c:otherwise><span class="ll-cell-note">Untimed</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:out value="${assessment.attemptsUsed}"/>
                                <span class="ll-cell-note">of <c:out value="${assessment.maxAttempts}"/> used</span>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:otherwise>
            <c:set var="emptyTitle" value="No assessments available"/>
            <c:set var="emptyText" value="Assessments from your enrolled classes will appear here."/>
            <c:set var="emptyCtaHref" value=""/>
            <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
