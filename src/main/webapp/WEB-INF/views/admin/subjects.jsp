<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<div class="row g-4">
    <div class="col-xl-4">
        <section class="ll-panel h-100" data-reveal>
            <div class="ll-panel-head">
                <div>
                    <span class="ll-panel-kicker">Catalogue</span>
                    <h2>Create a subject</h2>
                </div>
            </div>
            <form method="post" action="${ctx}/admin/subjects" class="ll-stack-form">
                <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                <input type="hidden" name="action" value="create">
                <div>
                    <label class="form-label" for="newSubjectCode">Code</label>
                    <input class="form-control ${not empty fieldErrors.code ? 'is-invalid' : ''}"
                           id="newSubjectCode" name="code" maxlength="20" required
                           value="<c:out value='${formCode}'/>" placeholder="e.g. BIO">
                </div>
                <div>
                    <label class="form-label" for="newSubjectName">Name</label>
                    <input class="form-control ${not empty fieldErrors.name ? 'is-invalid' : ''}"
                           id="newSubjectName" name="name" maxlength="120" required
                           value="<c:out value='${formName}'/>" placeholder="e.g. Biology">
                </div>
                <div>
                    <label class="form-label" for="newSubjectDescription">Description</label>
                    <textarea class="form-control" id="newSubjectDescription" name="description"
                              rows="4" maxlength="4000"
                              placeholder="A short description for teachers and students."><c:out value="${formDescription}"/></textarea>
                </div>
                <button class="btn ll-btn-primary" type="submit">Create subject</button>
            </form>
        </section>
    </div>

    <div class="col-xl-8">
        <section class="ll-panel h-100" data-reveal>
            <div class="ll-panel-head">
                <div>
                    <span class="ll-panel-kicker">Subjects</span>
                    <h2>Subjects and coverage</h2>
                </div>
                <a class="btn ll-btn-outline btn-sm" href="${ctx}/admin/assignments">View assignments</a>
            </div>

            <c:set var="pageResult" value="${subjects}"/>
            <c:set var="adminSearchPath" value="/admin/subjects"/>
            <c:set var="adminSearchPlaceholder" value="Search by code, name or description"/>
            <%@ include file="/WEB-INF/views/common/admin-search.jspf" %>

            <c:choose>
                <c:when test="${not empty subjects.items}">
                    <div class="table-responsive">
                        <table class="table ll-table align-middle mb-0">
                            <thead>
                            <tr>
                                <th scope="col">Code</th>
                                <th scope="col">Subject</th>
                                <th scope="col">State</th>
                                <th scope="col">Teachers</th>
                                <th scope="col">Classes</th>
                                <th scope="col" class="ll-table-actions">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${subjects.items}" var="subject">
                                <tr>
                                    <td><code><c:out value="${subject.code}"/></code></td>
                                    <td>
                                        <strong><c:out value="${subject.name}"/></strong>
                                        <c:if test="${not empty subject.description}">
                                            <span class="ll-cell-note"><c:out value="${subject.description}"/></span>
                                        </c:if>
                                    </td>
                                    <td>
                                        <span class="ll-state ${subject.active ? 'll-state-approved' : 'll-state-neutral'}">
                                            ${subject.active ? 'ACTIVE' : 'INACTIVE'}
                                        </span>
                                    </td>
                                    <td><c:out value="${subject.teacherCount}"/></td>
                                    <td><c:out value="${subject.classCount}"/></td>
                                    <td class="ll-table-actions">
                                        <div class="ll-action-stack">
                                            <form method="post" action="${ctx}/admin/subjects" class="ll-inline-form">
                                                <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                                <input type="hidden" name="subjectId" value="${subject.id}">
                                                <c:choose>
                                                    <c:when test="${subject.active}">
                                                        <input type="hidden" name="action" value="deactivate">
                                                        <button class="btn btn-outline-secondary btn-sm" type="submit">Deactivate</button>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <input type="hidden" name="action" value="activate">
                                                        <button class="btn ll-btn-outline btn-sm" type="submit">Activate</button>
                                                    </c:otherwise>
                                                </c:choose>
                                            </form>

                                            <details class="ll-inline-editor">
                                                <summary>Edit details</summary>
                                                <form method="post" action="${ctx}/admin/subjects" class="ll-stack-form ll-stack-form--compact">
                                                    <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                                    <input type="hidden" name="action" value="update">
                                                    <input type="hidden" name="subjectId" value="${subject.id}">
                                                    <div>
                                                        <label class="form-label" for="subjectCode${subject.id}">Code</label>
                                                        <input class="form-control form-control-sm" id="subjectCode${subject.id}"
                                                               name="code" maxlength="20" required value="<c:out value='${subject.code}'/>">
                                                    </div>
                                                    <div>
                                                        <label class="form-label" for="subjectName${subject.id}">Name</label>
                                                        <input class="form-control form-control-sm" id="subjectName${subject.id}"
                                                               name="name" maxlength="120" required value="<c:out value='${subject.name}'/>">
                                                    </div>
                                                    <div>
                                                        <label class="form-label" for="subjectDescription${subject.id}">Description</label>
                                                        <textarea class="form-control form-control-sm" id="subjectDescription${subject.id}"
                                                                  name="description" rows="3" maxlength="4000"><c:out value="${subject.description}"/></textarea>
                                                    </div>
                                                    <button class="btn ll-btn-primary btn-sm" type="submit">Save changes</button>
                                                </form>
                                            </details>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <c:set var="emptyTitle" value="No subjects found"/>
                    <c:set var="emptyText" value="Create a subject to start assigning teachers and building classes."/>
                    <c:set var="emptyCtaHref" value=""/>
                    <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
                </c:otherwise>
            </c:choose>

            <%@ include file="/WEB-INF/views/common/pagination.jspf" %>
        </section>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
