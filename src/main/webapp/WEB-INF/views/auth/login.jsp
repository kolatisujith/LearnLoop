<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="LearnLoop - Sign in"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main id="main-content" class="ll-auth-main">
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-lg-9 col-xl-7">
                <div class="ll-auth-card" data-reveal>
                    <span class="ll-eyebrow ll-eyebrow-muted">Account access</span>
                    <span class="ll-phase-badge ll-phase-live">Secure sign-in</span>
                    <h1>Sign in to LearnLoop</h1>
                    <p>
                        Use the email address and password for your approved LearnLoop account. Sessions are
                        protected on the server and expire automatically after 30 minutes of inactivity.
                    </p>

                    <c:if test="${not empty info}">
                        <div class="ll-form-alert ll-form-alert-info" role="status">
                            <c:out value="${info}"/>
                        </div>
                    </c:if>
                    <c:if test="${not empty error}">
                        <div class="ll-form-alert ll-form-alert-error" role="alert">
                            <c:out value="${error}"/>
                        </div>
                    </c:if>

                    <form method="post" action="${ctx}/login" class="ll-auth-form" novalidate>
                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                        <c:if test="${not empty nextPath}">
                            <input type="hidden" name="next" value="<c:out value='${nextPath}'/>">
                        </c:if>
                        <c:if test="${inviteReturn}">
                            <input type="hidden" name="invite" value="1">
                        </c:if>

                        <div class="ll-form-field">
                            <label for="email" class="form-label">Email address</label>
                            <input type="email" class="form-control ll-form-control" id="email" name="email"
                                   value="<c:out value='${formEmail}'/>" autocomplete="email"
                                   inputmode="email" maxlength="254" required autofocus>
                        </div>

                        <div class="ll-form-field">
                            <label for="password" class="form-label">Password</label>
                            <input type="password" class="form-control ll-form-control" id="password" name="password"
                                   autocomplete="current-password" maxlength="72" required>
                        </div>

                        <button type="submit" class="btn ll-btn-primary w-100">Sign in</button>
                    </form>

                    <div class="ll-auth-meta">
                        <span>New to LearnLoop? <a href="${ctx}/register">Create an account</a></span>
                        <span>Teacher accounts need administrator approval before sign-in.</span>
                    </div>

                    <div class="ll-info-box">
                        <h2>Account status</h2>
                        <ul class="ll-info-list">
                            <li><span class="ll-tick" aria-hidden="true"></span>Student accounts are active after registration.</li>
                            <li><span class="ll-tick" aria-hidden="true"></span>Teacher accounts stay pending until an administrator approves them.</li>
                            <li><span class="ll-tick" aria-hidden="true"></span>Repeated failed attempts are temporarily throttled.</li>
                        </ul>
                    </div>

                    <a class="ll-back-link" href="${ctx}/">&larr; Back to home</a>
                </div>
            </div>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
