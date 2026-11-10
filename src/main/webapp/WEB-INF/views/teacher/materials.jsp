<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<section class="ll-panel" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Resources</span>
            <h2>Materials in your classes</h2>
        </div>
        <a class="btn ll-btn-outline btn-sm" href="${ctx}/teacher/assessments">View assessments</a>
    </div>

    <c:choose>
        <c:when test="${not empty materials}">
            <div class="table-responsive">
                <table class="table ll-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th scope="col">Material</th>
                        <th scope="col">Class</th>
                        <th scope="col">Type</th>
                        <th scope="col">Visibility</th>
                        <th scope="col">Added</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${materials}" var="material">
                        <tr>
                            <td><strong><c:out value="${material.title}"/></strong></td>
                            <td>
                                <c:out value="${material.classTitle}"/>
                                <span class="ll-cell-note"><c:out value="${material.subjectCode}"/></span>
                            </td>
                            <td><span class="ll-state ll-state-neutral"><c:out value="${material.materialType}"/></span></td>
                            <td>
                                <span class="ll-state ${material.published ? 'll-state-approved' : 'll-state-pending'}">
                                    ${material.published ? 'PUBLISHED' : 'HIDDEN'}
                                </span>
                            </td>
                            <td><fmt:formatDate value="${material.createdAt}" pattern="d MMM yyyy"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:otherwise>
            <c:set var="emptyTitle" value="No class materials"/>
            <c:set var="emptyText" value="Materials attached to your classes will appear here."/>
            <c:set var="emptyCtaHref" value=""/>
            <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
