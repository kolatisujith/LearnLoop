<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>LearnLoop - Live class</title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/images/favicon.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/webjars/bootstrap/5.3.3/css/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/meeting.css">
</head>
<body class="ll-body">
<main class="container-fluid ll-meeting-shell" id="main-content">
    <div id="meeting-app"
         class="ll-meeting-card"
         data-room-name="<c:out value='${meetingRoomName}'/>"
         data-display-name="<c:out value='${meetingDisplayName}'/>"
         data-return-url="<c:out value='${meetingReturnUrl}'/>">
        <header class="ll-meeting-heading">
            <div>
                <span class="ll-eyebrow ll-eyebrow-muted">LearnLoop live classroom</span>
                <h1>Live class</h1>
                <p class="mb-0">You are joining the in-platform video room as <strong><c:out value="${meetingDisplayName}"/></strong>.</p>
            </div>
            <a class="btn ll-btn-outline" href="<c:out value='${meetingReturnUrl}'/>">Back to classroom</a>
        </header>

        <p id="meeting-status" class="ll-meeting-status" role="status" aria-live="polite">Connecting to the video room…</p>
        <div id="jitsi-container" class="ll-meeting-frame" aria-label="Live video classroom"></div>

        <p class="ll-meeting-note">
            Live audio and video are provided by the public Jitsi demo service for this prototype. Do not use this public demo room for confidential classes. A production deployment should use a secured, self-hosted Jitsi server or an authenticated meeting service.
        </p>
    </div>
</main>

<script src="https://meet.jit.si/external_api.js" defer></script>
<script src="${pageContext.request.contextPath}/assets/js/meeting.js" defer></script>
</body>
</html>
