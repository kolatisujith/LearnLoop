<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="LearnLoop - Method not allowed"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main id="main-content" class="ll-error-main">
    <div class="container">
        <div class="ll-error-card">
            <p class="ll-error-code">405</p>
            <h1>That action needs a secure form.</h1>
            <p>
                This address does not accept the requested method. Signing out, for example, must be submitted as a
                POST request with a valid security token.
            </p>
            <div class="ll-error-actions">
                <a class="btn ll-btn-primary" href="${ctx}/">Back to home</a>
                <a class="btn ll-btn-outline" href="${ctx}/login">Go to sign in</a>
            </div>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
