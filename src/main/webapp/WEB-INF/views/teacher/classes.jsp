<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<div class="row g-4">
    <div class="col-xl-4">
        <section class="ll-panel h-100" data-reveal>
            <div class="ll-panel-head">
                <div>
                    <span class="ll-panel-kicker">New class</span>
                    <h2>Create a class</h2>
                </div>
            </div>

            <c:choose>
                <c:when test="${empty assignedSubjects}">
                    <div class="alert alert-info ll-alert" role="status">
                        An administrator must assign you to at least one active subject before you can create a class.
                    </div>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${ctx}/teacher/classes" class="ll-stack-form">
                        <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                        <input type="hidden" name="action" value="create">

                        <div>
                            <label class="form-label" for="classTitle">Class name</label>
                            <input class="form-control ${not empty fieldErrors.title ? 'is-invalid' : ''}"
                                   id="classTitle" name="title" maxlength="160" required
                                   value="<c:out value='${formTitle}'/>"
                                   placeholder="e.g. Algebra Foundations">
                            <div class="invalid-feedback"><c:out value="${fieldErrors.title}"/></div>
                        </div>

                        <div>
                            <label class="form-label" for="classSubject">Assigned subject</label>
                            <select class="form-select ${not empty fieldErrors.subjectId ? 'is-invalid' : ''}"
                                    id="classSubject" name="subjectId" required>
                                <option value="">Choose a subject</option>
                                <c:forEach items="${assignedSubjects}" var="subject">
                                    <option value="${subject.id}" ${formSubjectId == subject.id ? 'selected' : ''}>
                                        <c:out value="${subject.code}"/> - <c:out value="${subject.name}"/>
                                    </option>
                                </c:forEach>
                            </select>
                            <div class="invalid-feedback"><c:out value="${fieldErrors.subjectId}"/></div>
                        </div>

                        <div>
                            <label class="form-label" for="classCapacity">
                                Capacity <span class="ll-optional">optional</span>
                            </label>
                            <input class="form-control ${not empty fieldErrors.maxStudents ? 'is-invalid' : ''}"
                                   id="classCapacity" name="maxStudents" type="number" min="1" inputmode="numeric"
                                   value="<c:out value='${formMaxStudents}'/>"
                                   placeholder="Leave blank for no limit">
                            <div class="invalid-feedback"><c:out value="${fieldErrors.maxStudents}"/></div>
                        </div>

                        <div>
                            <label class="form-label" for="classDescription">
                                Description <span class="ll-optional">optional</span>
                            </label>
                            <textarea class="form-control ${not empty fieldErrors.description ? 'is-invalid' : ''}"
                                      id="classDescription" name="description" rows="5" maxlength="4000"
                                      placeholder="What will students learn in this class?"><c:out value="${formDescription}"/></textarea>
                            <div class="invalid-feedback"><c:out value="${fieldErrors.description}"/></div>
                        </div>

                        <p class="ll-form-note">New classes start as drafts. You can activate the class from its details page.</p>
                        <button class="btn ll-btn-primary" type="submit">Create draft class</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <div class="col-xl-8">
        <section class="ll-panel h-100" data-reveal>
            <div class="ll-panel-head">
                <div>
                    <span class="ll-panel-kicker">Classes</span>
                    <h2>Classes you created</h2>
                </div>
                <a class="btn ll-btn-outline btn-sm" href="${ctx}/teacher/requests">Join requests</a>
            </div>

            <c:choose>
                <c:when test="${not empty classes}">
                    <div class="table-responsive">
                        <table class="table ll-table align-middle mb-0">
                            <thead>
                            <tr>
                                <th scope="col">Class</th>
                                <th scope="col">Subject</th>
                                <th scope="col">Status</th>
                                <th scope="col">Enrolled</th>
                                <th scope="col">Pending</th>
                                <th scope="col">Content</th>
                                <th scope="col">Created</th>
                                <th scope="col" class="ll-table-actions">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${classes}" var="learningClass">
                                <tr>
                                    <td><strong><c:out value="${learningClass.title}"/></strong></td>
                                    <td><code><c:out value="${learningClass.subjectCode}"/></code></td>
                                    <td>
                                        <span class="ll-state ${learningClass.status == 'ACTIVE' ? 'll-state-approved' : (learningClass.status == 'DRAFT' ? 'll-state-pending' : 'll-state-neutral')}">
                                            <c:out value="${learningClass.status}"/>
                                        </span>
                                    </td>
                                    <td>
                                        <c:out value="${learningClass.enrolledCount}"/>
                                        <c:if test="${not empty learningClass.maxStudents}">
                                            <span class="ll-cell-note">of <c:out value="${learningClass.maxStudents}"/></span>
                                        </c:if>
                                    </td>
                                    <td><c:out value="${learningClass.pendingRequestCount}"/></td>
                                    <td>
                                        <c:out value="${learningClass.materialCount}"/>
                                        <span class="ll-cell-note">
                                            <c:out value="${learningClass.assessmentCount}"/> assessments
                                        </span>
                                    </td>
                                    <td><fmt:formatDate value="${learningClass.createdAt}" pattern="d MMM yyyy"/></td>
                                    <td class="ll-table-actions">
                                        <a class="btn ll-btn-outline btn-sm"
                                           href="${ctx}/teacher/classes/${learningClass.id}">View details</a>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <c:set var="emptyTitle" value="No classes yet"/>
                    <c:set var="emptyText" value="Classes you create will appear here with enrollment and request totals."/>
                    <c:set var="emptyCtaHref" value=""/>
                    <%@ include file="/WEB-INF/views/common/empty-state.jspf" %>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
