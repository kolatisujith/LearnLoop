<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="LearnLoop - Page not found"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main id="main-content" class="ll-error-main">
    <div class="container">
        <div class="ll-error-card">
            <p class="ll-error-code">404</p>
            <h1>This page is outside the loop.</h1>
            <p>
                The address you requested does not exist. It may belong to a feature that has not been
                implemented yet, or the link may be incorrect.
            </p>
            <div class="ll-error-actions">
                <a class="btn ll-btn-primary" href="${ctx}/">Back to home</a>
                <a class="btn ll-btn-outline" href="${ctx}/#features">Explore the features</a>
            </div>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
