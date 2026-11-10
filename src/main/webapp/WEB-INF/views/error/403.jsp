<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="LearnLoop - Access denied"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main id="main-content" class="ll-error-main">
    <div class="container">
        <div class="ll-error-card">
            <p class="ll-error-code">403</p>
            <h1>You don't have access to this area.</h1>
            <p>
                Your account does not have permission to open this page. If you believe this is a mistake,
                return to the home page or contact the LearnLoop administrator.
            </p>
            <div class="ll-error-actions">
                <a class="btn ll-btn-primary" href="${ctx}/">Back to home</a>
                <a class="btn ll-btn-outline" href="${ctx}/health">Check platform health</a>
            </div>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
