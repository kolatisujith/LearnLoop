<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/views/common/workspace-start.jspf" %>

<c:if test="${not empty classDetails}">
    <c:set var="editable"
           value="${classDetails.status != 'ARCHIVED' and classDetails.status != 'CANCELLED'}"/>
    <c:set var="deletable"
           value="${classDetails.status == 'DRAFT'
                    and classDetails.enrolledCount == 0
                    and classDetails.joinRequestCount == 0
                    and classDetails.materialCount == 0
                    and classDetails.assessmentCount == 0}"/>
    <c:set var="classUrl" value="${ctx}/teacher/classes/${classDetails.id}"/>

    <section class="ll-panel ll-class-detail-hero" data-reveal>
        <div class="ll-panel-head">
            <div>
                <span class="ll-panel-kicker">
                    <code><c:out value="${classDetails.subjectCode}"/></code>
                    <c:out value="${classDetails.subjectName}"/>
                </span>
                <h2><c:out value="${classDetails.title}"/></h2>
            </div>
            <div class="ll-class-detail-actions">
                <span class="ll-state ${classDetails.status == 'ACTIVE' ? 'll-state-approved' : (classDetails.status == 'DRAFT' ? 'll-state-pending' : 'll-state-neutral')}">
                    <c:out value="${classDetails.status}"/>
                </span>
                <a class="btn ll-btn-outline btn-sm" href="${ctx}/teacher/classes">Back to classes</a>
            </div>
        </div>

        <p class="ll-class-description">
            <c:choose>
                <c:when test="${not empty classDetails.description}">
                    <c:out value="${classDetails.description}"/>
                </c:when>
                <c:otherwise>
                    <span class="ll-muted">No description has been added.</span>
                </c:otherwise>
            </c:choose>
        </p>

        <div class="ll-class-facts">
            <div>
                <span class="ll-stat-label">Capacity</span>
                <strong>
                    <c:choose>
                        <c:when test="${not empty classDetails.maxStudents}">
                            <c:out value="${classDetails.maxStudents}"/> students
                        </c:when>
                        <c:otherwise>No limit</c:otherwise>
                    </c:choose>
                </strong>
            </div>
            <div>
                <span class="ll-stat-label">Created</span>
                <strong><fmt:formatDate value="${classDetails.createdAt}" pattern="d MMM yyyy"/></strong>
            </div>
            <div>
                <span class="ll-stat-label">Last updated</span>
                <strong><fmt:formatDate value="${classDetails.updatedAt}" pattern="d MMM yyyy, HH:mm"/></strong>
            </div>
        </div>
    </section>

    <nav class="ll-workspace-tabs" aria-label="Class workspace sections">
        <a class="ll-workspace-tab ${tab == 'overview' ? 'is-active' : ''}"
           href="${classUrl}?tab=overview">Overview</a>
        <a class="ll-workspace-tab ${tab == 'materials' ? 'is-active' : ''}"
           href="${classUrl}?tab=materials">Materials
            <span class="ll-tab-count"><c:out value="${classroom.materialCount}"/></span></a>
        <a class="ll-workspace-tab ${tab == 'assessments' ? 'is-active' : ''}"
           href="${classUrl}?tab=assessments">Assessments
            <span class="ll-tab-count"><c:out value="${classroom.assessmentCount}"/></span></a>
        <a class="ll-workspace-tab ${tab == 'live' ? 'is-active' : ''}"
           href="${classUrl}?tab=live">Live session</a>
    </nav>

    <c:choose>
        <c:when test="${tab == 'materials'}">
            <section class="ll-panel" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Class materials</span>
                        <h2>Share links, readings and files</h2>
                    </div>
                    <span class="ll-state ${classroom.publishedMaterialCount > 0 ? 'll-state-approved' : 'll-state-neutral'}">
                        <c:out value="${classroom.publishedMaterialCount}"/> published
                    </span>
                </div>

                <c:choose>
                    <c:when test="${not editable}">
                        <div class="alert alert-info ll-alert" role="status">
                            This class is <c:out value="${classDetails.status}"/> and materials can no longer
                            be changed.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <p class="ll-form-note">
                            New materials are saved as drafts. Publish a material when you are ready for enrolled
                            students to see it. Files are served only through an authorised download link.
                        </p>

                        <div class="ll-material-create-grid">
                            <details class="ll-material-create"
                                ${empty editingMaterialId and materialFormType == 'LINK' ? 'open' : ''}>
                                <summary>Add a link</summary>
                                <form method="post" action="${classUrl}?tab=materials" class="ll-stack-form">
                                    <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                    <input type="hidden" name="action" value="material-create">
                                    <input type="hidden" name="materialType" value="LINK">
                                    <div>
                                        <label class="form-label" for="newLinkTitle">Title</label>
                                        <input class="form-control ${not empty fieldErrors.title and empty editingMaterialId ? 'is-invalid' : ''}"
                                               id="newLinkTitle" name="title" maxlength="160" required
                                               value="<c:out value='${empty editingMaterialId ? materialFormTitle : null}'/>">
                                        <div class="invalid-feedback"><c:out value="${fieldErrors.title}"/></div>
                                    </div>
                                    <div>
                                        <label class="form-label" for="newLinkUrl">Web address</label>
                                        <input class="form-control ${not empty fieldErrors.resourceUrl ? 'is-invalid' : ''}"
                                               id="newLinkUrl" name="resourceUrl" maxlength="1000" required
                                               placeholder="https://example.com/resource"
                                               value="<c:out value='${empty editingMaterialId ? materialFormResourceUrl : null}'/>">
                                        <div class="invalid-feedback"><c:out value="${fieldErrors.resourceUrl}"/></div>
                                    </div>
                                    <div>
                                        <label class="form-label" for="newLinkDescription">
                                            Description <span class="ll-optional">optional</span>
                                        </label>
                                        <textarea class="form-control" id="newLinkDescription" name="description"
                                                  rows="3" maxlength="4000"><c:out value="${empty editingMaterialId ? materialFormDescription : ''}"/></textarea>
                                    </div>
                                    <button class="btn ll-btn-primary" type="submit">Add link draft</button>
                                </form>
                            </details>

                            <details class="ll-material-create"
                                ${empty editingMaterialId and materialFormType == 'TEXT' ? 'open' : ''}>
                                <summary>Add a reading</summary>
                                <form method="post" action="${classUrl}?tab=materials" class="ll-stack-form">
                                    <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                    <input type="hidden" name="action" value="material-create">
                                    <input type="hidden" name="materialType" value="TEXT">
                                    <div>
                                        <label class="form-label" for="newTextTitle">Title</label>
                                        <input class="form-control ${not empty fieldErrors.title and empty editingMaterialId ? 'is-invalid' : ''}"
                                               id="newTextTitle" name="title" maxlength="160" required
                                               value="<c:out value='${empty editingMaterialId ? materialFormTitle : null}'/>">
                                        <div class="invalid-feedback"><c:out value="${fieldErrors.title}"/></div>
                                    </div>
                                    <div>
                                        <label class="form-label" for="newTextContent">Reading content</label>
                                        <textarea class="form-control ${not empty fieldErrors.contentText ? 'is-invalid' : ''}"
                                                  id="newTextContent" name="contentText" rows="8" required><c:out value="${empty editingMaterialId ? materialFormContentText : ''}"/></textarea>
                                        <div class="invalid-feedback"><c:out value="${fieldErrors.contentText}"/></div>
                                    </div>
                                    <div>
                                        <label class="form-label" for="newTextDescription">
                                            Description <span class="ll-optional">optional</span>
                                        </label>
                                        <textarea class="form-control" id="newTextDescription" name="description"
                                                  rows="2" maxlength="4000"><c:out value="${empty editingMaterialId ? materialFormDescription : ''}"/></textarea>
                                    </div>
                                    <button class="btn ll-btn-primary" type="submit">Add reading draft</button>
                                </form>
                            </details>

                            <details class="ll-material-create"
                                ${empty editingMaterialId and materialFormType == 'FILE' ? 'open' : ''}>
                                <summary>Upload a file</summary>
                                <form method="post" action="${classUrl}?tab=materials" enctype="multipart/form-data"
                                      class="ll-stack-form">
                                    <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                    <input type="hidden" name="action" value="material-create">
                                    <input type="hidden" name="materialType" value="FILE">
                                    <div>
                                        <label class="form-label" for="newFileTitle">Title</label>
                                        <input class="form-control ${not empty fieldErrors.title and empty editingMaterialId ? 'is-invalid' : ''}"
                                               id="newFileTitle" name="title" maxlength="160" required
                                               value="<c:out value='${empty editingMaterialId ? materialFormTitle : null}'/>">
                                        <div class="invalid-feedback"><c:out value="${fieldErrors.title}"/></div>
                                    </div>
                                    <div>
                                        <label class="form-label" for="newFile">File</label>
                                        <input class="form-control ${not empty fieldErrors.file ? 'is-invalid' : ''}"
                                               id="newFile" name="file" type="file" required
                                               accept=".pdf,.txt,.md,.csv,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.odt,.ods,.odp,.png,.jpg,.jpeg,.gif,.webp,.zip">
                                        <div class="invalid-feedback"><c:out value="${fieldErrors.file}"/></div>
                                        <p class="ll-form-note">
                                            Maximum 10 MB. PDF, document, spreadsheet, presentation, image and ZIP
                                            files are accepted. HTML, scripts and executables are blocked.
                                        </p>
                                    </div>
                                    <div>
                                        <label class="form-label" for="newFileDescription">
                                            Description <span class="ll-optional">optional</span>
                                        </label>
                                        <textarea class="form-control" id="newFileDescription" name="description"
                                                  rows="3" maxlength="4000"><c:out value="${empty editingMaterialId ? materialFormDescription : ''}"/></textarea>
                                    </div>
                                    <button class="btn ll-btn-primary" type="submit">Upload file draft</button>
                                </form>
                            </details>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <section class="ll-panel" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Material library</span>
                        <h2>Materials in this class</h2>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${empty classroom.materials}">
                        <div class="ll-empty-state">
                            <p>No materials have been added yet.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="ll-material-list">
                            <c:forEach items="${classroom.materials}" var="material">
                                <c:set var="isEditing"
                                       value="${not empty editingMaterialId and editingMaterialId == material.id}"/>
                                <article class="ll-material-manage-card">
                                    <div class="ll-material-manage-head">
                                        <div>
                                            <span class="ll-material-type">
                                                <c:out value="${material.typeLabel}"/>
                                            </span>
                                            <h3><c:out value="${material.title}"/></h3>
                                        </div>
                                        <span class="ll-state ${material.published ? 'll-state-approved' : 'll-state-pending'}">
                                            ${material.published ? 'Published' : 'Draft'}
                                        </span>
                                    </div>

                                    <c:if test="${not empty material.description}">
                                        <p class="ll-material-description"><c:out value="${material.description}"/></p>
                                    </c:if>

                                    <c:choose>
                                        <c:when test="${material.link}">
                                            <p class="ll-form-note">
                                                Link:
                                                <a href="<c:out value='${material.safeResourceUrl}'/>"
                                                   target="_blank" rel="noopener noreferrer"><c:out
                                                        value="${material.safeResourceUrl}"/></a>
                                            </p>
                                        </c:when>
                                        <c:when test="${material.file}">
                                            <p class="ll-form-note">
                                                <c:out value="${material.fileName}"/>
                                                <c:if test="${not empty material.fileSizeBytes}">
                                                    &middot; <c:out value="${material.fileSizeLabel}"/>
                                                </c:if>
                                            </p>
                                        </c:when>
                                        <c:when test="${material.text}">
                                            <div class="ll-material-text-preview"><c:out
                                                    value="${material.contentText}"/></div>
                                        </c:when>
                                    </c:choose>

                                    <div class="ll-material-manage-actions">
                                        <a class="btn ll-btn-outline btn-sm" href="${ctx}/materials/${material.id}">
                                            View
                                        </a>
                                        <c:if test="${material.published}">
                                            <form method="post" action="${classUrl}?tab=materials">
                                                <input type="hidden" name="_csrf"
                                                       value="<c:out value='${sessionScope.csrfToken}'/>">
                                                <input type="hidden" name="action" value="material-unpublish">
                                                <input type="hidden" name="materialId" value="${material.id}">
                                                <button class="btn ll-btn-outline btn-sm" type="submit">
                                                    Unpublish
                                                </button>
                                            </form>
                                        </c:if>
                                        <c:if test="${not material.published}">
                                            <form method="post" action="${classUrl}?tab=materials">
                                                <input type="hidden" name="_csrf"
                                                       value="<c:out value='${sessionScope.csrfToken}'/>">
                                                <input type="hidden" name="action" value="material-publish">
                                                <input type="hidden" name="materialId" value="${material.id}">
                                                <button class="btn ll-btn-primary btn-sm" type="submit">
                                                    Publish
                                                </button>
                                            </form>
                                        </c:if>
                                        <c:if test="${editable}">
                                            <form method="post" action="${classUrl}?tab=materials"
                                                  data-confirm="Delete this material permanently? This cannot be undone.">
                                                <input type="hidden" name="_csrf"
                                                       value="<c:out value='${sessionScope.csrfToken}'/>">
                                                <input type="hidden" name="action" value="material-delete">
                                                <input type="hidden" name="materialId" value="${material.id}">
                                                <button class="btn btn-outline-danger btn-sm" type="submit">
                                                    Delete
                                                </button>
                                            </form>
                                        </c:if>
                                    </div>

                                    <c:if test="${editable}">
                                        <details class="ll-material-edit" ${isEditing ? 'open' : ''}>
                                            <summary>Edit material</summary>
                                            <form method="post" action="${classUrl}?tab=materials"
                                                  enctype="multipart/form-data" class="ll-stack-form">
                                                <input type="hidden" name="_csrf"
                                                       value="<c:out value='${sessionScope.csrfToken}'/>">
                                                <input type="hidden" name="action" value="material-update">
                                                <input type="hidden" name="materialId" value="${material.id}">

                                                <div>
                                                    <label class="form-label"
                                                           for="materialTitle${material.id}">Title</label>
                                                    <input class="form-control ${isEditing and not empty fieldErrors.title ? 'is-invalid' : ''}"
                                                           id="materialTitle${material.id}" name="title" maxlength="160"
                                                           required
                                                           value="<c:out value='${isEditing ? materialFormTitle : material.title}'/>">
                                                    <div class="invalid-feedback"><c:out value="${fieldErrors.title}"/></div>
                                                </div>

                                                <div>
                                                    <label class="form-label"
                                                           for="materialType${material.id}">Material type</label>
                                                    <select class="form-select ${isEditing and not empty fieldErrors.materialType ? 'is-invalid' : ''}"
                                                            id="materialType${material.id}" name="materialType" required>
                                                        <c:set var="selectedType"
                                                               value="${isEditing ? materialFormType : material.materialType}"/>
                                                        <option value="LINK" ${selectedType == 'LINK' ? 'selected' : ''}>
                                                            Link
                                                        </option>
                                                        <option value="TEXT" ${selectedType == 'TEXT' ? 'selected' : ''}>
                                                            Reading
                                                        </option>
                                                        <option value="FILE" ${selectedType == 'FILE' ? 'selected' : ''}>
                                                            File
                                                        </option>
                                                    </select>
                                                    <div class="invalid-feedback"><c:out
                                                            value="${fieldErrors.materialType}"/></div>
                                                </div>

                                                <div>
                                                    <label class="form-label" for="materialUrl${material.id}">
                                                        Web address <span class="ll-optional">for links</span>
                                                    </label>
                                                    <input class="form-control ${isEditing and not empty fieldErrors.resourceUrl ? 'is-invalid' : ''}"
                                                           id="materialUrl${material.id}" name="resourceUrl"
                                                           maxlength="1000"
                                                           value="<c:out value='${isEditing ? materialFormResourceUrl : material.resourceUrl}'/>">
                                                    <div class="invalid-feedback"><c:out
                                                            value="${fieldErrors.resourceUrl}"/></div>
                                                </div>

                                                <div>
                                                    <label class="form-label" for="materialText${material.id}">
                                                        Reading content <span class="ll-optional">for readings</span>
                                                    </label>
                                                    <textarea class="form-control ${isEditing and not empty fieldErrors.contentText ? 'is-invalid' : ''}"
                                                              id="materialText${material.id}" name="contentText"
                                                              rows="5"><c:out value="${isEditing ? materialFormContentText : material.contentText}"/></textarea>
                                                    <div class="invalid-feedback"><c:out
                                                            value="${fieldErrors.contentText}"/></div>
                                                </div>

                                                <div>
                                                    <label class="form-label" for="materialFile${material.id}">
                                                        Replace file <span class="ll-optional">for files</span>
                                                    </label>
                                                    <input class="form-control ${isEditing and not empty fieldErrors.file ? 'is-invalid' : ''}"
                                                           id="materialFile${material.id}" name="file" type="file"
                                                           accept=".pdf,.txt,.md,.csv,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.odt,.ods,.odp,.png,.jpg,.jpeg,.gif,.webp,.zip">
                                                    <div class="invalid-feedback"><c:out value="${fieldErrors.file}"/></div>
                                                    <p class="ll-form-note">
                                                        Leave blank to keep the current file. Only fields for the
                                                        selected material type are used.
                                                    </p>
                                                </div>

                                                <div>
                                                    <label class="form-label" for="materialDescription${material.id}">
                                                        Description <span class="ll-optional">optional</span>
                                                    </label>
                                                    <textarea class="form-control"
                                                              id="materialDescription${material.id}"
                                                              name="description" rows="3"
                                                              maxlength="4000"><c:out value="${isEditing ? materialFormDescription : material.description}"/></textarea>
                                                </div>

                                                <button class="btn ll-btn-primary" type="submit">Save material</button>
                                            </form>
                                        </details>
                                    </c:if>
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
                        <h2>Checks and graded work</h2>
                    </div>
                    <span class="ll-state ${classroom.assessmentCount > 0 ? 'll-state-approved' : 'll-state-neutral'}">
                        <c:out value="${classroom.assessmentCount}"/> total
                    </span>
                </div>

                <c:if test="${editable}">
                    <details class="ll-material-create" ${not empty fieldErrors ? 'open' : ''}>
                        <summary>Create an assessment</summary>
                        <form method="post" action="${classUrl}?tab=assessments" class="ll-stack-form">
                            <input type="hidden" name="_csrf"
                                   value="<c:out value='${sessionScope.csrfToken}'/>">
                            <input type="hidden" name="action" value="assessment-create">

                            <div>
                                <label class="form-label" for="newAssessmentTitle">Title</label>
                                <input class="form-control ${not empty fieldErrors.title ? 'is-invalid' : ''}"
                                       id="newAssessmentTitle" name="title" maxlength="200" required
                                       value="<c:out value='${assessmentFormTitle}'/>">
                                <div class="invalid-feedback"><c:out value="${fieldErrors.title}"/></div>
                            </div>

                            <div>
                                <label class="form-label" for="newAssessmentInstructions">
                                    Instructions <span class="ll-optional">optional</span>
                                </label>
                                <textarea class="form-control ${not empty fieldErrors.instructions ? 'is-invalid' : ''}"
                                          id="newAssessmentInstructions" name="instructions" rows="4"
                                          maxlength="4000"><c:out value="${assessmentFormInstructions}"/></textarea>
                                <div class="invalid-feedback"><c:out value="${fieldErrors.instructions}"/></div>
                            </div>

                            <div class="row g-3">
                                <div class="col-md-4">
                                    <label class="form-label" for="newAssessmentAvailableFrom">
                                        Available from <span class="ll-optional">optional</span>
                                    </label>
                                    <input class="form-control ${not empty fieldErrors.availableFrom ? 'is-invalid' : ''}"
                                           id="newAssessmentAvailableFrom" name="availableFrom"
                                           type="datetime-local"
                                           value="<c:out value='${assessmentFormAvailableFrom}'/>">
                                    <div class="invalid-feedback"><c:out value="${fieldErrors.availableFrom}"/></div>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label" for="newAssessmentDueAt">
                                        Due <span class="ll-optional">optional</span>
                                    </label>
                                    <input class="form-control ${not empty fieldErrors.dueAt ? 'is-invalid' : ''}"
                                           id="newAssessmentDueAt" name="dueAt" type="datetime-local"
                                           value="<c:out value='${assessmentFormDueAt}'/>">
                                    <div class="invalid-feedback"><c:out value="${fieldErrors.dueAt}"/></div>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label" for="newAssessmentTimeLimit">
                                        Time limit in minutes <span class="ll-optional">optional</span>
                                    </label>
                                    <input class="form-control ${not empty fieldErrors.timeLimitMinutes ? 'is-invalid' : ''}"
                                           id="newAssessmentTimeLimit" name="timeLimitMinutes"
                                           type="number" min="1" inputmode="numeric"
                                           value="<c:out value='${assessmentFormTimeLimitMinutes}'/>"
                                           placeholder="Untimed">
                                    <div class="invalid-feedback"><c:out value="${fieldErrors.timeLimitMinutes}"/></div>
                                </div>
                            </div>

                            <div class="row g-3">
                                <div class="col-md-4">
                                    <label class="form-label" for="newAssessmentMaxAttempts">
                                        Attempts allowed
                                    </label>
                                    <input class="form-control ${not empty fieldErrors.maxAttempts ? 'is-invalid' : ''}"
                                           id="newAssessmentMaxAttempts" name="maxAttempts" type="number"
                                           min="1" inputmode="numeric" required
                                           value="<c:out value='${not empty assessmentFormMaxAttempts ? assessmentFormMaxAttempts : "1"}'/>">
                                    <div class="invalid-feedback"><c:out value="${fieldErrors.maxAttempts}"/></div>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label" for="newAssessmentPassingScore">
                                        Pass mark % <span class="ll-optional">optional</span>
                                    </label>
                                    <input class="form-control ${not empty fieldErrors.passingScorePercent ? 'is-invalid' : ''}"
                                           id="newAssessmentPassingScore" name="passingScorePercent"
                                           type="number" min="0" max="100" step="0.01"
                                           inputmode="decimal"
                                           value="<c:out value='${assessmentFormPassingScorePercent}'/>"
                                           placeholder="No pass mark">
                                    <div class="invalid-feedback"><c:out
                                            value="${fieldErrors.passingScorePercent}"/></div>
                                </div>
                                <div class="col-md-4 d-flex align-items-end">
                                    <div class="form-check">
                                        <input class="form-check-input" id="newAssessmentReveal"
                                               name="revealCorrectAnswers" type="checkbox"
                                               ${assessmentFormRevealCorrectAnswers != false ? 'checked' : ''}>
                                        <label class="form-check-label" for="newAssessmentReveal">
                                            Show correct answers after grading
                                        </label>
                                    </div>
                                </div>
                            </div>

                            <button class="btn ll-btn-primary" type="submit">Create draft assessment</button>
                            <p class="ll-form-note">
                                The assessment starts as a draft. Add questions, then publish it when the
                                class is active.
                            </p>
                        </form>
                    </details>
                </c:if>

                <c:choose>
                    <c:when test="${empty classroom.assessments}">
                        <div class="ll-empty-state">
                            <p>No assessments have been added to this class yet.</p>
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
                                                <a href="${ctx}/teacher/assessments/${assessment.id}"><c:out
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
                        <span class="ll-panel-kicker">Live session</span>
                        <h2>Live video room</h2>
                    </div>
                    <span class="ll-state ${classroom.liveSessionConfigured ? 'll-state-approved' : 'll-state-neutral'}">
                        ${classroom.liveSessionConfigured ? 'Configured' : 'Not configured'}
                    </span>
                </div>

                <c:choose>
                    <c:when test="${classroom.liveSessionConfigured and classroom.jitsiSession}">
                        <p class="ll-form-note">Your in-platform video room is ready. The teacher and enrolled students can join the live call from inside LearnLoop.</p>
                        <div class="ll-live-link">
                            <a class="btn ll-btn-primary" href="${ctx}/meetings/${classDetails.id}">Open in-platform live class</a>
                            <span class="ll-state ll-state-approved">In-platform room</span>
                        </div>
                    </c:when>
                    <c:when test="${classroom.liveSessionConfigured}">
                        <p class="ll-form-note">This class uses an external Google Meet room, which opens in a new browser tab.</p>
                        <div class="ll-live-link">
                            <a class="btn ll-btn-primary" href="${ctx}/meetings/${classDetails.id}" target="_blank" rel="noopener noreferrer">Open Google Meet</a>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <p class="ll-form-note">Create an in-platform video room for students to join directly from LearnLoop, or save a Google Meet link.</p>
                    </c:otherwise>
                </c:choose>

                <c:choose>
                    <c:when test="${editable}">
                        <form method="post" action="${classUrl}?tab=live" class="ll-stack-form">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>"/>
                            <input type="hidden" name="action" value="meeting-url"/>
                            <div>
                                <label class="form-label" for="meetingUrl">Google Meet or Jitsi room URL</label>
                                <input class="form-control ${not empty fieldErrors.meetingUrl ? 'is-invalid' : ''}"
                                       id="meetingUrl" name="meetingUrl" maxlength="1000"
                                       placeholder="https://meet.google.com/... or https://meet.jit.si/room-name"
                                       value="<c:out value='${meetingUrlValue != null ? meetingUrlValue : classroom.meetingUrl}'/>"/>
                                <div class="invalid-feedback"><c:out value="${fieldErrors.meetingUrl}"/></div>
                                <p class="ll-form-note">Google Meet opens externally. Jitsi rooms open inside LearnLoop. Leave the field blank and save to clear the current session.</p>
                            </div>
                            <button class="btn ll-btn-outline" type="submit">Save live session URL</button>
                        </form>

                        <div class="ll-panel mt-3">
                            <span class="ll-panel-kicker">Recommended for the project demo</span>
                            <h3>Create an in-platform room</h3>
                            <p class="ll-form-note">LearnLoop will generate a unique room name and embed the audio/video meeting in the platform. This replaces the currently configured session link. The public Jitsi demo service is for prototypes, not confidential or production classes.</p>
                            <form method="post" action="${classUrl}?tab=live" class="ll-inline-form"
                                  data-confirm="Create a new in-platform room? This replaces the currently configured live session link.">
                                <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}'/>"/>
                                <input type="hidden" name="action" value="meeting-room-create"/>
                                <button class="btn ll-btn-primary" type="submit">Generate in-platform live room</button>
                            </form>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="alert alert-info ll-alert" role="status">This class is <c:out value="${classDetails.status}"/> and its live session can no longer be changed.</div>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:when>
        <c:otherwise>
            <section class="ll-stat-grid ll-class-metrics" aria-label="Class activity totals" data-reveal>
                <div class="ll-stat-card ll-stat-card--static">
                    <span class="ll-stat-label">Enrolled students</span>
                    <strong class="ll-stat-value"><c:out value="${classDetails.enrolledCount}"/></strong>
                    <span class="ll-stat-meta">
                        <c:choose>
                            <c:when test="${not empty classDetails.maxStudents}">
                                of <c:out value="${classDetails.maxStudents}"/> places
                            </c:when>
                            <c:otherwise>Active enrollments</c:otherwise>
                        </c:choose>
                    </span>
                </div>
                <a class="ll-stat-card" href="${ctx}/teacher/requests">
                    <span class="ll-stat-label">Pending requests</span>
                    <strong class="ll-stat-value"><c:out value="${classDetails.pendingRequestCount}"/></strong>
                    <span class="ll-stat-meta"><c:out value="${classDetails.joinRequestCount}"/> requests in total</span>
                </a>
                <a class="ll-stat-card" href="${classUrl}?tab=materials">
                    <span class="ll-stat-label">Materials</span>
                    <strong class="ll-stat-value"><c:out value="${classDetails.materialCount}"/></strong>
                    <span class="ll-stat-meta">Class learning resources</span>
                </a>
                <a class="ll-stat-card" href="${classUrl}?tab=assessments">
                    <span class="ll-stat-label">Assessments</span>
                    <strong class="ll-stat-value"><c:out value="${classDetails.assessmentCount}"/></strong>
                    <span class="ll-stat-meta">Checks and graded work</span>
                </a>
            </section>

            <section class="ll-panel ll-invitation-panel" data-reveal>
                <div class="ll-panel-head">
                    <div>
                        <span class="ll-panel-kicker">Invitation access</span>
                        <h2>Invite students</h2>
                    </div>
                    <span class="ll-state ${invitation.state == 'ACTIVE' ? 'll-state-approved' : 'll-state-neutral'}">
                        <c:choose>
                            <c:when test="${invitation.state == 'ACTIVE'}">Active link</c:when>
                            <c:when test="${invitation.state == 'NOT_GENERATED'}">Not generated</c:when>
                            <c:when test="${invitation.state == 'EXPIRED'}">Expired</c:when>
                            <c:when test="${invitation.state == 'REVOKED'}">Revoked</c:when>
                            <c:otherwise>Class inactive</c:otherwise>
                        </c:choose>
                    </span>
                </div>

                <c:choose>
                    <c:when test="${classDetails.status != 'ACTIVE'}">
                        <div class="alert alert-info ll-alert" role="status">
                            Invitation links are available only while this class is active. Activate the class in
                            its settings before inviting students.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:if test="${not empty invitationLink}">
                            <div class="ll-invitation-share">
                                <div>
                                    <span class="ll-panel-kicker">Shown once</span>
                                    <h3>Your new invitation link</h3>
                                    <p>
                                        Copy or share this link now. For security, LearnLoop will not show the full
                                        link again after you leave this page.
                                    </p>
                                </div>
                                <div class="ll-invitation-copy-row">
                                    <label class="visually-hidden" for="invitationLink">Invitation link</label>
                                    <input class="form-control" id="invitationLink" type="text" readonly
                                           value="<c:out value='${invitationLink}'/>">
                                    <button class="btn ll-btn-primary" type="button"
                                            data-copy-target="invitationLink">Copy link</button>
                                </div>
                                <div class="ll-invitation-share-actions">
                                    <a class="btn ll-btn-outline" href="<c:out value='${invitationWhatsAppUrl}'/>"
                                       target="_blank" rel="noopener noreferrer">Share on WhatsApp</a>
                                    <span class="ll-form-note">WhatsApp opens with a prefilled message.</span>
                                </div>
                            </div>
                        </c:if>

                        <div class="ll-invitation-body">
                            <c:choose>
                                <c:when test="${invitation.state == 'ACTIVE'}">
                                    <p class="ll-form-note">
                                        The current link is active
                                        <c:if test="${not empty invitation.expiresAt}">
                                            until <strong><fmt:formatDate value="${invitation.expiresAt}"
                                                                          pattern="d MMM yyyy, HH:mm"/></strong>
                                        </c:if>.
                                        Regenerating immediately invalidates the current link.
                                    </p>
                                    <div class="ll-action-stack">
                                        <form method="post" action="${classUrl}"
                                              data-confirm="Regenerate this invitation? The current link will stop working immediately.">
                                            <input type="hidden" name="_csrf"
                                                   value="<c:out value='${sessionScope.csrfToken}'/>">
                                            <input type="hidden" name="action" value="regenerate-invitation">
                                            <button class="btn ll-btn-primary" type="submit">Regenerate link</button>
                                        </form>
                                        <form method="post" action="${classUrl}"
                                              data-confirm="Expire this invitation now? The current link will stop working.">
                                            <input type="hidden" name="_csrf"
                                                   value="<c:out value='${sessionScope.csrfToken}'/>">
                                            <input type="hidden" name="action" value="expire-invitation">
                                            <button class="btn ll-btn-outline" type="submit">Expire now</button>
                                        </form>
                                        <form method="post" action="${classUrl}"
                                              data-confirm="Revoke this invitation? The current link will stop working immediately.">
                                            <input type="hidden" name="_csrf"
                                                   value="<c:out value='${sessionScope.csrfToken}'/>">
                                            <input type="hidden" name="action" value="revoke-invitation">
                                            <button class="btn btn-outline-danger" type="submit">Revoke link</button>
                                        </form>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <c:choose>
                                        <c:when test="${invitation.state == 'NOT_GENERATED'}">
                                            <p class="ll-form-note">
                                                Generate a secure invitation link to share with students. Only the
                                                link's hash is stored; the full link is shown once.
                                            </p>
                                        </c:when>
                                        <c:when test="${invitation.state == 'EXPIRED'}">
                                            <p class="ll-form-note">
                                                The previous link expired. Generate a new link to invite more students.
                                            </p>
                                        </c:when>
                                        <c:when test="${invitation.state == 'REVOKED'}">
                                            <p class="ll-form-note">
                                                The previous link was revoked. Generate a new link when you are ready.
                                            </p>
                                        </c:when>
                                    </c:choose>
                                    <form method="post" action="${classUrl}">
                                        <input type="hidden" name="_csrf"
                                               value="<c:out value='${sessionScope.csrfToken}'/>">
                                        <input type="hidden" name="action" value="generate-invitation">
                                        <button class="btn ll-btn-primary" type="submit">Generate invitation link</button>
                                    </form>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <div class="row g-4 mt-1">
                <div class="col-xl-8">
                    <section class="ll-panel h-100" data-reveal>
                        <div class="ll-panel-head">
                            <div>
                                <span class="ll-panel-kicker">Class settings</span>
                                <h2>Edit class details</h2>
                            </div>
                        </div>

                        <c:choose>
                            <c:when test="${editable}">
                                <form method="post" action="${classUrl}" class="ll-stack-form">
                                    <input type="hidden" name="_csrf"
                                           value="<c:out value='${sessionScope.csrfToken}'/>">
                                    <input type="hidden" name="action" value="update">

                                    <div>
                                        <label class="form-label" for="editClassTitle">Class name</label>
                                        <input class="form-control ${not empty fieldErrors.title ? 'is-invalid' : ''}"
                                               id="editClassTitle" name="title" maxlength="160" required
                                               value="<c:out value='${formTitle != null ? formTitle : classDetails.title}'/>">
                                        <div class="invalid-feedback"><c:out value="${fieldErrors.title}"/></div>
                                    </div>

                                    <div class="row g-3">
                                        <div class="col-md-7">
                                            <label class="form-label" for="editClassSubject">Assigned subject</label>
                                            <select class="form-select ${not empty fieldErrors.subjectId ? 'is-invalid' : ''}"
                                                    id="editClassSubject" name="subjectId" required>
                                                <c:forEach items="${assignedSubjects}" var="subject">
                                                    <option value="${subject.id}"
                                                        ${(formSubjectId != null ? formSubjectId : classDetails.subjectId) == subject.id ? 'selected' : ''}>
                                                        <c:out value="${subject.code}"/> - <c:out value="${subject.name}"/>
                                                    </option>
                                                </c:forEach>
                                            </select>
                                            <div class="invalid-feedback"><c:out value="${fieldErrors.subjectId}"/></div>
                                        </div>
                                        <div class="col-md-5">
                                            <label class="form-label" for="editClassStatus">Status</label>
                                            <select class="form-select ${not empty fieldErrors.status ? 'is-invalid' : ''}"
                                                    id="editClassStatus" name="status" required>
                                                <option value="DRAFT"
                                                    ${(formStatus != null ? formStatus : classDetails.status) == 'DRAFT' ? 'selected' : ''}>
                                                    Draft
                                                </option>
                                                <option value="ACTIVE"
                                                    ${(formStatus != null ? formStatus : classDetails.status) == 'ACTIVE' ? 'selected' : ''}>
                                                    Active
                                                </option>
                                            </select>
                                            <div class="invalid-feedback"><c:out value="${fieldErrors.status}"/></div>
                                        </div>
                                    </div>

                                    <div>
                                        <label class="form-label" for="editClassCapacity">
                                            Capacity <span class="ll-optional">optional</span>
                                        </label>
                                        <input class="form-control ${not empty fieldErrors.maxStudents ? 'is-invalid' : ''}"
                                               id="editClassCapacity" name="maxStudents" type="number" min="1"
                                               inputmode="numeric"
                                               value="<c:out value='${formMaxStudents != null ? formMaxStudents : classDetails.maxStudents}'/>"
                                               placeholder="Leave blank for no limit">
                                        <div class="invalid-feedback"><c:out value="${fieldErrors.maxStudents}"/></div>
                                        <c:if test="${classDetails.enrolledCount > 0}">
                                            <p class="ll-form-note">
                                                Capacity cannot be set below the <c:out value="${classDetails.enrolledCount}"/>
                                                currently enrolled students.
                                            </p>
                                        </c:if>
                                    </div>

                                    <div>
                                        <label class="form-label" for="editClassDescription">
                                            Description <span class="ll-optional">optional</span>
                                        </label>
                                        <textarea class="form-control ${not empty fieldErrors.description ? 'is-invalid' : ''}"
                                                  id="editClassDescription" name="description" rows="6"
                                                  maxlength="4000"><c:out value="${formDescription != null ? formDescription : classDetails.description}"/></textarea>
                                        <div class="invalid-feedback"><c:out value="${fieldErrors.description}"/></div>
                                    </div>

                                    <button class="btn ll-btn-primary" type="submit">Save changes</button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <div class="alert alert-info ll-alert" role="status">
                                    This class is <c:out value="${classDetails.status}"/> and can no longer be edited.
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </section>
                </div>

                <div class="col-xl-4">
                    <section class="ll-panel h-100" data-reveal>
                        <div class="ll-panel-head">
                            <div>
                                <span class="ll-panel-kicker">Lifecycle</span>
                                <h2>Archive or delete</h2>
                            </div>
                        </div>

                        <div class="ll-stack-form">
                            <c:choose>
                                <c:when test="${classDetails.status == 'ARCHIVED'}">
                                    <p class="ll-form-note">
                                        This class is archived. Its records and totals remain available for reference.
                                    </p>
                                </c:when>
                                <c:otherwise>
                                    <p class="ll-form-note">
                                        Archiving keeps the class and its records visible to you but marks it inactive.
                                        Students and future classroom actions should no longer use it.
                                    </p>
                                    <form method="post" action="${classUrl}" class="ll-inline-form"
                                          data-confirm="Archive this class? Its records will be kept.">
                                        <input type="hidden" name="_csrf"
                                               value="<c:out value='${sessionScope.csrfToken}'/>">
                                        <input type="hidden" name="action" value="archive">
                                        <button class="btn ll-btn-outline" type="submit">Archive class</button>
                                    </form>
                                </c:otherwise>
                            </c:choose>

                            <c:choose>
                                <c:when test="${deletable}">
                                    <hr class="ll-divider">
                                    <p class="ll-form-note">
                                        This draft has no enrollments, requests, materials or assessments. Deleting it
                                        permanently removes the class.
                                    </p>
                                    <form method="post" action="${classUrl}" class="ll-inline-form"
                                          data-confirm="Delete this draft class permanently? This cannot be undone.">
                                        <input type="hidden" name="_csrf"
                                               value="<c:out value='${sessionScope.csrfToken}'/>">
                                        <input type="hidden" name="action" value="delete">
                                        <button class="btn btn-outline-danger" type="submit">Delete draft</button>
                                    </form>
                                </c:when>
                                <c:when test="${classDetails.status == 'DRAFT'}">
                                    <hr class="ll-divider">
                                    <p class="ll-form-note">
                                        This draft already has classroom records. Archive it instead of deleting it so
                                        those records are preserved.
                                    </p>
                                </c:when>
                                <c:otherwise>
                                    <hr class="ll-divider">
                                    <p class="ll-form-note">
                                        Only empty draft classes can be deleted. Use archive to retire this class safely.
                                    </p>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </section>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</c:if>

<%@ include file="/WEB-INF/views/common/workspace-end.jspf" %>
