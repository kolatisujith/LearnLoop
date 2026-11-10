#Isolated MySQL Integration Results

- Executed: 2026-10-28 19:18:41 +05:30
- Disposable database server: MySQL 8.0.46 on 127.0.0.1:3307, separate data directory
- Base tables after schema and seed: 15
- Maven result: **PASS** (exit code 0)
- JUnit totals: 163 tests; 163 passed; 0 failures; 0 errors; 0 skipped
- Build log: docs/phase14-integration-build.log
- Isolation: the suite used only the temporary MySQL instance. The existing MySQL service on port 3306 and its learnloop_db were not targeted.

## Per-suite summary

| Test suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| com.learnloop.config.DatabaseConfigTest | 7 | 0 | 0 | 0 |
| com.learnloop.config.PublicUrlConfigTest | 5 | 0 | 0 | 0 |
| com.learnloop.dao.DatabaseTransactionIntegrationTest | 4 | 0 | 0 | 0 |
| com.learnloop.dao.SchemaIntegrationTest | 11 | 0 | 0 | 0 |
| com.learnloop.filter.SecurityPolicyTest | 2 | 0 | 0 | 0 |
| com.learnloop.security.CsrfTokensTest | 1 | 0 | 0 | 0 |
| com.learnloop.security.LoginThrottleTest | 3 | 0 | 0 | 0 |
| com.learnloop.security.PasswordHasherTest | 5 | 0 | 0 | 0 |
| com.learnloop.security.PasswordPolicyTest | 3 | 0 | 0 | 0 |
| com.learnloop.security.ProtectedRoutesTest | 3 | 0 | 0 | 0 |
| com.learnloop.security.SafeRedirectsTest | 2 | 0 | 0 | 0 |
| com.learnloop.security.SessionUserTest | 2 | 0 | 0 | 0 |
| com.learnloop.service.AccountAccessPolicyTest | 2 | 0 | 0 | 0 |
| com.learnloop.service.AdminManagementServiceIntegrationTest | 9 | 0 | 0 | 0 |
| com.learnloop.service.AssessmentServiceIntegrationTest | 5 | 0 | 0 | 0 |
| com.learnloop.service.AssessmentViewsTest | 3 | 0 | 0 | 0 |
| com.learnloop.service.AuthenticationServiceIntegrationTest | 4 | 0 | 0 | 0 |
| com.learnloop.service.ClassManagementServiceIntegrationTest | 5 | 0 | 0 | 0 |
| com.learnloop.service.ClassroomServiceIntegrationTest | 10 | 0 | 0 | 0 |
| com.learnloop.service.ClassServiceIntegrationTest | 4 | 0 | 0 | 0 |
| com.learnloop.service.ClassServiceValidationTest | 6 | 0 | 0 | 0 |
| com.learnloop.service.DashboardServiceIntegrationTest | 6 | 0 | 0 | 0 |
| com.learnloop.service.DashboardViewsTest | 2 | 0 | 0 | 0 |
| com.learnloop.service.InvitationServiceIntegrationTest | 9 | 0 | 0 | 0 |
| com.learnloop.service.JoinRequestServiceIntegrationTest | 12 | 0 | 0 | 0 |
| com.learnloop.service.LearningAnalyticsServiceTest | 3 | 0 | 0 | 0 |
| com.learnloop.service.RegistrationValidatorTest | 7 | 0 | 0 | 0 |
| com.learnloop.service.UserServiceValidationTest | 2 | 0 | 0 | 0 |
| com.learnloop.servlet.HealthServletTest | 1 | 0 | 0 | 0 |
| com.learnloop.util.ExternalResourceUrlsTest | 9 | 0 | 0 | 0 |
| com.learnloop.util.InvitationSharingTest | 3 | 0 | 0 | 0 |
| com.learnloop.util.InvitationTokensTest | 5 | 0 | 0 | 0 |
| com.learnloop.util.MaterialFileStorageTest | 8 | 0 | 0 | 0 |
