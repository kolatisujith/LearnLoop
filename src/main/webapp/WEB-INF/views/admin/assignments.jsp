<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<section class="ll-panel" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">New authorisation</span>
            <h2>Assign a teacher to a subject</h2>
        </div>
        <a class="btn ll-btn-outline btn-sm" href="${ctx}/admin/subjects">Manage subjects</a>
    </div>

    <c:choose>
        <c:when test="${empty approvedTeachers or empty activeSubjects}">
            <div class="alert alert-info ll-alert" role="status">
                Approve a teacher and keep at least one subject active before creating an assignment.
            </div>
        </c:when>
        <c:otherwise>
            <form method="post" action="${ctx}/admin/assignments" class="ll-assignment-form">
                <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                <input type="hidden" name="action" value="assign">
                <div>
                    <label class="form-label" for="assignmentTeacher">Approved teacher</label>
                    <select class="form-select" id="assignmentTeacher" name="teacherId" required>
                        <option value="">Choose a teacher</option>
                        <c:forEach items="${approvedTeachers}" var="teacher">
                            <option value="${teacher.id}">
                                <c:out value="${teacher.fullName}"/> - <c:out value="${teacher.email}"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div>
                    <label class="form-label" for="assignmentSubject">Active subject</label>
                    <select class="form-select" id="assignmentSubject" name="subjectId" required>
                        <option value="">Choose a subject</option>
                        <c:forEach items="${activeSubjects}" var="subject">
                            <option value="${subject.id}">
                                <c:out value="${subject.code}"/> - <c:out value="${subject.name}"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <button class="btn ll-btn-primary" type="submit">Assign teacher</button>
            </form>
        </c:otherwise>
    </c:choose>
</section>

<section class="ll-panel mt-4" data-reveal>
    <div class="ll-panel-head">
        <div>
            <span class="ll-panel-kicker">Authorisation</span>
            <h2>Teacher-subject assignments</h2>
        </div>
    </div>

    <c:set var="pageResult" value="${assignments}"/>
    <c:set var="adminSearchPath" value="/admin/assignments"/>
    <c:set var="adminSearchPlaceholder" value="Search by teacher or subject"/>
    <%@ include file="/WEB-INF/views/common/admin-search.jspf" %>

    <c:choose>
        <c:when test="${not empty assignments.items}">
            <div class="table-responsive">
                <table class="table ll-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th scope="col">Teacher</th>
                        <th scope="col">Subject</th>
                        <th scope="col">State</th>
                        <th scope="col">Assigned</th>
                        <th scope="col" class="ll-table-actions">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${assignments.items}" var="assignment">
                        <tr>
                            <td>
                                <strong><c:out value="${assignment.teacherName}"/></strong>
                                <span class="ll-cell-note"><c:out value="${assignment.teacherEmail}"/></span>
                            </td>
                            <td>
                                <code><c:out value="${assignment.subjectCode}"/></code>
                                <span class="ll-cell-note"><c:out value="${assignment.subjectName}"/></span>
                            </td>
                            <td>
                                <span class="ll-state ${assignment.active ? 'll-state-approved' : 'll-state-neutral'}">
                                    ${assignment.active ? 'ACTIVE' : 'INACTIVE'}
                                </span>
                            </td>
                            <td><fmt:formatDate value="${assignment.assignedAt}" pattern="d MMM yyyy"/></td>
                            <td class="ll-table-actions">
                                <c:choose>
                                    <c:when test="${assignment.active}">
                                        <form method="post" action="${ctx}/admin/assignments" class="ll-inline-form">
                                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                            <input type="hidden" name="action" value="remove">
                                            <input type="hidden" name="teacherId" value="${assignment.teacherUserId}">
                                            <input type="hidden" name="subjectId" value="${assignment.subjectId}">
                                            <button class="btn btn-outline-danger btn-sm" type="submit">Remove</button>
                                        </form>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="ll-cell-note">Inactive</span>
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
            <c:set var="emptyTitle" value="No teacher assignments found"/>
            <c:set var="emptyText" value="Use the form above to authorise an approved teacher for a subject."/>
            <c:set var="emptyCtaHref" value=""/>
            <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
        </c:otherwise>
    </c:choose>

    <%@ include file="/WEB-INF/views/common/pagination.jspf" %>
</section>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
