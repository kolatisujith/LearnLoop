<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="LearnLoop - Something went wrong"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main id="main-content" class="ll-error-main">
    <div class="container">
        <div class="ll-error-card">
            <p class="ll-error-code">500</p>
            <h1>Something went wrong on our side.</h1>
            <p>
                The application could not complete your request. Technical details are not shown here to keep
                the system secure. Please try again, or return to the home page.
            </p>
            <div class="ll-error-actions">
                <a class="btn ll-btn-primary" href="${ctx}/">Back to home</a>
                <a class="btn ll-btn-outline" href="${ctx}/health">Check platform health</a>
            </div>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
