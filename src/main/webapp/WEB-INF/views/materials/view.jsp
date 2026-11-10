<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<c:if test="${not empty material}">
    <section class="ll-panel ll-material-detail" data-reveal>
        <div class="ll-panel-head">
            <div>
                <span class="ll-panel-kicker">
                    <c:out value="${material.typeLabel}"/>
                    &middot; <code><c:out value="${material.subjectCode}"/></code>
                </span>
                <h2><c:out value="${material.title}"/></h2>
                <p class="ll-section-copy">
                    <c:out value="${material.classTitle}"/>
                    &middot; <c:out value="${material.teacherName}"/>
                </p>
            </div>
            <div class="ll-class-detail-actions">
                <span class="ll-state ${material.published ? 'll-state-approved' : 'll-state-pending'}">
                    ${material.published ? 'Published' : 'Draft'}
                </span>
                <a class="btn ll-btn-outline btn-sm" href="${backUrl}">Back to classroom</a>
            </div>
        </div>

        <c:if test="${not empty material.description}">
            <p class="ll-class-description"><c:out value="${material.description}"/></p>
        </c:if>

        <div class="ll-material-detail-body">
            <c:choose>
                <c:when test="${material.link}">
                    <p class="ll-form-note">
                        This material opens an external website in a new tab. LearnLoop does not control the
                        content hosted there.
                    </p>
                    <c:choose>
                        <c:when test="${not empty material.safeResourceUrl}">
                            <a class="btn ll-btn-primary" href="<c:out value='${material.safeResourceUrl}'/>"
                               target="_blank" rel="noopener noreferrer">Open external resource</a>
                            <p class="ll-material-url"><code><c:out
                                    value="${material.safeResourceUrl}"/></code></p>
                        </c:when>
                        <c:otherwise>
                            <div class="alert alert-warning ll-alert" role="alert">
                                This resource link is not a valid web address.
                            </div>
                        </c:otherwise>
                    </c:choose>
                </c:when>

                <c:when test="${material.file}">
                    <p class="ll-form-note">
                        <c:out value="${material.fileName}"/>
                        <c:if test="${not empty material.fileSizeBytes}">
                            &middot; <c:out value="${material.fileSizeLabel}"/>
                        </c:if>
                    </p>
                    <c:choose>
                        <c:when test="${not empty material.storedName}">
                            <a class="btn ll-btn-primary" href="${ctx}/materials/${material.id}/download">
                                Download file
                            </a>
                        </c:when>
                        <c:otherwise>
                            <div class="alert alert-warning ll-alert" role="alert">
                                The stored file is no longer available. Ask your teacher to upload it again.
                            </div>
                        </c:otherwise>
                    </c:choose>
                </c:when>

                <c:when test="${material.text}">
                    <div class="ll-material-reading"><c:out value="${material.contentText}"/></div>
                </c:when>
            </c:choose>
        </div>

        <dl class="ll-material-meta">
            <div>
                <dt>Added</dt>
                <dd><fmt:formatDate value="${material.createdAt}" pattern="d MMM yyyy, HH:mm"/></dd>
            </div>
            <c:if test="${not empty material.publishedAt}">
                <div>
                    <dt>Published</dt>
                    <dd><fmt:formatDate value="${material.publishedAt}" pattern="d MMM yyyy, HH:mm"/></dd>
                </div>
            </c:if>
            <div>
                <dt>Last updated</dt>
                <dd><fmt:formatDate value="${material.updatedAt}" pattern="d MMM yyyy, HH:mm"/></dd>
            </div>
        </dl>
    </section>
</c:if>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
