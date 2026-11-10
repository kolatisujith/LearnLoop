<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="${empty pageTitle ? 'LearnLoop - Class invitation' : pageTitle}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main id="main-content" class="ll-auth-main ll-invite-main">
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-lg-10 col-xl-8">
                <c:choose>
                    <c:when test="${not empty invitation}">
                        <article class="ll-auth-card ll-invite-card" data-reveal>
                            <div class="ll-invite-head">
                                <span class="ll-eyebrow ll-eyebrow-muted">Private class invitation</span>
                                <span class="ll-state ll-state-approved">
                                    <c:out value="${invitation.subjectCode}"/>
                                </span>
                            </div>
                            <h1><c:out value="${invitation.title}"/></h1>
                            <p class="ll-invite-subtitle">
                                <c:out value="${invitation.subjectName}"/> with
                                <strong><c:out value="${invitation.teacherName}"/></strong>
                            </p>

                            <c:if test="${not empty error}">
                                <div class="ll-form-alert ll-form-alert-error" role="alert">
                                    <c:out value="${error}"/>
                                </div>
                            </c:if>

                            <c:if test="${not empty invitation.description}">
                                <p class="ll-invite-description"><c:out value="${invitation.description}"/></p>
                            </c:if>

                            <div class="ll-invite-facts">
                                <div>
                                    <span class="ll-stat-label">Enrolled</span>
                                    <strong><c:out value="${invitation.enrolledCount}"/>
                                        <c:choose>
                                            <c:when test="${not empty invitation.maxStudents}">
                                                of <c:out value="${invitation.maxStudents}"/>
                                            </c:when>
                                        </c:choose>
                                    </strong>
                                </div>
                                <div>
                                    <span class="ll-stat-label">Places remaining</span>
                                    <strong>
                                        <c:choose>
                                            <c:when test="${not empty invitation.seatsRemaining}">
                                                <c:out value="${invitation.seatsRemaining}"/>
                                            </c:when>
                                            <c:otherwise>No limit</c:otherwise>
                                        </c:choose>
                                    </strong>
                                </div>
                                <div>
                                    <span class="ll-stat-label">Invitation expires</span>
                                    <strong>
                                        <c:choose>
                                            <c:when test="${not empty invitation.expiresAt}">
                                                <fmt:formatDate value="${invitation.expiresAt}"
                                                                pattern="d MMM yyyy, HH:mm"/>
                                            </c:when>
                                            <c:otherwise>No expiry</c:otherwise>
                                        </c:choose>
                                    </strong>
                                </div>
                            </div>

                            <div class="ll-invite-action">
                                <c:choose>
                                    <c:when test="${invitation.viewerState == 'SIGN_IN'}">
                                        <div class="ll-info-box ll-invite-message">
                                            <h2>Ready to join?</h2>
                                            <p><c:out value="${invitation.viewerMessage}"/></p>
                                        </div>
                                        <div class="ll-invite-buttons">
                                            <a class="btn ll-btn-primary"
                                               href="${ctx}/login?invite=1">Sign in to request a place</a>
                                            <a class="btn ll-btn-outline" href="${ctx}/register">
                                                Create a student account
                                            </a>
                                        </div>
                                        <p class="ll-form-note">
                                            New accounts can return to this invitation after signing in.
                                        </p>
                                    </c:when>

                                    <c:when test="${invitation.viewerState == 'AVAILABLE'}">
                                        <form method="post" action="<c:out value='${invitationAction}'/>"
                                              class="ll-stack-form">
                                            <input type="hidden" name="_csrf"
                                                   value="<c:out value='${csrfToken}'/>">
                                            <div>
                                                <label class="form-label" for="joinMessage">
                                                    Message to the teacher
                                                    <span class="ll-optional">optional</span>
                                                </label>
                                                <textarea class="form-control ${not empty fieldErrors.message ? 'is-invalid' : ''}"
                                                          id="joinMessage" name="message" rows="4" maxlength="500"
                                                          placeholder="Share why you would like to join this class."></textarea>
                                                <div class="invalid-feedback"><c:out value="${fieldErrors.message}"/></div>
                                            </div>
                                            <p class="ll-form-note"><c:out value="${invitation.viewerMessage}"/></p>
                                            <button class="btn ll-btn-primary" type="submit">Request to join</button>
                                        </form>
                                    </c:when>

                                    <c:when test="${invitation.viewerState == 'PENDING'}">
                                        <div class="ll-form-alert ll-form-alert-info" role="status">
                                            <c:out value="${invitation.viewerMessage}"/>
                                        </div>
                                        <a class="btn ll-btn-outline" href="${ctx}/student/requests">
                                            View my request
                                        </a>
                                    </c:when>

                                    <c:when test="${invitation.viewerState == 'ENROLLED'}">
                                        <div class="ll-form-alert ll-form-alert-info" role="status">
                                            <c:out value="${invitation.viewerMessage}"/>
                                        </div>
                                        <a class="btn ll-btn-primary" href="${ctx}/student/classes">
                                            Open my classes
                                        </a>
                                    </c:when>

                                    <c:when test="${invitation.viewerState == 'ACCEPTED'}">
                                        <div class="ll-form-alert ll-form-alert-info" role="status">
                                            <c:out value="${invitation.viewerMessage}"/>
                                        </div>
                                        <a class="btn ll-btn-outline" href="${ctx}/student/requests">
                                            View my request
                                        </a>
                                    </c:when>

                                    <c:when test="${invitation.viewerState == 'REJECTED'}">
                                        <div class="ll-form-alert ll-form-alert-info" role="status">
                                            <c:out value="${invitation.viewerMessage}"/>
                                        </div>
                                        <a class="btn ll-btn-outline" href="${ctx}/student/requests">
                                            View my request
                                        </a>
                                    </c:when>

                                    <c:otherwise>
                                        <div class="ll-form-alert ll-form-alert-info" role="status">
                                            <c:out value="${invitation.viewerMessage}"/>
                                        </div>
                                        <a class="btn ll-btn-outline" href="${ctx}/">Back to LearnLoop</a>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <p class="ll-invite-privacy">
                                This invitation is personal to this class. Only its secure hash is stored by
                                LearnLoop.
                            </p>
                        </article>
                    </c:when>
                    <c:otherwise>
                        <div class="ll-error-card ll-invite-error" data-reveal>
                            <p class="ll-error-code">Wait</p>
                            <h1>Invitation unavailable</h1>
                            <p><c:out value="${empty error ? 'This invitation link is not available.' : error}"/></p>
                            <div class="ll-error-actions">
                                <a class="btn ll-btn-primary" href="${ctx}/">Back to home</a>
                                <a class="btn ll-btn-outline" href="${ctx}/health">Check platform health</a>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
