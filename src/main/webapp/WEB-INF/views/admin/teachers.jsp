<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<c:set var="pageResult" value="${teachers}"/>
<c:set var="adminSearchPath" value="/admin/teachers"/>
<c:set var="adminSearchPlaceholder" value="Search by teacher name or email"/>
<%@ include file="/WEB-INF/views/common/admin-search.jspf" %>

<section class="ll-panel" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Accounts</span>
            <h2>Teacher accounts</h2>
        </div>
        <a class="btn ll-btn-outline btn-sm" href="${ctx}/admin/users">Open user directory</a>
    </div>

    <c:choose>
        <c:when test="${not empty teachers.items}">
            <div class="table-responsive">
                <table class="table ll-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th scope="col">Teacher</th>
                        <th scope="col">Approval status</th>
                        <th scope="col">Registered</th>
                        <th scope="col" class="ll-table-actions">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${teachers.items}" var="teacher">
                        <tr>
                            <td>
                                <strong><c:out value="${teacher.fullName}"/></strong>
                                <span class="ll-cell-note"><c:out value="${teacher.email}"/></span>
                            </td>
                            <td>
                                <span class="ll-state ${teacher.approvalStatus == 'APPROVED' ? 'll-state-approved' : (teacher.approvalStatus == 'PENDING' ? 'll-state-pending' : 'll-state-neutral')}">
                                    <c:out value="${teacher.approvalStatus}"/>
                                </span>
                                <c:if test="${teacher.approvalStatus == 'REJECTED' and not empty teacher.rejectionReason}">
                                    <span class="ll-cell-note"><c:out value="${teacher.rejectionReason}"/></span>
                                </c:if>
                            </td>
                            <td><fmt:formatDate value="${teacher.createdAt}" pattern="d MMM yyyy, HH:mm"/></td>
                            <td class="ll-table-actions">
                                <c:choose>
                                    <c:when test="${teacher.approvalStatus == 'PENDING'}">
                                        <div class="ll-action-stack">
                                            <form method="post" action="${ctx}/admin/teachers" class="ll-inline-form">
                                                <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                                <input type="hidden" name="action" value="approve">
                                                <input type="hidden" name="teacherId" value="${teacher.id}">
                                                <button class="btn ll-btn-primary btn-sm" type="submit">Approve</button>
                                            </form>
                                            <form method="post" action="${ctx}/admin/teachers" class="ll-reject-form">
                                                <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                                <input type="hidden" name="action" value="reject">
                                                <input type="hidden" name="teacherId" value="${teacher.id}">
                                                <label class="visually-hidden" for="rejectReason${teacher.id}">
                                                    Rejection reason for <c:out value="${teacher.fullName}"/>
                                                </label>
                                                <input class="form-control form-control-sm" id="rejectReason${teacher.id}"
                                                       name="reason" maxlength="500" required
                                                       placeholder="Reason required">
                                                <button class="btn btn-outline-danger btn-sm" type="submit">Reject</button>
                                            </form>
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="ll-cell-note">No pending decision</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:otherwise>
            <c:set var="emptyTitle" value="No teacher accounts found"/>
            <c:set var="emptyText" value="Teacher registrations will appear here as soon as they are created."/>
            <c:set var="emptyCtaHref" value=""/>
            <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
        </c:otherwise>
    </c:choose>

    <%@ include file="/WEB-INF/views/common/pagination.jspf" %>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
