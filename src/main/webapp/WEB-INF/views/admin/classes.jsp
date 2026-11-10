<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<section class="ll-panel" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Classroom overview</span>
            <h2>All classes</h2>
        </div>
        <a class="btn ll-btn-outline btn-sm" href="${ctx}/admin/assignments">View assignments</a>
    </div>

    <c:set var="pageResult" value="${classes}"/>
    <c:set var="adminSearchPath" value="/admin/classes"/>
    <c:set var="adminSearchPlaceholder" value="Search by class, subject or teacher"/>
    <%@ include file="/WEB-INF/views/common/admin-search.jspf" %>

    <c:choose>
        <c:when test="${not empty classes.items}">
            <div class="table-responsive">
                <table class="table ll-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th scope="col">Class</th>
                        <th scope="col">Subject</th>
                        <th scope="col">Teacher</th>
                        <th scope="col">Status</th>
                        <th scope="col">Enrolled</th>
                        <th scope="col">Pending requests</th>
                        <th scope="col">Created</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${classes.items}" var="learningClass">
                        <tr>
                            <td>
                                <a href="${ctx}/admin/classes/${learningClass.id}">
                                    <strong><c:out value="${learningClass.title}"/></strong>
                                </a>
                            </td>
                            <td><code><c:out value="${learningClass.subjectCode}"/></code></td>
                            <td><c:out value="${learningClass.teacherName}"/></td>
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
            <c:set var="emptyTitle" value="No classes found"/>
            <c:set var="emptyText" value="Classes created by teachers will appear here."/>
            <c:set var="emptyCtaHref" value=""/>
            <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
        </c:otherwise>
    </c:choose>

    <%@ include file="/WEB-INF/views/common/pagination.jspf" %>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
