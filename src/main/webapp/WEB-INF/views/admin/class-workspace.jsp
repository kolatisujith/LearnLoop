<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<c:if test="${not empty classroom}">
    <c:set var="classroomUrl" value="${ctx}/admin/classes/${classroom.id}"/>

    <section class="ll-panel ll-class-detail-hero" data-reveal>
        <div class="ll-panel-head">
            <div>
                <span class="ll-panel-kicker">
                    <code><c:out value="${classroom.subjectCode}"/></code>
                    <c:out value="${classroom.subjectName}"/>
                </span>
                <h2><c:out value="${classroom.title}"/></h2>
            </div>
            <div class="ll-class-detail-actions">
                <span class="ll-state ll-state-neutral"><c:out value="${classroom.status}"/></span>
                <a class="btn ll-btn-outline btn-sm" href="${ctx}/admin/classes">Back to classes</a>
            </div>
        </div>
        <p class="ll-class-description">
            <c:choose>
                <c:when test="${not empty classroom.description}">
                    <c:out value="${classroom.description}"/>
                </c:when>
                <c:otherwise>
                    <span class="ll-muted">No description has been added.</span>
                </c:otherwise>
            </c:choose>
        </p>
        <div class="ll-class-facts">
            <div>
                <span class="ll-stat-label">Teacher</span>
                <strong><c:out value="${classroom.teacherName}"/></strong>
            </div>
            <div>
                <span class="ll-stat-label">Enrolled</span>
                <strong>
                    <c:out value="${classroom.enrolledCount}"/>
                    <c:if test="${not empty classroom.maxStudents}">
                        of <c:out value="${classroom.maxStudents}"/>
                    </c:if>
                </strong>
            </div>
            <div>
                <span class="ll-stat-label">Live session</span>
                <strong>${classroom.liveSessionConfigured ? 'Configured' : 'Not configured'}</strong>
            </div>
        </div>
    </section>

    <nav class="ll-workspace-tabs" aria-label="Class workspace sections">
        <a class="ll-workspace-tab ${tab == 'overview' ? 'is-active' : ''}"
           href="${classroomUrl}?tab=overview">Overview</a>
        <a class="ll-workspace-tab ${tab == 'materials' ? 'is-active' : ''}"
           href="${classroomUrl}?tab=materials">Materials
            <span class="ll-tab-count"><c:out value="${classroom.materialCount}"/></span></a>
        <a class="ll-workspace-tab ${tab == 'assessments' ? 'is-active' : ''}"
           href="${classroomUrl}?tab=assessments">Assessments
            <span class="ll-tab-count"><c:out value="${classroom.assessmentCount}"/></span></a>
        <a class="ll-workspace-tab ${tab == 'live' ? 'is-active' : ''}"
           href="${classroomUrl}?tab=live">Live session</a>
    </nav>

    <c:choose>
        <c:when test="${tab == 'materials'}">
            <section class="ll-panel" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Read-only inspection</span>
                        <h2>All class materials</h2>
                    </div>
                    <span class="ll-state ll-state-neutral">
                        <c:out value="${classroom.publishedMaterialCount}"/> of
                        <c:out value="${classroom.materialCount}"/> published
                    </span>
                </div>
                <c:choose>
                    <c:when test="${empty classroom.materials}">
                        <div class="ll-empty-state">
                            <p>This class has no materials yet.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-responsive">
                            <table class="table ll-table align-middle">
                                <thead>
                                <tr>
                                    <th scope="col">Material</th>
                                    <th scope="col">Type</th>
                                    <th scope="col">Visibility</th>
                                    <th scope="col">Added</th>
                                    <th scope="col"></th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach items="${classroom.materials}" var="material">
                                    <tr>
                                        <td>
                                            <strong><c:out value="${material.title}"/></strong>
                                            <c:if test="${not empty material.description}">
                                                <div class="ll-muted"><c:out
                                                        value="${material.description}"/></div>
                                            </c:if>
                                        </td>
                                        <td><c:out value="${material.typeLabel}"/></td>
                                        <td>
                                            <span class="ll-state ${material.published ? 'll-state-approved' : 'll-state-pending'}">
                                                ${material.published ? 'Published' : 'Draft'}
                                            </span>
                                        </td>
                                        <td><fmt:formatDate value="${material.createdAt}"
                                                            pattern="d MMM yyyy"/></td>
                                        <td>
                                            <a class="btn ll-btn-outline btn-sm"
                                               href="${ctx}/materials/${material.id}">Inspect</a>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:when>

        <c:when test="${tab == 'assessments'}">
            <section class="ll-panel" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Read-only inspection</span>
                        <h2>Assessments</h2>
                    </div>
                </div>
                <c:choose>
                    <c:when test="${empty classroom.assessments}">
                        <div class="ll-empty-state">
                            <p>This class has no assessments yet.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-responsive">
                            <table class="table ll-table align-middle">
                                <thead>
                                <tr>
                                    <th scope="col">Assessment</th>
                                    <th scope="col">Status</th>
                                    <th scope="col">Available</th>
                                    <th scope="col">Due</th>
                                    <th scope="col">Attempts</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach items="${classroom.assessments}" var="assessment">
                                    <tr>
                                        <td><strong><c:out value="${assessment.title}"/></strong></td>
                                        <td><span class="ll-state ll-state-neutral"><c:out
                                                value="${assessment.status}"/></span></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty assessment.availableFrom}">
                                                    <fmt:formatDate value="${assessment.availableFrom}"
                                                                    pattern="d MMM yyyy, HH:mm"/>
                                                </c:when>
                                                <c:otherwise>Open</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty assessment.dueAt}">
                                                    <fmt:formatDate value="${assessment.dueAt}"
                                                                    pattern="d MMM yyyy, HH:mm"/>
                                                </c:when>
                                                <c:otherwise>No due date</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><c:out value="${assessment.maxAttempts}"/></td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:when>

        <c:when test="${tab == 'live'}">
            <section class="ll-panel" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Read-only inspection</span>
                        <h2>Live session</h2>
                    </div>
                </div>
                <c:choose>
                    <c:when test="${classroom.liveSessionConfigured and classroom.jitsiSession}">
                        <p class="ll-form-note">The teacher configured an in-platform room embedded in LearnLoop using the Jitsi demo service.</p>
                        <div class="ll-live-link">
                            <a class="btn ll-btn-primary" href="${ctx}/meetings/${classroom.id}">Preview in-platform live room</a>
                            <span class="ll-state ll-state-approved">In-platform room</span>
                        </div>
                    </c:when>
                    <c:when test="${classroom.liveSessionConfigured}">
                        <p class="ll-form-note">The teacher configured an external Google Meet link for this class.</p>
                        <div class="ll-live-link">
                            <a class="btn ll-btn-primary" href="${ctx}/meetings/${classroom.id}" target="_blank" rel="noopener noreferrer">Open Google Meet</a>
                            <code><c:out value="${classroom.meetingUrl}"/></code>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="ll-empty-state"><p>No live session is configured for this class.</p></div>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:when>
        <c:otherwise>
            <section class="ll-panel" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Class summary</span>
                        <h2>Platform records</h2>
                    </div>
                </div>
                <dl class="ll-class-meta ll-class-meta--wide">
                    <div>
                        <dt>Subject</dt>
                        <dd><c:out value="${classroom.subjectCode}"/> &middot; <c:out
                                value="${classroom.subjectName}"/></dd>
                    </div>
                    <div>
                        <dt>Materials</dt>
                        <dd><c:out value="${classroom.materialCount}"/> total</dd>
                    </div>
                    <div>
                        <dt>Assessments</dt>
                        <dd><c:out value="${classroom.assessmentCount}"/> total</dd>
                    </div>
                    <div>
                        <dt>Created</dt>
                        <dd><fmt:formatDate value="${classroom.createdAt}" pattern="d MMM yyyy"/></dd>
                    </div>
                </dl>
                <p class="ll-form-note">
                    Administrator access is read-only. Teachers manage the class, its materials and its live
                    session link.
                </p>
            </section>
        </c:otherwise>
    </c:choose>
</c:if>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
