<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="LearnLoop - Private classes, real progress"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main id="main-content">
    <section class="ll-hero">
        <div class="container">
            <div class="row align-items-center g-5">
                <div class="col-lg-6" data-reveal>
                    <span class="ll-eyebrow">
                        <span class="ll-pulse" aria-hidden="true"></span>
                        Role workspaces are live
                    </span>
                    <h1 class="ll-hero-title">Keep every learner <span class="ll-highlight">in the loop.</span></h1>
                    <p class="ll-hero-lead">
                        LearnLoop brings administrators, teachers and students into one focused space for
                        invitation-only classes &mdash; from enrollment and materials to live sessions and assessments.
                    </p>
                    <div class="d-flex flex-column flex-sm-row gap-3">
                        <a class="btn ll-btn-primary btn-lg" href="${ctx}/register">
                            Create an account
                            <svg viewBox="0 0 20 20" width="18" height="18" fill="none" aria-hidden="true">
                                <path d="M4 10h11m-4.5-4.5L15 10l-4.5 4.5" stroke="currentColor"
                                      stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                        </a>
                        <a class="btn ll-btn-ghost btn-lg" href="#features">See what's inside</a>
                    </div>
                    <p class="ll-hero-note">
                        Phase 13: role-protected workspaces, assessments, learning analytics and browser security
                        controls work together to support a safer private-classroom experience.
                        <a href="${ctx}/health">Check platform health</a>.
                    </p>
                    <div class="ll-hero-points">
                        <span class="ll-hero-point">
                            <span class="ll-check" aria-hidden="true">
                                <svg viewBox="0 0 16 16" width="12" height="12" fill="none">
                                    <path d="m3 8.5 3 3 7-7" stroke="currentColor" stroke-width="2"
                                          stroke-linecap="round" stroke-linejoin="round"/>
                                </svg>
                            </span>
                            Admin-governed access
                        </span>
                        <span class="ll-hero-point">
                            <span class="ll-check" aria-hidden="true">
                                <svg viewBox="0 0 16 16" width="12" height="12" fill="none">
                                    <path d="m3 8.5 3 3 7-7" stroke="currentColor" stroke-width="2"
                                          stroke-linecap="round" stroke-linejoin="round"/>
                                </svg>
                            </span>
                            Invitation-only classes
                        </span>
                        <span class="ll-hero-point">
                            <span class="ll-check" aria-hidden="true">
                                <svg viewBox="0 0 16 16" width="12" height="12" fill="none">
                                    <path d="m3 8.5 3 3 7-7" stroke="currentColor" stroke-width="2"
                                          stroke-linecap="round" stroke-linejoin="round"/>
                                </svg>
                            </span>
                            Progress with context
                        </span>
                    </div>
                </div>

                <div class="col-lg-6" data-reveal>
                    <div class="ll-visual-wrap">
                        <div class="ll-visual-ring" aria-hidden="true"></div>
                        <div class="ll-class-preview" aria-label="Preview of a LearnLoop class card">
                            <div class="ll-preview-top">
                                <div>
                                    <span class="ll-preview-kicker">Private class</span>
                                    <h2>Physics &middot; Grade 12</h2>
                                </div>
                                <span class="ll-status-badge ll-status-pending">Request pending</span>
                            </div>
                            <div class="ll-preview-body">
                                <div class="ll-preview-grid">
                                    <div><span>Teacher</span><strong>Ms. Rivera</strong></div>
                                    <div><span>Schedule</span><strong>Tue &amp; Thu, 6 PM</strong></div>
                                    <div><span>Capacity</span><strong>18 of 24 seats</strong></div>
                                    <div><span>Subject</span><strong>Physics</strong></div>
                                </div>
                                <div class="ll-preview-progress">
                                    <div class="ll-preview-progress-label">
                                        <span>Course progress</span><strong>68%</strong>
                                    </div>
                                    <div class="ll-progress-track"><span class="ll-progress-fill"></span></div>
                                </div>
                                <ul class="ll-preview-list">
                                    <li>
                                        <span class="ll-preview-list-icon" aria-hidden="true">
                                            <svg viewBox="0 0 24 24" width="18" height="18" fill="none">
                                                <rect x="4.5" y="10" width="15" height="10" rx="2.5"
                                                      stroke="currentColor" stroke-width="1.8"/>
                                                <path d="M8 10V7.5a4 4 0 0 1 8 0V10" stroke="currentColor"
                                                      stroke-width="1.8" stroke-linecap="round"/>
                                            </svg>
                                        </span>
                                        <span>Meeting link unlocks after approval</span>
                                    </li>
                                    <li>
                                        <span class="ll-preview-list-icon" aria-hidden="true">
                                            <svg viewBox="0 0 24 24" width="18" height="18" fill="none">
                                                <path d="M7 3.5h7.5L19 8v12.5H7a2 2 0 0 1-2-2v-13a2 2 0 0 1 2-2Z"
                                                      stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
                                                <path d="M14 3.5V8h4.5M9 12h6M9 15.5h4" stroke="currentColor"
                                                      stroke-width="1.8" stroke-linecap="round"/>
                                            </svg>
                                        </span>
                                        <span>3 learning materials waiting</span>
                                    </li>
                                </ul>
                            </div>
                            <div class="ll-preview-footer">
                                <svg viewBox="0 0 16 16" width="14" height="14" fill="none" aria-hidden="true">
                                    <circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.4"/>
                                    <path d="M8 7.2v4M8 4.8v.2" stroke="currentColor" stroke-width="1.6"
                                          stroke-linecap="round"/>
                                </svg>
                                Preview interface only. No live class data is shown.
                            </div>
                        </div>
                        <div class="ll-floating-card ll-floating-card--one">
                            <span class="ll-floating-icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24" width="18" height="18" fill="none">
                                    <path d="M10 13a5 5 0 0 0 7.1 0l2-2a5 5 0 0 0-7.1-7.1l-1.1 1.1"
                                          stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                                    <path d="M14 11a5 5 0 0 0-7.1 0l-2 2A5 5 0 0 0 12 20.1l1.1-1.1"
                                          stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                                </svg>
                            </span>
                            <span><small>Secure invitation</small><strong>Link shown once</strong></span>
                        </div>
                        <div class="ll-floating-card ll-floating-card--two">
                            <span class="ll-floating-icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24" width="18" height="18" fill="none">
                                    <circle cx="9" cy="8" r="3.2" stroke="currentColor" stroke-width="1.8"/>
                                    <path d="M3.8 20a5.2 5.2 0 0 1 10.4 0M15.5 5.5a3.2 3.2 0 0 1 0 6.2M17 14.5a5.2 5.2 0 0 1 3.2 4.8"
                                          stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                                </svg>
                            </span>
                            <span><small>Approved students</small><strong>18 enrolled</strong></span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <section class="ll-role-strip" aria-label="Roles supported by LearnLoop">
        <div class="container">
            <div class="ll-role-strip-inner">
                <span class="ll-role-strip-title">One platform. Three clear roles.</span>
                <div class="ll-role-chips">
                    <span class="ll-role-chip">Admin</span>
                    <span class="ll-role-chip">Teacher</span>
                    <span class="ll-role-chip">Student</span>
                </div>
            </div>
        </div>
    </section>

    <section id="features" class="ll-section">
        <div class="container">
            <div class="ll-section-head" data-reveal>
                <span class="ll-eyebrow ll-eyebrow-muted">What the loop includes</span>
                <h2>Everything needed to run a private classroom.</h2>
                <p>
                    LearnLoop is designed around one connected workflow: approve the teacher, assign the subject,
                    invite the student, protect the classroom and measure progress.
                </p>
            </div>

            <div class="row g-4">
                <div class="col-md-6 col-xl-4">
                    <article class="ll-feature-card h-100" data-reveal>
                        <span class="ll-feature-icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
                                <path d="M10 13a5 5 0 0 0 7.1 0l2-2a5 5 0 0 0-7.1-7.1l-1.1 1.1"
                                      stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                                <path d="M14 11a5 5 0 0 0-7.1 0l-2 2A5 5 0 0 0 12 20.1l1.1-1.1"
                                      stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                            </svg>
                        </span>
                        <h3>Invitation-only classrooms</h3>
                        <p>Each private class gets a cryptographically secure invitation link that teachers can
                            copy, replace, revoke or share through WhatsApp or any messaging app.</p>
                        <span class="ll-phase-badge ll-phase-live">Available &middot; Secure invitations</span>
                    </article>
                </div>

                <div class="col-md-6 col-xl-4">
                    <article class="ll-feature-card h-100" data-reveal>
                        <span class="ll-feature-icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
                                <circle cx="9" cy="8" r="3.2" stroke="currentColor" stroke-width="1.8"/>
                                <path d="M3.8 20a5.2 5.2 0 0 1 10.4 0M16 13.5l1.6 1.6 3-3"
                                      stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
                                      stroke-linejoin="round"/>
                            </svg>
                        </span>
                        <h3>Teacher approvals</h3>
                        <p>Administrators review teacher applications, manage accounts and assign teachers only to
                            the subjects they are authorized to teach.</p>
                        <span class="ll-phase-badge ll-phase-live">Available &middot; Admin workspace</span>
                    </article>
                </div>

                <div class="col-md-6 col-xl-4">
                    <article class="ll-feature-card h-100" data-reveal>
                        <span class="ll-feature-icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
                                <path d="M4 5.5h16v13H4z" stroke="currentColor" stroke-width="1.8"
                                      stroke-linejoin="round"/>
                                <path d="M4 12h4l1.5 2.5h5L16 12h4M8.5 8.5h7" stroke="currentColor"
                                      stroke-width="1.8" stroke-linecap="round"/>
                            </svg>
                        </span>
                        <h3>Join request workflow</h3>
                        <p>Approved students request a seat with an optional note. Duplicate requests, reviewed
                            requests and capacity overruns are blocked.</p>
                        <span class="ll-phase-badge ll-phase-live">Available &middot; Join requests</span>
                    </article>
                </div>

                <div class="col-md-6 col-xl-4">
                    <article class="ll-feature-card h-100" data-reveal>
                        <span class="ll-feature-icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
                                <rect x="4.5" y="10" width="15" height="10" rx="2.5"
                                      stroke="currentColor" stroke-width="1.8"/>
                                <path d="M8 10V7.5a4 4 0 0 1 8 0V10M12 14v2.5" stroke="currentColor"
                                      stroke-width="1.8" stroke-linecap="round"/>
                            </svg>
                        </span>
                        <h3>Protected resources</h3>
                        <p>Materials, assessments and Google Meet links are visible only to approved, enrolled
                            students. Hiding a button is never the security boundary.</p>
                        <span class="ll-phase-badge ll-phase-live">Available &middot; Protected classroom</span>
                    </article>
                </div>

                <div class="col-md-6 col-xl-4">
                    <article class="ll-feature-card h-100" data-reveal>
                        <span class="ll-feature-icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
                                <path d="M8 4.5h8a2 2 0 0 1 2 2V20H6V6.5a2 2 0 0 1 2-2Z"
                                      stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
                                <path d="M9.5 9h5M9.5 12.5h5M9.5 16h3" stroke="currentColor"
                                      stroke-width="1.8" stroke-linecap="round"/>
                            </svg>
                        </span>
                        <h3>Assessments and scores</h3>
                        <p>Teachers build class assessments, students attempt them once, and scores are calculated
                            on the server with clear result visibility.</p>
                        <span class="ll-phase-badge ll-phase-live">Available &middot; Phase 11</span>
                    </article>
                </div>

                <div class="col-md-6 col-xl-4">
                    <article class="ll-feature-card h-100" data-reveal>
                        <span class="ll-feature-icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
                                <path d="M4 20V10M10 20V4M16 20v-7M22 20H2" stroke="currentColor"
                                      stroke-width="1.8" stroke-linecap="round"/>
                            </svg>
                        </span>
                        <h3>Progress and recommendations</h3>
                        <p>Students see completed work, assessment scores and personalized next steps based on
                            their actual classroom activity.</p>
                        <span class="ll-phase-badge ll-phase-live">Available &middot; Phase 12</span>
                    </article>
                </div>
            </div>

            <div class="ll-live-note" data-reveal>
                <span><strong>Working now:</strong> registration, approved-account sign-in, POST-based logout,
                    CSRF protection, login throttling, role-protected dashboards with live database summaries,
                    responsive workspace navigation and the MySQL/JDBC foundation.</span>
                <a href="${ctx}/health">Open <code>/health</code></a>
            </div>
        </div>
    </section>

    <section id="how-it-works" class="ll-section ll-section-alt">
        <div class="container">
            <div class="ll-section-head" data-reveal>
                <span class="ll-eyebrow ll-eyebrow-muted">The LearnLoop workflow</span>
                <h2>From assignment to enrollment in four steps.</h2>
                <p>The workflow is integrated: administrators govern access, teachers manage private classes,
                    and approved students request enrollment before accessing protected learning resources.</p>
            </div>

            <div class="ll-steps">
                <article class="ll-step" data-reveal>
                    <span class="ll-step-number">01</span>
                    <h3>Admin sets the stage</h3>
                    <p>Approve the teacher, create the subject and assign the teacher to it.</p>
                </article>
                <article class="ll-step" data-reveal>
                    <span class="ll-step-number">02</span>
                    <h3>Teacher opens a class</h3>
                    <p>Create a private class and share its secure invitation link.</p>
                </article>
                <article class="ll-step" data-reveal>
                    <span class="ll-step-number">03</span>
                    <h3>Student requests a seat</h3>
                    <p>Open the invitation, sign in or register, then request to join.</p>
                </article>
                <article class="ll-step" data-reveal>
                    <span class="ll-step-number">04</span>
                    <h3>The loop begins</h3>
                    <p>Approval unlocks materials, the Meet link and class assessments.</p>
                </article>
            </div>
        </div>
    </section>

    <section id="roles" class="ll-section">
        <div class="container">
            <div class="ll-section-head" data-reveal>
                <span class="ll-eyebrow ll-eyebrow-muted">Built for every role</span>
                <h2>Clear boundaries. Shared progress.</h2>
                <p>Each role gets a focused workspace, and every sensitive action is checked on the server.</p>
            </div>

            <div class="row g-4">
                <div class="col-lg-4">
                    <article class="ll-role-card ll-role-card--admin h-100" data-reveal>
                        <span class="ll-role-icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
                                <path d="M12 3 4.5 6v5.2c0 4.5 3.2 8.7 7.5 9.8 4.3-1.1 7.5-5.3 7.5-9.8V6L12 3Z"
                                      stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
                                <path d="m9 11.8 2 2 4-4" stroke="currentColor" stroke-width="1.8"
                                      stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                        </span>
                        <div class="ll-role-heading">
                            <h3>Admin</h3>
                            <span class="ll-phase-badge ll-phase-live">Dashboard live</span>
                        </div>
                        <p>Own the platform and keep access intentional.</p>
                        <ul class="ll-role-list">
                            <li><span class="ll-tick" aria-hidden="true"></span>Approve or reject teacher accounts</li>
                            <li><span class="ll-tick" aria-hidden="true"></span>Create and manage subjects</li>
                            <li><span class="ll-tick" aria-hidden="true"></span>Assign teachers to subjects</li>
                            <li><span class="ll-tick" aria-hidden="true"></span>Monitor classes and audit activity</li>
                        </ul>
                    </article>
                </div>

                <div class="col-lg-4">
                    <article class="ll-role-card ll-role-card--teacher h-100" data-reveal>
                        <span class="ll-role-icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
                                <path d="M3.5 8 12 4l8.5 4L12 12 3.5 8Z" stroke="currentColor"
                                      stroke-width="1.8" stroke-linejoin="round"/>
                                <path d="M6.5 10.5V15c0 1.4 2.5 2.8 5.5 2.8s5.5-1.4 5.5-2.8v-4.5M20.5 8v5"
                                      stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                            </svg>
                        </span>
                        <div class="ll-role-heading">
                            <h3>Teacher</h3>
                            <span class="ll-phase-badge ll-phase-live">Dashboard live</span>
                        </div>
                        <p>Teach only where you are authorized and invited.</p>
                        <ul class="ll-role-list">
                            <li><span class="ll-tick" aria-hidden="true"></span>Create classes for assigned subjects</li>
                            <li><span class="ll-tick" aria-hidden="true"></span>Share secure invitation links</li>
                            <li><span class="ll-tick" aria-hidden="true"></span>Review pending join requests</li>
                            <li><span class="ll-tick" aria-hidden="true"></span>Publish materials and assessments</li>
                        </ul>
                    </article>
                </div>

                <div class="col-lg-4">
                    <article class="ll-role-card ll-role-card--student h-100" data-reveal>
                        <span class="ll-role-icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
                                <circle cx="12" cy="7.5" r="3.5" stroke="currentColor" stroke-width="1.8"/>
                                <path d="M5 20a7 7 0 0 1 14 0" stroke="currentColor" stroke-width="1.8"
                                      stroke-linecap="round"/>
                            </svg>
                        </span>
                        <div class="ll-role-heading">
                            <h3>Student</h3>
                            <span class="ll-phase-badge ll-phase-live">Dashboard live</span>
                        </div>
                        <p>Join the right class and see exactly what comes next.</p>
                        <ul class="ll-role-list">
                            <li><span class="ll-tick" aria-hidden="true"></span>Request access through invitations</li>
                            <li><span class="ll-tick" aria-hidden="true"></span>Track pending, accepted and rejected requests</li>
                            <li><span class="ll-tick" aria-hidden="true"></span>Access protected resources and links</li>
                            <li><span class="ll-tick" aria-hidden="true"></span>Take assessments and view progress</li>
                        </ul>
                    </article>
                </div>
            </div>
        </div>
    </section>

    <section class="ll-cta-section">
        <div class="container">
            <div class="ll-cta" data-reveal>
                <div class="row align-items-center g-4">
                    <div class="col-lg-8">
                        <span class="ll-eyebrow ll-eyebrow-dark">Phase 13 security hardening</span>
                        <h2>Private learning, with safer defaults.</h2>
                        <p>LearnLoop combines approved accounts, role-specific access, CSRF-checked actions,
                            protected classroom resources and assessment analytics. Deployment over HTTPS with
                            production-appropriate secrets and monitoring is required before public launch.</p>
                    </div>
                    <div class="col-lg-4">
                        <div class="ll-cta-actions">
                            <a class="btn ll-btn-primary btn-lg" href="${ctx}/register">Create an account</a>
                            <a class="btn ll-btn-ghost btn-lg" href="${ctx}/login">Log in</a>
                        </div>
                        <p class="ll-cta-note">Student accounts are active immediately. Teacher accounts require
                            administrator approval before sign-in.</p>
                    </div>
                </div>
            </div>
        </div>
    </section>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
