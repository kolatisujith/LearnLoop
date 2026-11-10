<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<section class="ll-panel" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Directory</span>
            <h2>Registered users</h2>
        </div>
        <a class="btn ll-btn-outline btn-sm" href="${ctx}/admin/teachers">Teacher approvals</a>
    </div>

    <c:set var="pageResult" value="${users}"/>
    <c:set var="adminSearchPath" value="/admin/users"/>
    <c:set var="adminSearchPlaceholder" value="Search by name or email"/>
    <%@ include file="/WEB-INF/views/common/admin-search.jspf" %>

    <c:choose>
        <c:when test="${not empty users.items}">
            <div class="table-responsive">
                <table class="table ll-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th scope="col">Name</th>
                        <th scope="col">Email</th>
                        <th scope="col">Role</th>
                        <th scope="col">Status</th>
                        <th scope="col">Registered</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${users.items}" var="user">
                        <tr>
                            <td><strong><c:out value="${user.fullName}"/></strong></td>
                            <td><c:out value="${user.email}"/></td>
                            <td><span class="ll-role-tag"><c:out value="${user.role}"/></span></td>
                            <td>
                                <span class="ll-state ${user.approvalStatus == 'APPROVED' ? 'll-state-approved' : (user.approvalStatus == 'PENDING' ? 'll-state-pending' : 'll-state-neutral')}">
                                    <c:out value="${user.approvalStatus}"/>
                                </span>
                                <c:if test="${user.approvalStatus == 'REJECTED' and not empty user.rejectionReason}">
                                    <span class="ll-cell-note"><c:out value="${user.rejectionReason}"/></span>
                                </c:if>
                            </td>
                            <td><fmt:formatDate value="${user.createdAt}" pattern="d MMM yyyy, HH:mm"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:otherwise>
            <c:set var="emptyTitle" value="No users found"/>
            <c:set var="emptyText" value="Registered accounts will appear here."/>
            <c:set var="emptyCtaHref" value=""/>
            <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
        </c:otherwise>
    </c:choose>

    <%@ include file="/WEB-INF/views/common/pagination.jspf" %>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
