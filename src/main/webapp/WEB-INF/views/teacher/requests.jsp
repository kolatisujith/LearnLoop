<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<section class="ll-panel" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Enrollment queue</span>
            <h2>Pending join requests</h2>
        </div>
        <a class="btn ll-btn-outline btn-sm" href="${ctx}/teacher/classes">View classes</a>
    </div>

    <c:choose>
        <c:when test="${not empty requests}">
            <div class="table-responsive">
                <table class="table ll-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th scope="col">Student</th>
                        <th scope="col">Email</th>
                        <th scope="col">Class</th>
                        <th scope="col">Message</th>
                        <th scope="col">Requested</th>
                        <th scope="col">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${requests}" var="joinRequest">
                        <tr>
                            <td><strong><c:out value="${joinRequest.studentName}"/></strong></td>
                            <td><c:out value="${joinRequest.studentEmail}"/></td>
                            <td><c:out value="${joinRequest.classTitle}"/></td>
                            <td class="ll-table-message">
                                <c:choose>
                                    <c:when test="${not empty joinRequest.message}">
                                        <c:out value="${joinRequest.message}"/>
                                    </c:when>
                                    <c:otherwise><span class="ll-cell-note">No message supplied</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td><fmt:formatDate value="${joinRequest.createdAt}" pattern="d MMM yyyy, HH:mm"/></td>
                            <td>
                                <div class="d-flex flex-column gap-2">
                                    <form method="post" action="${ctx}/teacher/requests" class="d-inline">
                                        <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                        <input type="hidden" name="requestId" value="${joinRequest.id}">
                                        <input type="hidden" name="action" value="accept">
                                        <button type="submit" class="btn btn-sm ll-btn-primary">Accept</button>
                                    </form>
                                    <form method="post" action="${ctx}/teacher/requests" class="ll-inline-form">
                                        <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                        <input type="hidden" name="requestId" value="${joinRequest.id}">
                                        <input type="hidden" name="action" value="reject">
                                        <input type="text" name="reason" class="form-control form-control-sm"
                                               maxlength="255" placeholder="Reason (optional)">
                                        <button type="submit" class="btn btn-sm ll-btn-outline">Reject</button>
                                    </form>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:otherwise>
            <c:set var="emptyTitle" value="No pending join requests"/>
            <c:set var="emptyText" value="Requests from students will appear here for review."/>
            <c:set var="emptyCtaHref" value=""/>
            <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
