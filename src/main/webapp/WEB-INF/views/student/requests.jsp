<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<section data-reveal>
    <div class="ll-section-head">
        <div>
            <span class="ll-panel-kicker">Enrollment</span>
            <h2>My join requests</h2>
            <p class="ll-section-copy">Track every request you have submitted and the teacher's latest decision.</p>
        </div>
        <a class="btn ll-btn-primary btn-sm" href="${ctx}/student/discover">Discover classes</a>
    </div>

    <c:choose>
        <c:when test="${not empty requests}">
            <div class="ll-panel">
                <div class="table-responsive">
                    <table class="table ll-table align-middle mb-0">
                        <thead>
                        <tr>
                            <th scope="col">Class</th>
                            <th scope="col">Subject</th>
                            <th scope="col">Teacher</th>
                            <th scope="col">Status</th>
                            <th scope="col">Requested</th>
                            <th scope="col">Reviewed</th>
                            <th scope="col">Your message</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${requests}" var="joinRequest">
                            <tr>
                                <td>
                                    <strong><c:out value="${joinRequest.classTitle}"/></strong>
                                    <c:if test="${joinRequest.classStatus != 'ACTIVE'}">
                                        <span class="ll-cell-note">Class is no longer active</span>
                                    </c:if>
                                </td>
                                <td>
                                    <c:out value="${joinRequest.subjectCode}"/>
                                    <span class="ll-cell-note"><c:out value="${joinRequest.subjectName}"/></span>
                                </td>
                                <td><c:out value="${joinRequest.teacherName}"/></td>
                                <td>
                                    <span class="ll-state ${joinRequest.statusCssClass}">
                                        <c:out value="${joinRequest.statusLabel}"/>
                                    </span>
                                </td>
                                <td><fmt:formatDate value="${joinRequest.createdAt}" pattern="d MMM yyyy, HH:mm"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty joinRequest.reviewedAt}">
                                            <fmt:formatDate value="${joinRequest.reviewedAt}" pattern="d MMM yyyy, HH:mm"/>
                                        </c:when>
                                        <c:otherwise><span class="ll-cell-note">Awaiting review</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="ll-table-message">
                                    <c:choose>
                                        <c:when test="${not empty joinRequest.message}">
                                            <c:out value="${joinRequest.message}"/>
                                        </c:when>
                                        <c:otherwise><span class="ll-cell-note">No message supplied</span></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <section class="ll-panel">
                <c:set var="emptyTitle" value="No join requests yet"/>
                <c:set var="emptyText" value="Use an invitation link from a teacher or browse discovery to request a place in a class."/>
                <c:set var="emptyCtaHref" value="${ctx}/student/discover"/>
                <c:set var="emptyCtaLabel" value="Discover classes"/>
                <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
            </section>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
