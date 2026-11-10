#End-to-End Validation

- Executed: 2026-10-28 22:37:49 +05:30
- Base URL: http://localhost:8080/learnloop
- Result: **25 passed, 0 failed**
- Cleanup: temporary student removed; zero matching rows remain

| Check | Result | Detail |
|---|---|---|
| Public health endpoint | PASS | HTTP 200 |
| Landing page and CSP | PASS | HTTP 200; CSP present=True |
| Login page and no-store headers | PASS | HTTP 200; Cache-Control=no-store, no-cache, must-revalidate, max-age=0 |
| Registration page has CSRF token | PASS | HTTP 200 |
| WEB-INF view cannot be fetched directly | PASS | HTTP 404 |
| Unauthenticated /student/dashboard requires sign-in | PASS | HTTP 200; final path /learnloop/login |
| Unauthenticated /teacher/dashboard requires sign-in | PASS | HTTP 200; final path /learnloop/login |
| Unauthenticated /admin/dashboard requires sign-in | PASS | HTTP 200; final path /learnloop/login |
| Student registration workflow | PASS | HTTP 200; final path /learnloop/login |
| Student sign-in and dashboard redirect | PASS | HTTP 200; final path /learnloop/student/dashboard |
| Session ID rotated after sign-in | PASS | pre-login cookie present=True; post-login cookie present=True |
| Student GET /student/dashboard | PASS | HTTP 200; final path /learnloop/student/dashboard |
| Student GET /student/classes | PASS | HTTP 200; final path /learnloop/student/classes |
| Student GET /student/requests | PASS | HTTP 200; final path /learnloop/student/requests |
| Student GET /student/discover | PASS | HTTP 200; final path /learnloop/student/discover |
| Student GET /student/learning | PASS | HTTP 200; final path /learnloop/student/learning |
| Student GET /student/assessments | PASS | HTTP 200; final path /learnloop/student/assessments |
| Student GET /student/analytics | PASS | HTTP 200; final path /learnloop/student/analytics |
| Student cannot open teacher dashboard | PASS | HTTP 403; final path /learnloop/teacher/dashboard |
| Student cannot open admin dashboard | PASS | HTTP 403; final path /learnloop/admin/dashboard |
| Student cannot read a class without enrollment | PASS | HTTP 404; final path /learnloop/student/classes/2 |
| GET logout is rejected | PASS | HTTP 405 |
| POST logout without CSRF is rejected | PASS | HTTP 403 |
| Rejected logout leaves session valid | PASS | HTTP 200; final path /learnloop/student/dashboard |
| Temporary account cleanup | PASS | remaining matching users=0 |

This script exercises public routes, registration, student authentication, protected route access, role isolation, session rotation, and CSRF rejection over HTTP. It does not reset the database or mutate existing classroom and assessment records.

## Cross-suite Phase 14 validation
- Isolated MySQL integration: `docs/phase14-integration-result.md` records 163/163 tests passing with no failures, errors or skips on a disposable MySQL 8.0.46 instance; port-3306 `learnloop_db` was not targeted.
- Interactive browser coverage: `docs/phase14-browser-validation.md` records 26/26 checks passing, including teacher routes, progress analytics, owner file download headers, anonymous download denial, landing navigation/status, Jitsi iframe/CSP, and no uncaught JavaScript errors.
- Admin coverage: `docs/phase14-admin-isolated-browser.md` records 15/15 signed-in admin checks passing against a disposable database/Tomcat; those temporary resources were removed.
- Production readiness still requires environment-specific HTTPS configuration and a professional security review; the public Jitsi demo service remains prototype-only.
