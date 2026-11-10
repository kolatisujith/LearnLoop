<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<section data-reveal>
    <div class="ll-section-head">
        <div>
            <span class="ll-panel-kicker">Study resources</span>
            <h2>Learning materials</h2>
            <p class="ll-section-copy">Published materials from your enrolled classes.</p>
        </div>
        <a class="btn ll-btn-outline btn-sm" href="${ctx}/student/classes">My classes</a>
    </div>

    <c:choose>
        <c:when test="${not empty materials}">
            <div class="ll-material-grid">
                <c:forEach items="${materials}" var="material">
                    <article class="ll-material-card">
                        <div class="ll-class-card-top">
                            <span class="ll-role-tag"><c:out value="${material.materialType}"/></span>
                            <span class="ll-state ll-state-neutral"><c:out value="${material.subjectCode}"/></span>
                        </div>
                        <h3><c:out value="${material.title}"/></h3>
                        <p class="ll-class-teacher"><c:out value="${material.classTitle}"/></p>
                        <c:if test="${not empty material.description}">
                            <p class="ll-class-description"><c:out value="${material.description}"/></p>
                        </c:if>
                        <c:if test="${not empty material.contentText}">
                            <div class="ll-material-content"><c:out value="${material.contentText}"/></div>
                        </c:if>
                        <div class="ll-material-foot">
                            <a class="btn ll-btn-outline btn-sm" href="${ctx}/materials/${material.id}">
                                Open material
                            </a>
                            <time><fmt:formatDate value="${material.createdAt}" pattern="d MMM yyyy"/></time>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </c:when>
        <c:otherwise>
            <section class="ll-panel">
                <c:set var="emptyTitle" value="No learning materials yet"/>
                <c:set var="emptyText" value="Published materials from your enrolled classes will appear here."/>
                <c:set var="emptyCtaHref" value=""/>
                <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
            </section>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
