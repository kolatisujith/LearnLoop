<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="LearnLoop - Create an account"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main id="main-content" class="ll-auth-main">
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-lg-10 col-xl-8">
                <div class="ll-auth-card" data-reveal>
                    <span class="ll-eyebrow ll-eyebrow-muted">Create an account</span>
                    <span class="ll-phase-badge ll-phase-live">Registration open</span>
                    <h1>Join LearnLoop</h1>
                    <p>
                        Students can register and sign in immediately. Teachers can register, but their account
                        stays pending until an administrator approves it. Administrator accounts cannot be created
                        through public registration.
                    </p>

                    <c:if test="${not empty error}">
                        <div class="ll-form-alert ll-form-alert-error" role="alert">
                            <c:out value="${error}"/>
                        </div>
                    </c:if>

                    <form method="post" action="${ctx}/register" class="ll-auth-form" novalidate>
                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">

                        <div class="ll-form-row">
                            <div class="ll-form-field">
                                <label for="fullName" class="form-label">Full name</label>
                                <input type="text" class="form-control ll-form-control" id="fullName" name="fullName"
                                       value="<c:out value='${formFullName}'/>" autocomplete="name"
                                       maxlength="120" required autofocus>
                                <c:if test="${not empty fieldErrors.fullName}">
                                    <p class="ll-field-error" id="fullName-error">
                                        <c:out value="${fieldErrors.fullName}"/>
                                    </p>
                                </c:if>
                            </div>

                            <div class="ll-form-field">
                                <label for="role" class="form-label">Account type</label>
                                <select class="form-select ll-form-control" id="role" name="role" required>
                                    <option value="STUDENT" ${formRole == 'STUDENT' ? 'selected' : ''}>Student</option>
                                    <option value="TEACHER" ${formRole == 'TEACHER' ? 'selected' : ''}>Teacher</option>
                                </select>
                                <c:if test="${not empty fieldErrors.role}">
                                    <p class="ll-field-error" id="role-error">
                                        <c:out value="${fieldErrors.role}"/>
                                    </p>
                                </c:if>
                            </div>
                        </div>

                        <div class="ll-form-field">
                            <label for="email" class="form-label">Email address</label>
                            <input type="email" class="form-control ll-form-control" id="email" name="email"
                                   value="<c:out value='${formEmail}'/>" autocomplete="email"
                                   inputmode="email" maxlength="254" required>
                            <c:if test="${not empty fieldErrors.email}">
                                <p class="ll-field-error" id="email-error">
                                    <c:out value="${fieldErrors.email}"/>
                                </p>
                            </c:if>
                        </div>

                        <div class="ll-form-row">
                            <div class="ll-form-field">
                                <label for="password" class="form-label">Password</label>
                                <input type="password" class="form-control ll-form-control" id="password"
                                       name="password" autocomplete="new-password" maxlength="72" required>
                                <p class="ll-field-hint">
                                    At least 8 characters with uppercase, lowercase, a number and a symbol.
                                </p>
                                <c:if test="${not empty fieldErrors.password}">
                                    <p class="ll-field-error" id="password-error">
                                        <c:out value="${fieldErrors.password}"/>
                                    </p>
                                </c:if>
                            </div>

                            <div class="ll-form-field">
                                <label for="confirmPassword" class="form-label">Confirm password</label>
                                <input type="password" class="form-control ll-form-control" id="confirmPassword"
                                       name="confirmPassword" autocomplete="new-password" maxlength="72" required>
                                <c:if test="${not empty fieldErrors.confirmPassword}">
                                    <p class="ll-field-error" id="confirmPassword-error">
                                        <c:out value="${fieldErrors.confirmPassword}"/>
                                    </p>
                                </c:if>
                            </div>
                        </div>

                        <button type="submit" class="btn ll-btn-primary w-100">Create account</button>
                    </form>

                    <div class="ll-auth-meta">
                        <span>Already registered? <a href="${ctx}/login">Sign in</a></span>
                        <span>Passwords are hashed with BCrypt and are never stored in plain text.</span>
                    </div>

                    <a class="ll-back-link" href="${ctx}/">&larr; Back to home</a>
                </div>
            </div>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
