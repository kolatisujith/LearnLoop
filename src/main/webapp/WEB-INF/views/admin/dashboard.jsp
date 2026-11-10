<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<c:if test="${not empty overview}">
    <section class="ll-stat-grid" aria-label="Platform totals" data-reveal>
        <a class="ll-stat-card" href="${ctx}/admin/users">
            <span class="ll-stat-label">Users</span>
            <strong class="ll-stat-value"><c:out value="${overview.userCount}"/></strong>
            <span class="ll-stat-meta">All registered accounts</span>
        </a>
        <a class="ll-stat-card" href="${ctx}/admin/teachers">
            <span class="ll-stat-label">Pending teachers</span>
            <strong class="ll-stat-value"><c:out value="${overview.pendingTeacherCount}"/></strong>
            <span class="ll-stat-meta">Awaiting administrator review</span>
        </a>
        <a class="ll-stat-card" href="${ctx}/admin/subjects">
            <span class="ll-stat-label">Subjects</span>
            <strong class="ll-stat-value"><c:out value="${overview.subjectCount}"/></strong>
            <span class="ll-stat-meta">Subject catalogue</span>
        </a>
        <a class="ll-stat-card" href="${ctx}/admin/classes">
            <span class="ll-stat-label">Classes</span>
            <strong class="ll-stat-value"><c:out value="${overview.classCount}"/></strong>
            <span class="ll-stat-meta">Across all teachers</span>
        </a>
        <div class="ll-stat-card ll-stat-card--static">
            <span class="ll-stat-label">Join requests</span>
            <strong class="ll-stat-value"><c:out value="${overview.joinRequestCount}"/></strong>
            <span class="ll-stat-meta"><c:out value="${overview.pendingJoinRequestCount}"/> currently pending</span>
        </div>
    </section>

    <div class="row g-4 mt-1">
        <div class="col-xl-7">
            <section class="ll-panel h-100" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Approval queue</span>
                        <h2>Pending teacher registrations</h2>
                    </div>
                    <a class="btn ll-btn-outline btn-sm" href="${ctx}/admin/teachers">Open approvals</a>
                </div>

                <c:choose>
                    <c:when test="${not empty overview.pendingTeachers}">
                        <ul class="ll-record-list">
                            <c:forEach items="${overview.pendingTeachers}" var="teacher">
                                <li class="ll-record">
                                    <div class="ll-record-main">
                                        <strong><c:out value="${teacher.fullName}"/></strong>
                                        <span><c:out value="${teacher.email}"/></span>
                                    </div>
                                    <div class="ll-record-side">
                                        <span class="ll-state ll-state-pending">PENDING</span>
                                        <time><fmt:formatDate value="${teacher.createdAt}" pattern="d MMM yyyy"/></time>
                                    </div>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:when>
                    <c:otherwise>
                        <c:set var="emptyTitle" value="No teachers waiting for approval"/>
                        <c:set var="emptyText" value="New teacher registrations will appear here for review."/>
                        <c:set var="emptyCtaHref" value=""/>
                        <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
                    </c:otherwise>
                </c:choose>
            </section>
        </div>

        <div class="col-xl-5">
            <section class="ll-panel h-100" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Shortcuts</span>
                        <h2>Administrative views</h2>
                    </div>
                </div>
                <div class="ll-quick-links">
                    <a class="ll-quick-link" href="${ctx}/admin/teachers">
                        <strong>Teacher approvals</strong>
                        <span>Review teacher account status</span>
                    </a>
                    <a class="ll-quick-link" href="${ctx}/admin/subjects">
                        <strong>Subject management</strong>
                        <span>Subjects and teacher coverage</span>
                    </a>
                    <a class="ll-quick-link" href="${ctx}/admin/assignments">
                        <strong>Assignments</strong>
                        <span>Teacher-to-subject authorisations</span>
                    </a>
                    <a class="ll-quick-link" href="${ctx}/admin/users">
                        <strong>User directory</strong>
                        <span>All accounts and approval states</span>
                    </a>
                    <a class="ll-quick-link" href="${ctx}/admin/classes">
                        <strong>Class overview</strong>
                        <span>Enrollment and request totals</span>
                    </a>
                </div>
            </section>
        </div>
    </div>
</c:if>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
