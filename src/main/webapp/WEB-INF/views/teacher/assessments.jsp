<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<section class="ll-panel" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Assessment activity</span>
            <h2>Assessments in your classes</h2>
        </div>
        <a class="btn ll-btn-outline btn-sm" href="${ctx}/teacher/progress">Progress reports</a>
    </div>

    <c:choose>
        <c:when test="${not empty assessments}">
            <div class="table-responsive">
                <table class="table ll-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th scope="col">Assessment</th>
                        <th scope="col">Class</th>
                        <th scope="col">Status</th>
                        <th scope="col">Availability</th>
                        <th scope="col">Attempts</th>
                        <th scope="col">Graded</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${assessments}" var="assessment">
                        <tr>
                            <td>
                                <strong>
                                    <a href="${ctx}/teacher/assessments/${assessment.id}"><c:out
                                            value="${assessment.title}"/></a>
                                </strong>
                                <span class="ll-cell-note">Up to <c:out value="${assessment.maxAttempts}"/> attempt(s)</span>
                            </td>
                            <td>
                                <c:out value="${assessment.classTitle}"/>
                                <span class="ll-cell-note"><c:out value="${assessment.subjectCode}"/></span>
                            </td>
                            <td><span class="ll-state ll-state-neutral"><c:out value="${assessment.status}"/></span></td>
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
                            <td><c:out value="${assessment.attemptCount}"/></td>
                            <td><c:out value="${assessment.gradedCount}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:otherwise>
            <c:set var="emptyTitle" value="No assessments yet"/>
            <c:set var="emptyText" value="Assessments created for your classes will appear here."/>
            <c:set var="emptyCtaHref" value=""/>
            <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
