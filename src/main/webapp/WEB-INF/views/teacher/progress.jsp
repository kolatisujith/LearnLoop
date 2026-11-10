<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<section class="ll-panel" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Learning outcomes</span>
            <h2>Class analytics</h2>
            <p class="ll-section-copy">The average percentage compares earned points to the total points on each graded attempt. Pass rate includes only graded assessments with a configured pass mark.</p>
        </div>
        <a class="btn ll-btn-outline btn-sm" href="${ctx}/teacher/assessments">View assessments</a>
    </div>

    <c:choose>
        <c:when test="${not empty analytics.classes}">
            <div class="table-responsive">
                <table class="table ll-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th>Class</th><th>Students</th><th>Published assessments</th><th>Graded attempts</th>
                        <th>Awaiting grading</th><th>Average</th><th>Pass rate</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${analytics.classes}" var="classStats">
                        <tr>
                            <td><strong><c:out value="${classStats.classTitle}"/></strong></td>
                            <td><c:out value="${classStats.enrolledStudents}"/></td>
                            <td><c:out value="${classStats.publishedAssessments}"/></td>
                            <td><c:out value="${classStats.gradedAttempts}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${classStats.awaitingGrading > 0}">
                                        <span class="ll-state ll-state-pending"><c:out value="${classStats.awaitingGrading}"/> to grade</span>
                                    </c:when>
                                    <c:otherwise>0</c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose><c:when test="${classStats.gradedAttempts > 0}"><strong><c:out value="${classStats.averagePercentage}"/></strong></c:when><c:otherwise><span class="ll-cell-note">No graded attempts</span></c:otherwise></c:choose>
                            </td>
                            <td>
                                <c:choose><c:when test="${not empty classStats.passRate}"><c:out value="${classStats.passRate}"/></c:when><c:otherwise><span class="ll-cell-note">No configured pass marks</span></c:otherwise></c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:otherwise>
            <c:set var="emptyTitle" value="No class analytics yet"/>
            <c:set var="emptyText" value="Create or activate a class to begin tracking enrollment and assessment outcomes."/>
            <c:set var="emptyCtaHref" value="${ctx}/teacher/classes"/>
            <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
        </c:otherwise>
    </c:choose>
</section>

<section class="ll-panel mt-4" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Teaching follow-up</span>
            <h2>Suggested review actions</h2>
        </div>
    </div>
    <p class="ll-section-copy">Use low averages as a prompt to revisit the learning materials with your class. Submitted attempts may need manual grading before their results are included in the averages.</p>
    <div class="ll-material-grid">
        <c:forEach items="${analytics.classes}" var="classStats">
            <c:if test="${classStats.awaitingGrading > 0}">
                <article class="ll-material-card">
                    <div class="ll-class-card-top"><span class="ll-role-tag">Grading queue</span><span class="ll-state ll-state-pending"><c:out value="${classStats.awaitingGrading}"/> pending</span></div>
                    <h3><c:out value="${classStats.classTitle}"/></h3>
                    <p class="ll-class-description">Review submitted responses so the student's learning analytics can reflect a complete graded result.</p>
                    <div class="ll-material-foot"><a class="btn ll-btn-outline btn-sm" href="${ctx}/teacher/assessments">Review assessments</a></div>
                </article>
            </c:if>
        </c:forEach>
    </div>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
