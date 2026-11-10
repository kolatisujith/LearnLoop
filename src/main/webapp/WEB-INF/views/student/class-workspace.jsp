<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<c:if test="${not empty classroom}">
    <c:set var="classroomUrl" value="${ctx}/student/classes/${classroom.id}"/>

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
                <span class="ll-state ll-state-approved">Enrolled</span>
                <a class="btn ll-btn-outline btn-sm" href="${ctx}/student/classes">Back to classes</a>
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
                <span class="ll-stat-label">Classmates</span>
                <strong>
                    <c:out value="${classroom.enrolledCount}"/>
                    <c:if test="${not empty classroom.maxStudents}">
                        of <c:out value="${classroom.maxStudents}"/>
                    </c:if>
                </strong>
            </div>
            <div>
                <span class="ll-stat-label">Live session</span>
                <strong>${classroom.liveSessionConfigured ? 'Available' : 'Not scheduled'}</strong>
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
                        <span class="ll-panel-kicker">Study resources</span>
                        <h2>Published materials</h2>
                    </div>
                </div>
                <c:choose>
                    <c:when test="${empty classroom.materials}">
                        <div class="ll-empty-state">
                            <p>Your teacher has not published any materials for this class yet.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="ll-material-grid">
                            <c:forEach items="${classroom.materials}" var="material">
                                <article class="ll-material-card">
                                    <div class="ll-class-card-top">
                                        <span class="ll-role-tag"><c:out value="${material.typeLabel}"/></span>
                                        <span class="ll-state ll-state-neutral"><c:out
                                                value="${material.subjectCode}"/></span>
                                    </div>
                                    <h3><c:out value="${material.title}"/></h3>
                                    <c:if test="${not empty material.description}">
                                        <p class="ll-class-description"><c:out
                                                value="${material.description}"/></p>
                                    </c:if>
                                    <c:if test="${not empty material.fileName}">
                                        <p class="ll-cell-note">
                                            <c:out value="${material.fileName}"/>
                                            <c:if test="${not empty material.fileSizeBytes}">
                                                &middot; <c:out value="${material.fileSizeLabel}"/>
                                            </c:if>
                                        </p>
                                    </c:if>
                                    <div class="ll-material-foot">
                                        <a class="btn ll-btn-outline btn-sm"
                                           href="${ctx}/materials/${material.id}">Open material</a>
                                        <time><fmt:formatDate value="${material.publishedAt}"
                                                              pattern="d MMM yyyy"/></time>
                                    </div>
                                </article>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:when>

        <c:when test="${tab == 'assessments'}">
            <section class="ll-panel" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Assessments</span>
                        <h2>Available checks and graded work</h2>
                    </div>
                </div>
                <c:choose>
                    <c:when test="${empty classroom.assessments}">
                        <div class="ll-empty-state">
                            <p>No assessments are published for this class yet.</p>
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
                                        <td>
                                            <strong>
                                                <a href="${ctx}/student/assessments/${assessment.id}"><c:out
                                                        value="${assessment.title}"/></a>
                                            </strong>
                                            <c:if test="${not empty assessment.instructions}">
                                                <div class="ll-muted"><c:out
                                                        value="${assessment.instructions}"/></div>
                                            </c:if>
                                        </td>
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
                        <span class="ll-panel-kicker">Live class</span>
                        <h2>Join your live session</h2>
                    </div>
                </div>
                <c:choose>
                    <c:when test="${classroom.liveSessionConfigured and classroom.jitsiSession}">
                        <p class="ll-form-note">Your teacher has created an in-platform video room. Join here from LearnLoop and allow your browser to use your camera and microphone if you want to speak or appear on video.</p>
                        <div class="ll-live-link">
                            <a class="btn ll-btn-primary" href="${ctx}/meetings/${classroom.id}">Join in-platform live class</a>
                            <span class="ll-state ll-state-approved">Room ready</span>
                        </div>
                    </c:when>
                    <c:when test="${classroom.liveSessionConfigured}">
                        <p class="ll-form-note">Your teacher has shared an external Google Meet link. The meeting will open in a new browser tab.</p>
                        <div class="ll-live-link">
                            <a class="btn ll-btn-primary" href="${ctx}/meetings/${classroom.id}" target="_blank" rel="noopener noreferrer">Open Google Meet</a>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="ll-empty-state">
                            <p>Your teacher has not configured a live session for this class yet.</p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:when>
        <c:otherwise>
            <section class="ll-panel" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Overview</span>
                        <h2>Learning with <c:out value="${classroom.teacherName}"/></h2>
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
                        <dd><c:out value="${classroom.materialCount}"/> published</dd>
                    </div>
                    <div>
                        <dt>Assessments</dt>
                        <dd><c:out value="${classroom.assessmentCount}"/> available</dd>
                    </div>
                    <div>
                        <dt>Live session</dt>
                        <dd>${classroom.liveSessionConfigured ? 'Link available' : 'Not scheduled'}</dd>
                    </div>
                </dl>
                <p class="ll-form-note">
                    Use the tabs above to open materials, review assessments or join a live session.
                </p>
            </section>
        </c:otherwise>
    </c:choose>
</c:if>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
