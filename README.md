# LearnLoop

LearnLoop is an invitation-based learning platform for administrators, teachers and students. It is
implemented with Java 21, Jakarta Servlets, JSP/JSTL, JDBC, MySQL, Maven and Apache Tomcat 10.1.

This repository implements **Phases 1–14**, including security hardening and end-to-end validation. The student HTTP smoke suite passed 25/25 checks, the isolated MySQL integration suite passed 163/163 tests, the interactive teacher/download/Jitsi browser suite passed 26/26 checks, and the isolated signed-in admin suite passed 15/15 checks. See the four `docs/phase14-*.md` reports for the recorded results and cleanup evidence. Students and teachers can register, approved accounts can sign in, and every role has a protected, database-backed workspace. Administrators can bootstrap the first admin, review teacher applications, manage subjects, maintain teacher-subject assignments and inspect every classroom. Approved teachers can manage classes, issue secure invitations, accept or reject join requests, publish protected learning materials, configure a Google Meet link or an embedded Jitsi demo room, create and publish assessments, add questions, review submissions and grade written responses. Approved students can request a place, access enrolled classes and published materials, complete assessments, receive automatic scores for objective questions and view graded results.

## Implemented feature status (Phases 1–14)

Implemented:

- All Phase 1 and Phase 2 foundation work: Maven WAR project, Jakarta Servlet 6.0/JSP views, responsive
  landing page, MySQL schema and seed, database configuration, JDBC infrastructure, DAOs, transactional
  class creation and `/health`.
- Public student and teacher registration with server-side validation. Students are approved immediately;
  teachers are created with `PENDING` status and cannot sign in until an administrator approves them.
  Public administrator registration is rejected.
- BCrypt password hashing with a cost of 12, a 72-byte input limit and a password policy requiring upper
  case, lower case, a digit and a symbol.
- Login for approved accounts with generic invalid-credential messages, dummy BCrypt work for unknown
  emails, account-status checks, session-ID rotation and a 30-minute idle timeout.
- In-memory login throttling: five failed attempts per normalized email and client IP within a 15-minute
  window, with blocked attempts audited.
- POST-only logout with CSRF validation and session invalidation. A GET request to `/logout` returns 405.
- Session-backed CSRF tokens for unsafe application methods, accepted from the `_csrf` form field or the
  `X-CSRF-Token` header; HTTP TRACE is rejected with 405.
- Server-side authentication and role authorization for `/admin/**`, `/teacher/**` and `/student/**`,
  including the protected JSP views behind those routes. Wrong-role access returns 403.
- Safe post-login redirects: only local application paths are accepted, and sensitive or authentication
  routes are rejected.
- Audit events for registration, successful login, failed login, blocked login and logout. Passwords and
  password hashes are never written to logs or audit details.
- Session-aware navigation, functional sign-in/registration pages, no-store auth responses, security
  headers and a custom 405 error page.
- Integrated role workspaces built from reusable JSP fragments for the header, sidebar navigation, alerts,
  empty states and footer:
  - Admin overview with live user, teacher, subject, class and join-request totals plus the pending-teacher
    queue; searchable, paginated teacher, subject, assignment, user and class directories.
  - Administrator write workflows for approving or rejecting pending teachers with a required rejection
    reason, creating and editing subjects, activating or deactivating subjects, and assigning or removing
    approved teachers from active subjects. Removing an assignment deactivates it so existing classes remain
    valid.
  - One-time first-administrator bootstrap from environment variables, with no default administrator
    credentials in the repository or seed data. Every admin mutation rechecks the acting account inside the
    database transaction and writes an audit event.
  - Teacher overview with approval state, assigned subjects, owned classes, pending join requests and
    enrollment totals; live class management, CSRF-protected accept/reject actions for join requests,
    progress reports, and protected classroom tabs for materials, assessments and live sessions.
  - Teacher class management with active-subject assignment checks, draft creation, owned-class listing,
    detail totals, editing, activation, archiving and guarded draft deletion.
  - Secure class invitations with one-time link display, copy and WhatsApp sharing, regeneration, manual
    expiry and revocation. Links expire after 14 days, and leaving the active state or archiving a class
    revokes its invitation automatically.
  - Public invitation pages that show class details without exposing the raw token in storage, audit details
    or the page body. Signed-out visitors can sign in and return to the same invitation through a
    session-backed path; the token is never placed in the login query string.
  - Approved students can request a place from a valid invitation. Duplicate pending requests, already
    reviewed requests, existing enrollments, ineligible accounts and full classes are blocked, and no
    enrollment is created until a teacher accepts the request.
  - A dedicated student join-request history shows pending, accepted and rejected requests with request and
    review timestamps. The existing database `APPROVED` status is presented as **Accepted**, preserving
    compatibility with the Phase 7 schema and seed. The page is scoped to the signed-in student, and pending
    requests are also surfaced on the student overview and class page.
  - Teacher join requests are loaded through a service that revalidates the approved teacher and returns only
    pending requests for classes they own. Teachers can accept requests (creating an active enrollment in the
    same transaction) or reject them with an optional reason; changes are audited and students can see the
    resulting status in their request history.
  - Student overview with enrolled classes, pending requests, available assessments and recent results;
    class catalogue, discovery, a protected class workspace and a dedicated material reader/download route.
  - Classroom materials support safe external links, plain-text reading notes and allowlisted file uploads
    stored outside the web root. Owners can create drafts, edit, publish, unpublish and delete them; students
    only see published materials in `ACTIVE` classes where they have an `ACTIVE` enrollment.
  - Assessment authoring and grading: teachers can create/edit draft assessments, configure due dates, time limits,
    attempt limits and pass marks, add single-choice, multiple-choice, true/false and written-response questions,
    publish or close assessments, and grade submitted written answers. Students can start and submit protected
    attempts, continue unexpired attempts, see automatic scoring for objective questions, and review graded results.
    Publishing validates questions; attempts and grading are transactionally saved and audited. Teacher ownership,
    student enrollment, CSRF protection and attempt limits are rechecked server-side.
  - A validated Google Meet link or Jitsi room URL per class is available only to the owning teacher, enrolled
    students and administrators. Google Meet opens externally; Jitsi rooms use the IFrame API to embed a
    video call inside LearnLoop. Jitsi's public demo service is for prototype use only; production requires
    a secured/self-hosted or authenticated meeting service. Unsafe schemes and unapproved hosts are rejected.
- Phase 12 learning analytics and recommendations:
  - Student-only `/student/analytics` reports graded-attempt count, normalized average percentage, submitted
    attempts awaiting grading, per-class performance, and the ten most recent submitted/graded results.
  - Recommended next steps link to available assessments with remaining attempts. When a class average is below
    70%, the prototype suggests revisiting that class's published materials; this transparent rule is a study prompt,
    not a machine-learning prediction or guarantee.
  - Teacher `/teacher/progress` reports active enrollment, published assessments, graded and awaiting-grading
    attempts, normalized average percentage and pass rates. Pass rates only include graded attempts for which the
    assessment has a configured pass mark.
- Phase 13 security hardening:
  - A restrictive Content Security Policy is attached to requests, forwards and error responses. Inline
    scripts and `<style>` elements are disallowed. Inline style attributes are separately permitted for the
    external Jitsi embedding API's dynamically sized iframe; the app's own inline initialization, meeting
    scripts/styles, and landing-page style attribute were moved to static assets or classes. Jitsi-related
    script, frame, image, media and connection origins are explicitly allowlisted.
  - Moved the live-meeting page's inline CSS/JavaScript into static assets and moved shared-header initialization
    and the landing-page progress width into external assets so the page can operate without inline script execution.
  - Added `X-Permitted-Cross-Domain-Policies`, made HSTS conditional on a secure request, and added `SameSite=Lax`
    session-cookie defaults with an opt-in `Secure` flag via `-Dlearnloop.cookie.secure=true`.
  - The CSRF filter explicitly rejects TRACE with 405 and treats only GET, HEAD and OPTIONS as safe methods.
  - Uploaded MIME types are selected from an allowlist based on the filename extension, not the untrusted browser
    Content-Type value. Downloads continue to use an attachment disposition and `nosniff`.
- Every dashboard, join-request, class and analytics query is scoped to the authenticated teacher or student;
  student analytics use the student's own attempts and active enrollments, while teacher analytics include only
  classes owned by that teacher. Student discovery excludes classes they already attend. Resource links are only
  rendered for `http` or `https` URLs.
- Shared workspace base servlet with exact route matching, no-store responses, reusable route metadata and
  a graceful `503` page when dashboard data cannot be loaded.
- Phase 14 verification: the isolated MySQL suite passed 163/163 tests with 0 failures, 0 errors and 0 skipped against a disposable MySQL 8.0.46 instance. The latest standard build also runs the unit/slice tests; without `LEARNLOOP_TEST_DB_*` configured, the 79 database integration tests are expected to skip in that standard invocation. Full integration results are recorded in `docs/phase14-integration-result.md` and the build log in `docs/phase14-integration-build.log`.

Further development and production considerations:
- Phase 14 validation is complete for the automated scope: live student smoke checks passed 25/25; isolated MySQL tests passed 163/163; teacher pages, upload/publish/download, anonymous download denial, landing navigation/status, Jitsi iframe/CSP and JavaScript checks passed 26/26; isolated signed-in admin pages passed 15/15. The real `/teacher/progress` 500 discovered by browser validation was fixed by using record-supported EL expressions. Reports: `docs/phase14-validation.md`, `docs/phase14-integration-result.md`, `docs/phase14-browser-validation.md`, and `docs/phase14-admin-isolated-browser.md`. Phase 14 validation does not replace a professional security review or configured production HTTPS.
- Production-grade video conferencing (authenticated meeting access, recording, moderation and attendance reporting); the public Jitsi room integration remains a prototype.
- Rich-text editing and direct file previews. Materials currently support safe links, plain-text notes and downloadable allowlisted files.
- Remember-me, password reset, email verification and multi-factor authentication.
- Connection pooling; the application opens a `DriverManager` connection per operation.
- Database migration tooling; schema and seed files are loaded with the MySQL client.
- A database connectivity check on `/health`.
- Persistent/distributed login throttling; the limiter is per running application instance.

## Technology stack

| Area | Choice |
| --- | --- |
| Language | Java 21 |
| Web API | Jakarta Servlet 6.0 (`jakarta.servlet-api` 6.0.0, provided) |
| Views | JSP 3.1, JSTL 3.0 |
| Database | MySQL 8.0+ with MySQL Connector/J 9.1.0 |
| Password hashing | jBCrypt 0.4 (BCrypt cost 12) |
| Build | Maven 3.9+, WAR packaging |
| Server | Apache Tomcat 10.1.x |
| UI | HTML5, CSS3, JavaScript, Bootstrap 5.3.3 (packaged as a WebJar) |
| Tests | JUnit 5.11.3, opt-in MySQL integration tests |

No Spring, Spring Boot, Hibernate, JPA, Struts or JSF is used.

## Prerequisites

- JDK 21 (`JAVA_HOME` must point to the JDK, not a JRE).
- Apache Maven 3.9 or newer.
- Apache Tomcat 10.1.x. Do not mix Tomcat 10.1 with `javax.servlet` examples; this project uses
  `jakarta.servlet`.
- MySQL 8.0 or newer for schema loading and integration tests.
- Optional: Eclipse IDE for Enterprise Java and Web Developers, or VS Code with the Extension Pack for Java.

Check the local toolchain:

```powershell
java -version
mvn -version
mysql --version
```

`java -version` must report Java 21 and `mvn -version` must report Java 21 as the runtime.

## Project structure

```text
LearnLoop/
├── pom.xml
├── README.md
├── .gitignore
├── database/
│   ├── schema.sql               # Idempotent MySQL schema
│   └── seed.sql                 # Idempotent development data
├── src/
│   ├── main/
│   │   ├── java/com/learnloop/
│   │   │   ├── config/          # AppMetadata, DatabaseConfig
│   │   │   ├── model/           # Account, credentials, user role and class records
│   │   │   ├── dao/             # JDBC providers, transactions, role dashboard DAOs
│   │   │   ├── security/        # BCrypt, CSRF, sessions, throttling, safe redirects
│   │   │   ├── service/         # Registration, authentication, class creation, dashboards
│   │   │   ├── servlet/         # HTTP controllers
│   │   │   │   ├── auth/        # Login, registration and logout
│   │   │   │   ├── workspace/   # Shared role-workspace rendering and failure handling
│   │   │   │   ├── admin/       # Admin workspace routes
│   │   │   │   ├── teacher/     # Teacher workspace routes
│   │   │   │   └── student/     # Student workspace routes
│   │   │   ├── filter/          # CSRF, authentication, role and header filters
│   │   │   ├── listener/        # Startup security configuration
│   │   │   └── util/            # InvitationTokens
│   │   ├── resources/
│   │   │   └── db.properties.example
│   │   └── webapp/
│   │       ├── index.jsp
│   │       ├── assets/
│   │       │   ├── css/style.css, meeting.css
│   │       │   ├── images/favicon.svg
│   │       │   └── js/app.js, meeting.js
│   │       └── WEB-INF/
│   │           ├── web.xml
│   │           └── views/
│   │               ├── common/  # header, footer, workspace, alerts and empty-state fragments
│   │               ├── auth/    # login.jsp, register.jsp
│   │               ├── admin/   # Dashboard and five read-only directories
│   │               ├── teacher/ # Dashboard, class management and protected classroom tabs
│   │               ├── student/ # Dashboard, join-request history, classroom and material views
│   │               └── error/   # 403.jsp, 404.jsp, 405.jsp, 500.jsp
│   └── test/java/com/learnloop/
│       ├── config/              # DatabaseConfig tests
│       ├── dao/                 # Transaction and schema integration tests
│       ├── security/            # Password, CSRF, redirect and route tests
│       ├── service/             # Validation and authentication integration tests
│       ├── servlet/             # Health payload test
│       ├── support/             # MySQL test connection helper
│       └── util/                # Invitation token tests
└── target/                      # Generated by Maven (not committed)
```

Only public entry resources live outside `WEB-INF`. Authentication and role views are kept under
`WEB-INF/views` so they cannot be opened directly by URL.

## Database setup

The scripts target MySQL 8.0+ and were verified with MySQL 8.4.13 LTS. The schema script creates the
`learnloop_db` database if needed and does not drop existing tables. The seed script inserts missing rows
and does not overwrite data changed by a developer.

### 1. Load the schema and development seed

Run these commands from the project root as a MySQL administrative user:

```powershell
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
```

Both scripts are safe to rerun. For a clean development reset, drop the database explicitly and then load
both scripts again:

```sql
DROP DATABASE IF EXISTS learnloop_db;
```

### 2. Create a least-privilege application account

The web application needs DML privileges on `learnloop_db`; it does not need `CREATE`, `DROP`, `ALTER` or
`GRANT` privileges. Create a local account with a real password:

```sql
CREATE USER IF NOT EXISTS 'learnloop_app'@'localhost' IDENTIFIED BY 'replace-with-a-strong-password';
CREATE USER IF NOT EXISTS 'learnloop_app'@'127.0.0.1' IDENTIFIED BY 'replace-with-a-strong-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON learnloop_db.* TO 'learnloop_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON learnloop_db.* TO 'learnloop_app'@'127.0.0.1';
FLUSH PRIVILEGES;
```

Use separate development, test and production accounts. Never run the application as `root`.

### 3. Development accounts and seed data

All seeded teacher and student accounts use the development-only password:

```text
LearnLoopSeed#2026
```

No administrator account is seeded. Create the first administrator with the one-time bootstrap command in
the next section. On a clean database, bootstrap before loading the seed if you want seeded approval audit
rows to reference that administrator; the seed works with a `NULL` actor when no admin exists.

| Email | Role | Status |
| --- | --- | --- |
| `teacher@learnloop.local` | TEACHER | APPROVED |
| `pending.teacher@learnloop.local` | TEACHER | PENDING |
| `alice.student@learnloop.local` | STUDENT | APPROVED |
| `bob.student@learnloop.local` | STUDENT | APPROVED |

The seed also contains:

- Subjects: Mathematics (`MATH`), Physics (`PHYS`) and Computer Science (`CS`).
- Assignments: the approved teacher can teach Mathematics and Computer Science; the pending teacher is
  assigned to Physics so approval rules can be tested.
- Classes: **Algebra Foundations** and **Intro to Programming**; the math class also has the demo Meet link
  `https://meet.google.com/learnloop-dev-session`.
- Development invitation tokens:
  - `learnloop-dev-invite-math-class-2026` for Algebra Foundations.
  - `learnloop-dev-invite-cs-class-2026` for Intro to Programming.
- Join requests, enrollments, materials, assessments, questions, options, one graded attempt, notifications
  and audit logs.

Only SHA-256 invitation token hashes are stored in the database. The raw tokens above exist only in this
README and in the seed script. These accounts, hashes and tokens are for local development only and must
not be reused in production.

## First administrator bootstrap

No administrator credentials are shipped in the schema, seed or source code. The first administrator is
created once by `AdminBootstrapCli`:

```powershell
$env:LEARNLOOP_DB_URL = 'jdbc:mysql://localhost:3306/learnloop_db?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true'
$env:LEARNLOOP_DB_USER = 'learnloop_app'
$env:LEARNLOOP_DB_PASSWORD = 'replace-with-the-app-password'
$env:LEARNLOOP_ADMIN_EMAIL = 'admin@example.com'
$env:LEARNLOOP_ADMIN_NAME = 'Platform Administrator'
$env:LEARNLOOP_ADMIN_PASSWORD = 'replace-with-a-strong-password'

mvn -q -DskipTests compile exec:exec
```

The command validates the email, name and password, hashes the password with BCrypt, inserts an approved
`ADMIN` account and writes an `ADMIN_BOOTSTRAPPED` audit event in one transaction. A MySQL named lock prevents
two concurrent bootstrap runs from creating two first administrators. The command refuses to run if an
administrator already exists. A failure exits non-zero; Maven prints the exit as an execution error after the
CLI message.

Clear the three `LEARNLOOP_ADMIN_*` environment variables after the command completes, and do not put the
administrator password in the repository, shell history or deployment scripts. Run this command from a
trusted host against the intended database.

## Configuration

### Resolution order

`DatabaseConfig.load()` resolves each setting in this order:

1. Environment variables.
2. Java system properties.
3. A properties file.

| Setting | Environment | System property | File key |
| --- | --- | --- | --- |
| URL | `LEARNLOOP_DB_URL` | `learnloop.db.url` | `db.url` |
| Username | `LEARNLOOP_DB_USER` (or `LEARNLOOP_DB_USERNAME`) | `learnloop.db.username` | `db.username` |
| Password | `LEARNLOOP_DB_PASSWORD` | `learnloop.db.password` | `db.password` |
| Driver | `LEARNLOOP_DB_DRIVER` | `learnloop.db.driver` | `db.driver` |

The driver defaults to `com.mysql.cj.jdbc.Driver`. The password defaults to an empty string only when no
value is supplied; the URL and username are required.

### Properties file

For local Eclipse development, copy `src/main/resources/db.properties.example` to
`src/main/resources/db.properties` and replace the placeholders. The file is ignored by Git and can be
used by Eclipse's local exploded deployment.

The Maven WAR configuration excludes `WEB-INF/classes/db.properties`, so local database credentials are
not shipped in `target/learnloop.war`. When deploying that WAR, provide settings through environment
variables, Java system properties, or an external file outside the repository:

```powershell
$env:LEARNLOOP_DB_CONFIG = 'C:\secure\learnloop\db.properties'
```

The external file uses the same `db.*` keys as the example. `LEARNLOOP_DB_CONFIG` takes precedence over
the classpath file.

### Connection URL

The example uses:

```text
jdbc:mysql://localhost:3306/learnloop_db?serverTimezone=UTC&characterEncoding=UTF-8&sslMode=PREFERRED&allowPublicKeyRetrieval=true
```

Notes:

- `serverTimezone=UTC` keeps JDBC timestamp conversion predictable.
- `allowPublicKeyRetrieval=true` is needed for local MySQL accounts using
  `caching_sha2_password` when TLS is not enabled.
- For production, use TLS (`sslMode=REQUIRED` or `VERIFY_IDENTITY`) and remove the need for public-key
  retrieval.
- The application opens a new `DriverManager` connection per operation. Connection pooling is a later
  improvement.

### Public application URL

Invitation links are built from a server-side public URL, never from the request `Host` header. Set one of:

| Setting | Environment | System property |
| --- | --- | --- |
| Public base URL | `LEARNLOOP_APP_BASE_URL` | `learnloop.app.base-url` |

The value must be an absolute `http://` or `https://` URL without a query string, fragment or embedded
credentials. Trailing slashes are removed. Include the context path when the WAR is not deployed at the
root:

```powershell
$env:LEARNLOOP_APP_BASE_URL = 'http://localhost:8080/learnloop'
```

If the URL is missing or invalid, invitation generation fails with a `503` message and no link is stored.
Set this variable before deploying the WAR.

### Material storage

Uploaded classroom files are stored outside the exploded web application. Set one of:

| Setting | Environment | System property | Default |
| --- | --- | --- | --- |
| Materials directory | `LEARNLOOP_MATERIALS_DIR` | `learnloop.materials.dir` | `~/learnloop-materials` |

The directory is created on demand. The application generates a random UUID filename and keeps the
sanitized original name only in the database. Allowed extensions are PDF, TXT, MD, CSV, DOC, DOCX, PPT,
PPTX, XLS, XLSX, ODT, ODS, ODP, PNG, JPG/JPEG, GIF, WEBP and ZIP; the maximum upload size is 10 MB.
Downloads always pass through `MaterialServlet`, which rechecks the signed-in account, class status,
enrollment and material publication state before streaming the file. Never expose the storage directory
through Tomcat or a static web server.

## Teacher class management workflow

The teacher UI uses `ClassService.createManagedClass(TeacherClassCreateRequest, ClientContext)`. It
validates the title, description, capacity, teacher and subject before opening a connection, then performs
the following work in one transaction:

1. Lock and reload the acting teacher row with `SELECT ... FOR UPDATE`.
2. Require role `TEACHER` and approval status `APPROVED`; the session role alone is not trusted.
3. Lock the subject row and require it to be active.
4. Require an active `teacher_subject_assignments` row for that teacher and subject.
5. Trim and validate the title (maximum 160 characters), description (maximum 4,000 characters) and
   optional positive capacity.
6. Generate a random placeholder invitation hash so the unique column remains populated, without marking
   the invitation as issued.
7. Insert a `DRAFT` class and a `CLASS_CREATED` audit event with the actor and optional client context.
8. Return the class to the servlet. No usable invitation link exists until the teacher generates one.

Owned reads use teacher-scoped SQL. `updateClass` can change an owned `DRAFT` or `ACTIVE` class, but cannot
set an unsupported status or reduce capacity below the active enrollment count. Moving a class out of
`ACTIVE` revokes any issued invitation in the same transaction. `archiveClass` moves a class to `ARCHIVED`,
revokes its invitation and keeps its records. `deleteClass` is limited to a `DRAFT` with zero enrollments,
join requests, materials and assessments; all other classes must be archived. Cross-teacher class IDs
return `ClassNotFoundException` (HTTP 404), and failed or unauthorized mutations roll back.

The lower-level `ClassService.createClass(ClassCreationRequest)` remains available for existing tests and
service-level callers. It follows the same approval, active-subject and assignment rules and returns the
raw invitation token to its caller.

## Invitation workflow

`InvitationService` owns invitation issuance and public access:

1. Load `LEARNLOOP_APP_BASE_URL` before the transaction so a configuration failure cannot issue a link whose
   raw value is lost.
2. Lock the acting approved teacher and the owned class. Only `ACTIVE` classes can issue invitations.
3. Generate a 256-bit URL-safe token and store only its SHA-256 hash, creation time and 14-day expiry.
   Issuing over an active link requires an explicit regenerate action; regenerating replaces the hash, so
   the previous URL stops working immediately.
4. Keep the raw URL only in the servlet response/session handoff. It is shown once and is never written to
   the database, audit details or logs.
5. Revoke or expire an invitation by updating lifecycle timestamps. Public access returns a generic 404 for
   malformed, unknown, unissued, expired, revoked and inactive-class invitations.
6. Public invite pages expose the class title, subject, teacher, description, capacity and enrollment count.
   They set `no-store`, `Referrer-Policy: no-referrer` and `X-Robots-Tag: noindex, nofollow`.
7. Signed-out visitors use `/login?invite=1`; the invitation path stays in the server-side session and is
   never placed in the login query string.
8. An approved student can submit one optional note of at most 500 characters. Enrollment and request rows
   are checked under the transaction; duplicate or reviewed requests, already enrolled students and full
   classes are rejected. Success redirects to `/student/classes?requested=1`.

Invitation audit actions are `INVITATION_ISSUED`, `INVITATION_REGENERATED`, `INVITATION_REVOKED`,
`INVITATION_EXPIRED` and `JOIN_REQUEST_CREATED`. Audit details contain only timestamps and class IDs.

## Protected classroom workflow

`ClassroomService` is the authorization boundary for class content:

1. Teacher reads require role `TEACHER`, approval status `APPROVED` and ownership of the class. Admins may
   inspect any class read-only. Students must be approved, the class must be `ACTIVE`, and an `ACTIVE`
   enrollment must exist; knowing a class or material ID is never sufficient.
2. Material types are `LINK`, `TEXT` and `FILE`. Links must be absolute HTTP/HTTPS URLs without credentials
   or control characters. Reading notes are trimmed plain text. Files use the extension allowlist, size limit
   and UUID storage described above.
3. New materials are created as unpublished drafts. Only the owning approved teacher can edit, publish,
   unpublish or delete materials, and only while the class is `DRAFT` or `ACTIVE`. Students see only
   published materials, and an authorized file download is rechecked on every request.
4. Replacing or converting a file material deletes the previous stored file after the database transaction
   succeeds. Deleting a file material removes its stored file as well. Missing files return a safe 404 rather
   than exposing server paths.
5. Each class can store an optional Google Meet or Jitsi room URL. Google Meet accepts only
   `https://meet.google.com/...` links and opens externally. Validated `https://meet.jit.si/{room}` URLs open
   in an embedded in-platform video room through `/meetings/{classId}` after access is rechecked. Public Jitsi
   rooms are for local prototype use; production should use secured/self-hosted or authenticated meeting service.
6. Audit actions are `MATERIAL_CREATED`, `MATERIAL_UPDATED`, `MATERIAL_PUBLISHED`,
   `MATERIAL_UNPUBLISHED`, `MATERIAL_DELETED` and `CLASS_MEETING_UPDATED`. Audit details contain metadata
   only, never file paths, document contents or raw upload bytes.

## Admin management workflow

All admin mutations are handled by `AdminManagementService` in a single database transaction. The service
locks and reloads the acting user, requiring role `ADMIN` and status `APPROVED` even though the route
filter has already checked the session role. The admin servlet then renders or redirects only after the
transaction commits.

- **Teacher review:** only `PENDING` teachers can be approved or rejected. Approval stores the reviewer and
  timestamp and clears any previous reason. Rejection requires a trimmed reason of at most 500 characters
  and stores it on the user row.
- **Subjects:** codes are trimmed and upper-cased, must match `[A-Z0-9][A-Z0-9_-]{1,19}`, and are unique
  alongside the trimmed name. Descriptions are trimmed and limited to 4,000 characters. Inactive subjects
  remain visible for administration but are excluded from new assignments.
- **Assignments:** only approved teachers and active subjects can be assigned. Reassigning a previously
  removed pair reactivates the existing row, preserving class references. Removing an active assignment
  deactivates it rather than deleting it.
- **Search and pagination:** admin directories accept `q` and `page`, use a fixed page size of 12 and clamp
  out-of-range pages to the available range. Query parameters are bound with prepared statements, and wildcard
  characters in user search text are escaped.
- **Audit:** successful changes record `USER_APPROVED`, `USER_REJECTED`, `SUBJECT_CREATED`, `SUBJECT_UPDATED`,
  `SUBJECT_ACTIVATED`, `SUBJECT_DEACTIVATED`, `TEACHER_ASSIGNED` or `TEACHER_UNASSIGNED` with the acting admin
  and optional client IP/user-agent context.

## Authentication and authorization

### Registration

`POST /register` is handled by `RegistrationServlet` and `UserService`:

1. Full name, email, role, password and confirmation are validated on the server. Email addresses are
   trimmed and normalized to lower case.
2. Only `STUDENT` and `TEACHER` can be selected. A posted `ADMIN` role is rejected even if the request is
   crafted manually.
3. Passwords must contain upper case, lower case, a digit and a symbol, and must be 8 to 72 UTF-8 bytes.
4. The password is hashed with BCrypt cost 12. Plaintext passwords and hashes are never stored in the
   session, returned in a response or written to logs.
5. Students are inserted as `APPROVED`; teachers are inserted as `PENDING`.
6. The account and an `ACCOUNT_REGISTERED` audit event are committed in one transaction. A duplicate email
   returns a field-level validation message rather than a database error.

### Login

`POST /login` is handled by `LoginServlet` and `AuthenticationService`:

1. The CSRF token is validated before credentials are processed.
2. Five failures for the same normalized email and client IP within 15 minutes block further attempts for
   that key until the window expires. Blocked attempts write a `LOGIN_BLOCKED` audit event.
3. Unknown emails still perform a dummy BCrypt comparison so timing does not obviously reveal whether an
   account exists. Unknown emails and wrong passwords return the same generic message.
4. `PENDING`, `REJECTED` and `SUSPENDED` accounts cannot sign in. A correct password for those accounts is
   audited as a blocked login without exposing the password hash.
5. On success, the session ID is rotated with `request.changeSessionId()`, the CSRF token is regenerated,
   the safe `next` path is applied if present, and the user is redirected to the dashboard for their role.
6. Successful and failed attempts are audited. The session stores only `SessionUser`: id, email, full name,
   role and approval status.

### Sessions, CSRF and routes

- `web.xml` sets a 30-minute session timeout and HTTP-only, cookie-only session tracking.
- `CsrfFilter` applies to every request. Every `POST`, `PUT`, `PATCH` or `DELETE` must send the session CSRF
  token in the `_csrf` form field or the `X-CSRF-Token` header.
- `AuthenticationFilter` protects `/admin/**`, `/teacher/**`, `/student/**` and the corresponding JSP views.
- `RoleAuthorizationFilter` requires `ADMIN`, `TEACHER` or `STUDENT` for the matching path prefix. A valid
  session with the wrong role receives 403.
- Dashboard JSPs live under `WEB-INF`, so they cannot be bypassed by requesting the view directly.
- `SecurityHeadersFilter` adds `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`,
  `X-Permitted-Cross-Domain-Policies: none`, `Referrer-Policy: strict-origin-when-cross-origin`,
  a restrictive Content-Security-Policy and a Permissions-Policy. HSTS is emitted only on secure requests.
- Authentication pages and protected responses use `Cache-Control: no-store`.
- Logout is `POST /logout` with a valid CSRF token. It records a `LOGOUT` audit event and invalidates the
  session. `GET /logout` returns 405 and never signs the user out.
- Post-login redirects are restricted to local paths and reject `/WEB-INF`, `/META-INF`, `/login`,
  `/register` and `/logout`.

### Service usage

```java
Database database = Database.fromConfiguration();
UserService userService = new UserService(database);
AuthenticationService authenticationService = new AuthenticationService(database);

UserAccount account = userService.register(new RegistrationRequest(
        "student@example.com",
        "Example Student",
        "STUDENT",
        "LearnLoopSeed#2026",
        "LearnLoopSeed#2026"));

LoginResult login = authenticationService.authenticate(new LoginAttempt(
        "student@example.com",
        "LearnLoopSeed#2026",
        "127.0.0.1",
        "Mozilla/5.0"));
```

## Build

From the project root:

```powershell
mvn clean package
```

The WAR is written to:

```text
target/learnloop.war
```

Run the tests only:

```powershell
mvn test
```

A plain `mvn test` runs the 71 unit tests and skips the 74 MySQL integration tests unless the test database
variables are set.

### Integration tests

The integration tests use a real MySQL schema and the development seed. They expect `schema.sql` and
`seed.sql` to have been loaded. They create unique temporary rows and clean up or roll back after each test,
but they should still run only against a disposable development database, never production.

PowerShell example for the default MySQL port:

```powershell
$env:LEARNLOOP_TEST_DB_URL = 'jdbc:mysql://localhost:3306/learnloop_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:LEARNLOOP_TEST_DB_USER = 'learnloop_app'
$env:LEARNLOOP_TEST_DB_PASSWORD = 'replace-with-the-app-password'
mvn test
```

With the integration variables set, the suite runs 145 tests:

- 71 unit tests covering configuration, public URL validation, password hashing and policy, CSRF token
  comparison, login throttling, account access policy, every protected role route, safe redirects,
  registration validation, class validation, dashboard view resource-link safety, token hashing and format
  checks, WhatsApp URL encoding, resource/Meet URL validation, material file storage safety and the health
  payload.
- 4 transaction commit/rollback and SQL error tests.
- 11 schema constraint, migration and cascade tests, including the Phase 5 rejection-reason, Phase 7
  invitation lifecycle and Phase 10 classroom columns.
- 4 class-creation service integration tests.
- 4 authentication integration tests covering student registration and login, stored BCrypt hashes,
  duplicate email rejection, unknown accounts and pending-teacher login blocking.
- 6 dashboard integration tests covering admin totals and directories, teacher-owned class and request
  scoping, student enrollment scoping, discovery isolation, published-material access and assessment
  eligibility.
- 9 admin-management integration tests covering approval, rejection, required reasons, admin
  authorization, subject normalization and lifecycle, active-subject and approved-teacher constraints,
  assignment reactivation, paged search, bootstrap validation and audit rows.
- 5 teacher class-management integration tests covering managed draft creation and counts, pending-teacher
  and unassigned-subject rejection, capacity limits, cross-teacher read/update/archive/delete denial,
  owned updates and audit rows, archive behavior and draft-only deletion rules.
- 9 invitation integration tests covering issuance, one-time raw-token handling, regeneration invalidation,
  expiry, revocation, malformed/unknown/unissued/inactive access, automatic inactive-class revocation,
  join-request creation, duplicate and ineligible access, capacity limits, enrollment/reviewed states,
  audit safety and cross-teacher denial.
- 12 join-request service integration tests covering pending/accepted/rejected history, review timestamps,
  student and teacher scoping, service authorization, duplicate protection, database uniqueness and the
  guarantee that requesting a place does not create an enrollment.
- 10 classroom integration tests covering published/draft visibility, enrollment and approval scoping,
  archived classes, material lifecycle audits, cross-teacher denial, safe link validation, Meet-link access,
  admin inspection, file replacement/cleanup and file deletion.

Phase 10 was built with Maven 3.9.9 and Eclipse Temurin 21.0.12.1. All 71 unit tests and 74 opt-in MySQL
integration tests pass against an isolated MySQL 8.0.46 instance with the Phase 10 schema and seed loaded.
The checked-in local `learnloop_app` account does not have `ALTER` privileges on this machine, so a MySQL
administrator must still apply `database/schema.sql` before using the default `learnloop_db`. The JSP pages
also compile with Tomcat 10.1 `JspC`, and `mvn clean package` produces `target/learnloop.war`.

## Deploy to Tomcat 10.1

1. Install or extract Apache Tomcat **10.1.x** and set `CATALINA_HOME`.
2. Build the WAR with `mvn clean package`.
3. Copy `target/learnloop.war` into `%CATALINA_HOME%\webapps\`.
4. Configure the database connection and `LEARNLOOP_APP_BASE_URL` before starting Tomcat. Registration,
   login, every role workspace and invitation generation require MySQL; the landing page and `/health` do
   not query the database. Set the public URL to the externally reachable application root, including the
   `/learnloop` context path when applicable.
5. Start Tomcat:
   - Windows: `%CATALINA_HOME%\bin\startup.bat`
   - Linux/macOS: `$CATALINA_HOME/bin/startup.sh`
6. Open:
   - Landing page: <http://localhost:8080/learnloop/>
   - Health check: <http://localhost:8080/learnloop/health>
   - Sign in: <http://localhost:8080/learnloop/login>
   - Registration: <http://localhost:8080/learnloop/register>
7. Stop Tomcat with `shutdown.bat` or `shutdown.sh`.

The context path is `learnloop` because the WAR is named `learnloop.war`. If you rename the WAR, the
context path changes. If you deploy from Eclipse, verify the server module has the context root
`/learnloop`.

Expected health response:

```json
{
  "status": "UP",
  "application": "LearnLoop",
  "version": "1.0.0",
  "phase": "Phase 13 - Security and System Hardening",
  "timestamp": "2026-10-26T10:00:00Z"
}
```

## Import and run in Eclipse

1. Install JDK 21. In Eclipse, open **Window > Preferences > Java > Installed JREs** and add the JDK 21
   installation.
2. Import the Maven project: **File > Import > Maven > Existing Maven Projects**, choose the
   `LearnLoop` directory, and select `pom.xml`.
3. Right-click the project and choose **Maven > Update Project**, with **Force Update of Snapshots/Releases**
   enabled. This lets m2e align the Eclipse facets and classpath with the POM.
4. Add Tomcat 10.1: **Window > Preferences > Server > Runtime Environments > Add > Apache Tomcat v10.1**,
   then point it at the Tomcat 10.1 installation directory.
5. Open **Project > Properties > Targeted Runtimes** and enable the Tomcat 10.1 runtime.
6. In the **Servers** view, create a Tomcat 10.1 server, add the `learnloop` module, and start it.
7. Open <http://localhost:8080/learnloop/>.

The checked-in Eclipse metadata already targets Java 21 and Dynamic Web Module 6.0. If Eclipse reports
a facet mismatch, run **Maven > Update Project** again and confirm the JRE is JDK 21.

## Run and edit in VS Code

1. Install the **Extension Pack for Java** and **Maven for Java** extensions.
2. Optionally install **Community Server Connectors** to manage Tomcat from VS Code.
3. Open the `LearnLoop` folder. VS Code imports it as a Maven project.
4. Configure a Java 21 runtime in `java.configuration.runtimes` or set `JAVA_HOME` to JDK 21.
5. Load the schema and seed using the MySQL commands above.
6. Build from the integrated terminal with `mvn clean package`.
7. Deploy `target/learnloop.war` to a Tomcat 10.1 server, or add the Tomcat installation to the server
   connector and deploy the WAR from there.
8. Open <http://localhost:8080/learnloop/>.

VS Code does not run this application with `mvn spring-boot:run`; LearnLoop is a traditional WAR that
requires a servlet container.

## Sessions and cookies

`web.xml` configures a 30-minute session timeout, HTTP-only session cookies and cookie-only session
tracking. The session ID is rotated after successful login and the session is invalidated on logout. Auth
and protected responses are marked `no-store`.

The session cookie is HTTP-only and uses `SameSite=Lax`. In HTTPS deployments, enable the `Secure`
flag with `-Dlearnloop.cookie.secure=true` (or equivalent container configuration); keep it disabled only
for local HTTP development. HSTS is emitted when the servlet request is secure. Configure the HTTPS connector
or reverse proxy correctly so the application sees the original secure request.

## Routes implemented through Phase 13

| Route | Method | Access | Purpose |
| --- | --- | --- | --- |
| `/` | GET | Public | Landing page |
| `/health` | GET | Public | JSON health check; does not query MySQL |
| `/register` | GET, POST | Public | Student or teacher registration |
| `/login` | GET, POST | Public | Sign in with an approved account |
| `/logout` | POST | Signed-in user with CSRF token | Invalidate the session |
| `/logout` | GET | Public | Returns 405 and does not sign the user out |
| `/invite/{token}` | GET | Public | Preview a class from a valid invitation |
| `/invite/{token}` | POST | STUDENT + CSRF | Submit a join request for a valid invitation |
| `/admin/dashboard` | GET | ADMIN | Admin totals and pending-teacher queue |
| `/admin/teachers` | GET | ADMIN | Searchable teacher directory with approval controls |
| `/admin/teachers` | POST | ADMIN + CSRF | Approve or reject a pending teacher |
| `/admin/subjects` | GET | ADMIN | Searchable subject catalogue with management forms |
| `/admin/subjects` | POST | ADMIN + CSRF | Create, update, activate or deactivate a subject |
| `/admin/assignments` | GET | ADMIN | Searchable teacher-subject assignments and assignment form |
| `/admin/assignments` | POST | ADMIN + CSRF | Assign or remove a teacher-subject pairing |
| `/admin/users` | GET | ADMIN | Searchable account directory |
| `/admin/classes` | GET | ADMIN | Searchable class directory |
| `/admin/classes/{id}` | GET | ADMIN | Read-only classroom details, materials, assessments and live session |
| `/teacher/dashboard` | GET | TEACHER | Teacher overview and assigned subjects |
| `/teacher/classes` | GET | TEACHER | Classes owned by the signed-in teacher |
| `/teacher/classes` | POST | TEACHER + CSRF | Create a draft class |
| `/teacher/classes/{id}` | GET | TEACHER | Owned class details with overview, materials, assessments and live-session tabs |
| `/teacher/classes/{id}` | POST | TEACHER + CSRF | Update/archive/delete a class, manage invitations, materials or the Meet link |
| `/teacher/requests` | GET | TEACHER | Pending join requests for owned classes |
| `/teacher/requests` | POST | TEACHER + CSRF | Accept or reject a pending request; acceptance creates enrollment |
| `/teacher/materials` | GET | TEACHER | Materials in owned classes |
| `/teacher/assessments` | GET | TEACHER | Assessments in owned classes |
| `/teacher/assessments/{id}` | GET | TEACHER | Owned assessment builder, questions, attempts and grading workspace |
| `/teacher/assessments/{id}` | POST | TEACHER + CSRF | Edit/publish/close draft assessments, add/remove questions, grade submitted written responses |
| `/teacher/progress` | GET | TEACHER | Owned-class learning analytics, normalized averages, pending grading and pass rates |
| `/student/dashboard` | GET | STUDENT | Student overview and recent results |
| `/student/classes` | GET | STUDENT | Enrolled classes and pending join requests |
| `/student/classes/{id}` | GET | STUDENT | Protected class workspace with published materials and live session |
| `/student/requests` | GET | STUDENT | Full join-request history with statuses and review timestamps |
| `/student/discover` | GET | STUDENT | Active classes not yet enrolled |
| `/student/learning` | GET | STUDENT | Published materials from enrolled classes |
| `/student/assessments` | GET | STUDENT | Assessments available to enrolled students and latest result summaries |
| `/student/analytics` | GET | STUDENT | Student-only performance summary, recent results and rule-based study recommendations |
| `/student/assessments/{id}` | GET | STUDENT | Assessment details, availability rules, attempt history and results |
| `/student/assessments/{id}` | POST | STUDENT + CSRF | Start an eligible attempt |
| `/student/attempts/{id}` | GET | STUDENT | Continue an in-progress attempt or review a submitted/graded attempt |
| `/student/attempts/{id}` | POST | STUDENT + CSRF | Submit answers for an owned in-progress attempt |
| `/materials/{id}` | GET | Signed-in + classroom access | Material detail for an authorized teacher, student or admin |
| `/materials/{id}/download` | GET | Signed-in + classroom access | Authorized file download with attachment headers |
| `/meetings/{classId}` | GET | Signed-in + classroom access | Revalidate access, embed Jitsi demo rooms in LearnLoop, or redirect to Google Meet |
| `/WEB-INF/views/**` | - | Container/filters | Not directly reachable |
| Unknown path | - | Public | Custom 404 page |

The administrator routes are paginated and searchable through the `q` and `page` query parameters. Every
admin and teacher class POST requires the session CSRF token. Admin mutations revalidate the actor as an
approved administrator, while teacher class mutations revalidate an approved teacher and owned class inside
the database transaction, then record an audit event. Public invitation GET and POST requests are also
CSRF-protected for the join mutation. Student request history is scoped to the signed-in student, and the
teacher request queue is scoped through a service that revalidates the approved teacher. Material pages,
downloads and meeting routes revalidate classroom access on every request. Teacher join-request decisions
revalidate ownership and account approval, create enrollment transactionally on acceptance, and write audit
records. Assessment reads and writes are scoped to the owning teacher or enrolled student; publication, answer submission and grading use validated CSRF-protected actions and transactional updates.

## Verification checklist (Phases 1–14 validation status)

- [x] Student registration creates an `APPROVED` account and permits immediate sign-in.
- [x] Teacher registration creates a `PENDING` account and blocks sign-in until approval.
- [x] Public administrator registration is rejected.
- [x] Registration passwords are stored as BCrypt hashes, never as plaintext.
- [x] Duplicate emails and invalid registration fields are rejected without a database error.
- [x] Unknown emails and wrong passwords return the same generic invalid-credentials result.
- [x] Failed logins are throttled after five attempts in the configured window.
- [x] Sessions are rotated on login and invalidated on logout.
- [x] CSRF tokens are required for registration, login, logout, admin mutations and teacher class mutations.
- [x] Role filters enforce ADMIN, TEACHER and STUDENT routes and return 403 for wrong roles.
- [x] Unauthenticated users are redirected to login with a safe local return path.
- [x] `POST /logout` signs out; `GET /logout` returns 405 without signing out.
- [x] All 18 workspace GET routes render their role-specific page with `no-store` response headers.
- [x] The seed no longer creates a default administrator.
- [x] The bootstrap command creates only the first approved admin and refuses to create another.
- [x] Admin actions revalidate the acting account as an approved administrator inside the transaction.
- [x] Pending teachers can be approved or rejected; rejection requires a reason of at most 500 characters.
- [x] Subjects can be created, renamed, described, activated and deactivated with duplicate protection.
- [x] Approved teachers can be assigned to active subjects; duplicate active assignments are rejected.
- [x] Removing an assignment deactivates it and preserves existing classes.
- [x] Admin directories support search and 12-row pagination with clamped page numbers.
- [x] Admin mutations write audit rows and roll back on validation or authorization failures.
- [x] Teacher pages expose only classes, requests, materials, assessments and progress owned by that teacher.
- [x] An approved teacher can create a draft class only for a currently assigned active subject.
- [x] Teacher class lists and details include live enrollment, pending-request, material and assessment totals.
- [x] Another approved teacher cannot read, update, archive or delete a class they do not own.
- [x] Class edits support title, description, subject, capacity, draft and active status; capacity cannot fall
  below active enrollment.
- [x] Archiving preserves class records, while deletion is limited to an empty draft.
- [x] Teacher class mutations write audit rows and roll back on validation or authorization failures.
- [x] Invitation links use the configured public base URL and never trust the request `Host` header.
- [x] Only an approved owner can issue, regenerate, expire or revoke an active class invitation.
- [x] Invitation tokens are stored only as SHA-256 hashes; raw tokens never appear in audit details or logs.
- [x] Regeneration invalidates the previous link immediately.
- [x] Invitations expire after 14 days and can be expired or revoked manually.
- [x] Leaving `ACTIVE`, archiving or cancelling a class blocks its invitation.
- [x] Malformed, unknown, unissued, expired, revoked and inactive-class invitations are rejected.
- [x] Signed-out invite visitors return to the same invitation through `/login?invite=1` without a token in
  the login query string.
- [x] Public invite pages render class details, capacity and viewer state with no-store and no-referrer
  headers.
- [x] Approved students can submit one optional note of at most 500 characters to request a place.
- [x] Duplicate pending requests, reviewed requests, existing enrollments, ineligible accounts and full
  classes are rejected.
- [x] Submitting a join request never creates an enrollment before a teacher decision.
- [x] Join requests write `JOIN_REQUEST_CREATED` audit events and redirect to `/student/classes?requested=1`.
- [x] `/student/requests` shows only the signed-in student's requests with clear pending, accepted and
  rejected labels plus request and review timestamps.
- [x] Pending requests are surfaced on the student overview and class page with links to the full history.
- [x] `/teacher/requests` exposes only pending requests for classes owned by the signed-in approved teacher.
- [x] Approved teachers can accept or reject pending requests using CSRF-protected POST forms; acceptance creates an active enrollment in the same transaction and audit records are written.
- [x] End-to-end workflow verified: a test student registered, used the Intro to Programming invitation, requested entry, was accepted, and then accessed published materials and the in-platform meeting page.
- [x] Join-request listing services reject unapproved, wrong-role and unknown accounts with 403/access errors.
- [x] Teacher invitation controls provide a one-time link with copy and WhatsApp sharing affordances.
- [x] Student pages expose only enrolled classes, published materials and eligible assessments, and discovery
  excludes current enrollments.
- [x] A workspace request returns a graceful `503` page when MySQL is unavailable and recovers when it returns.
- [x] All internal workspace links in the admin, teacher and student sidebars resolve for the signed-in role.
- [x] External material links accept only absolute HTTP/HTTPS URLs and reject credentials, control characters
  and dangerous schemes.
- [x] Google Meet links accept only HTTPS URLs on `meet.google.com`; Jitsi room URLs are validated and opened in an access-checked embedded meeting page.
- [x] Uploaded files use UUID names, an extension allowlist, a 10 MB limit, path-traversal guards and storage
  outside the web root.
- [x] Classroom services require an approved owner for material writes and an approved, actively enrolled
  student for published reads; admins remain read-only.
- [x] New materials start unpublished and expose publish, unpublish and delete operations with audit rows.
- [x] File replacement and deletion remove the previous stored file.
- [x] Phase 13 `mvn -B package` passed 163 tests with 0 failures and 0 errors; 84 tests passed and 79 optional MySQL integration tests were skipped because the separate `LEARNLOOP_TEST_DB_*` variables were not configured.
- [x] `SecurityPolicyTest` verifies CSRF-safe method classification and that the CSP forbids inline scripts.
- [x] `MaterialFileStorageTest` verifies uploaded MIME types are derived from the extension allowlist rather than browser-supplied Content-Type, and unknown/legacy extensions use `application/octet-stream` on download.
- [x] Deployed HTTP smoke check: `/`, `/login`, `/health`, `/assets/css/meeting.css` and `/assets/js/meeting.js` returned 200 with the CSP and `X-Content-Type-Options: nosniff` headers.
- [x] HTTP login response included `HttpOnly; SameSite=Lax`; unauthenticated role routes redirected to login; `TRACE /health` returned 405.
- [x] Signed-in browser check of `/meetings/2`: Jitsi iframe appeared, `https://meet.jit.si/external_api.js` returned HTTP 200, and no parent-page CSP violations or CSP refusal messages were observed. Re-validate provider policy when deploying to a different environment.
- [x] `LearningAnalyticsServiceTest` covers recommendation generation, attempt-limit filtering, and the rule for suggesting material review when a class average is below 70%.
- [x] `AssessmentViewsTest` verifies the record-style properties used by assessment JSPs so Jakarta EL rendering regressions are caught by unit tests.
- [x] End-to-end assessment test creates a draft, adds objective and written questions, publishes it, submits a student attempt, confirms objective auto-scoring, saves a teacher-written grade and verifies the persisted result is 9/10 (90%).
- [x] Ran all 79 opt-in MySQL integration tests against a fresh disposable MySQL 8.0.46 instance on port 3307; `mvn -B clean verify` passed 163/163 tests with no skips. The live port-3306 `learnloop_db` was not targeted; the temporary server and data directory were removed.
- [x] Apply the Phase 10 schema migration to an isolated MySQL 8.0.46 test database and verify both columns.
- [x] Applied the Phase 10 schema updates to the developer's existing `learnloop_db` without dropping tables or resetting records.
- [x] All JSP pages compile with Tomcat 10.1 `JspC`.
- [x] `mvn clean package` produces `target/learnloop.war`.
- [x] Rebuilt the WAR and published the updated classes/JSPs to the local Tomcat deployment; `/health`, public pages, teacher dashboard/class pages, student classroom/material pages and the embedded meeting route were verified with HTTP checks.
- [x] Browser-tested file upload, publish and download as the owner (HTTP 200, `text/plain`, attachment, `nosniff`, exact file content); verified unauthenticated download redirects to login. Admin dashboards, directories and all class tabs passed 15/15 checks on a disposable test DB. Temporary test materials and isolated runtime were removed.
- [x] Phase 14 `scripts/phase14-smoke.ps1` passed 25/25 live HTTP checks: health/CSP, login/register forms and CSRF token, protected anonymous routes, temporary student registration/sign-in, session rotation, seven student workspace pages, role-denial checks, GET logout 405, missing-CSRF POST logout 403 and session continuity. Temporary account cleanup was verified with zero matching rows remaining.
- [x] Manual HTTP checks confirm login, role isolation, CSRF rejection and logout behavior.
- [x] Manual admin checks confirm all six admin pages render, a subject can be created and deactivated, duplicate
  subject codes return 400, and a temporary teacher can be rejected with a stored reason.
- [x] Manual teacher class checks confirm create, list, details, edit/activate, archive and empty-draft delete
  workflows render and persist through Tomcat.
- [x] Manual invitation checks confirm generation, one-time display, copy/WhatsApp links, public preview,
  login return, join request, regeneration invalidation and revoked-link rejection.
- [x] Browser test confirms the landing-page `#features` link moves the target into view.
- [x] Browser test confirms the navigation status pill changes to "Platform online" after the health request.
- [x] Browser checks observed no uncaught application JavaScript errors on the tested public, teacher, meeting and admin routes.

## Troubleshooting

**`mvn` is not recognized**

Install Maven 3.9+, add its `bin` directory to `PATH`, or invoke the full path to `mvn.cmd`. Confirm
`JAVA_HOME` points to JDK 21.

**`invalid target release: 21`**

Maven is using an older JDK. Set `JAVA_HOME` to JDK 21 and run `mvn -version` again. Eclipse also needs
JDK 21 selected in **Installed JREs**.

**Landing page returns 404**

Check the deployed context path. A WAR named `learnloop.war` normally maps to `/learnloop`, not `/`.
Confirm Tomcat started without deployment errors and open `http://localhost:8080/learnloop/`.

**JSP compilation or `NoClassDefFoundError`**

Use Tomcat 10.1.x with Jakarta imports. Do not mix `javax.servlet` and `jakarta.servlet` dependencies.
Run `mvn clean package` and redeploy the WAR rather than an older exploded directory.

**Styles are missing**

Confirm `/learnloop/webjars/bootstrap/5.3.3/css/bootstrap.min.css` and
`/learnloop/assets/css/style.css` return HTTP 200. Check that the context path in the URL matches the
deployed WAR name.

**MySQL connection fails with `Authentication requires secure connection`**

For local development without TLS, add `allowPublicKeyRetrieval=true` to the JDBC URL. For production,
enable TLS instead. Also confirm the MySQL account exists for the host the application uses
(`localhost` versus `127.0.0.1`).

**MySQL connection fails with `Access denied`**

Verify `LEARNLOOP_DB_URL`, `LEARNLOOP_DB_USER` and `LEARNLOOP_DB_PASSWORD`, or the values in
`db.properties`. Confirm the account has `SELECT`, `INSERT`, `UPDATE` and `DELETE` on `learnloop_db`.

**`Table 'learnloop_db.users' doesn't exist`**

Load the schema first:

```powershell
mysql -u root -p < database/schema.sql
```

**Seed IDs or test data are missing**

Load the development seed:

```powershell
mysql -u root -p < database/seed.sql
```

Integration tests depend on the seeded accounts, subjects, assignments and classes.

**Integration tests are skipped**

That is expected without the test database variables. Set `LEARNLOOP_TEST_DB_URL`,
`LEARNLOOP_TEST_DB_USER` and `LEARNLOOP_TEST_DB_PASSWORD`, then rerun `mvn test`.

**Port 8080 is already in use**

Stop the conflicting process or change Tomcat's HTTP connector port in `conf/server.xml`.

## Security notes (Phase 13)

- Role authorization is enforced server-side. CSRF validation is required for unsafe HTTP methods, and HTTP
  TRACE is explicitly rejected. Security response headers include a restrictive Content Security Policy,
  clickjacking protection, MIME-sniffing protection, referrer policy, permissions policy and HSTS for secure requests.
- Inline application scripts and `<style>` elements were removed from the shared header and live-meeting view,
  and the landing-page progress bar uses a CSS class. CSP blocks inline script execution and style elements;
  inline style attributes remain allowed for Bootstrap's dynamic layout behavior. The Jitsi integration remains a prototype; validate
  CSP dependencies in the target browser/provider and review any new external origin before allowing it.
- The session cookie is HTTP-only and `SameSite=Lax`; HTTPS deployments can enable `Secure` using
  `-Dlearnloop.cookie.secure=true`. Configure HTTPS, proxy trust and deployment-appropriate cookie attributes
  in the target container before public exposure.
- This is a security-hardening pass, not certification as production-safe. Login throttling remains in process
  memory and resets on restart; it is not shared between instances. Use a persistent/distributed limiter for a cluster.
- Passwords are hashed with BCrypt cost 12. Plaintext passwords and hashes are never stored in sessions,
  returned by the application or included in logs or audit details. Seed hashes are development-only.
- All accepted unsafe HTTP methods require a session-bound CSRF token; TRACE is explicitly rejected with 405.
  Do not exclude new POST endpoints from `CsrfFilter`.
- Login throttling is held in application memory and resets when the application restarts. It also applies
  per application instance, not across a cluster.
- Sessions use a 30-minute idle timeout, an HTTP-only cookie and session-ID rotation on login. Logout
  invalidates the session server-side.
- The seed accounts and password are for local development only. Replace them before any shared deployment.
- No default administrator is seeded. The one-time bootstrap reads a password from environment variables,
  uses the normal password policy and BCrypt cost, and refuses to run after the first admin exists.
- Admin mutations never trust the role in the session for authorization. `AdminManagementService` reloads
  the actor with `FOR UPDATE` and requires `ADMIN` plus `APPROVED` inside the same transaction as the change.
- Teacher class mutations likewise reload the actor as an approved teacher and load the class with both its
  ID and `teacher_user_id` inside the transaction. A cross-teacher class ID returns 404 without revealing
  whether it exists.
- Teacher approvals, rejection reasons, subject changes, assignment changes and class lifecycle actions are
  audited. Rejection is limited to pending teachers, and inactive subjects cannot receive new assignments.
- Removing a teacher-subject assignment deactivates it instead of deleting it. Existing classes keep their
  composite foreign-key reference, while the teacher cannot create new classes for that subject.
- Workspace queries derive the teacher or student identity from the authenticated session. Class detail URLs
  accept an owned class ID only after the service verifies ownership, and enrollment reads never accept
  another student's identity from page parameters.
- Student discovery excludes classes the student already attends. Learning pages show only published
  materials from enrolled classes, and assessment pages show only assessments for those classes.
- External material links are rendered only when they use `http` or `https`; the URL is HTML-escaped in the
  page and cannot introduce a script scheme. Meet links are restricted to `https://meet.google.com/`.
- Uploaded files are stored outside the web root with random names, an extension allowlist and a 10 MB
  limit. Download URLs are not public: `MaterialServlet` reloads the material and rechecks teacher
  ownership, student enrollment or admin role before streaming. Stored paths, document contents and raw
  upload bytes are never written to audit details.
- Material writes are limited to the owning approved teacher in a mutable class. New materials are draft and
  are hidden until explicitly published, and file replacement/deletion cleans up the previous stored file.
- The Meet link is visible only to authorized classroom readers. `/meetings/{classId}` validates the saved
  URL and rechecks access before redirecting, so the destination is not exposed to unauthorized users.
- Database credentials are read from environment variables, system properties or ignored/external
  properties files. Keep them out of JSP files, servlet source and Git history.
- WAR packaging excludes `WEB-INF/classes/db.properties` from the distributable archive. Prefer environment
  variables or `LEARNLOOP_DB_CONFIG` for deployed environments.
- The application account should have only DML privileges on `learnloop_db`, not administrative rights.
- Invitation tokens are 256-bit URL-safe random values. Only their SHA-256 hashes are stored, and the raw
  URL is shown once in the teacher session handoff. It is never written to the database, audit details or
  logs. Public invitation responses set no-store, no-referrer and noindex headers.
- Invitation links use `LEARNLOOP_APP_BASE_URL` instead of the request `Host` header, preventing host-header
  injection from changing the generated link. Login return paths stay in the server-side session and the
  CTA uses `/login?invite=1`; the sign-in form carries only an `invite=1` marker, so a raw token is never
  placed in the login URL, a redirect, or the sign-in page.
- Public invitation responses use generic 404s for malformed, unknown, expired, revoked and inactive links.
  Join POSTs re-check the link, student approval, existing enrollment/request state and class capacity in a
  single transaction.
- Error pages do not expose stack traces or database details. A workspace query failure is logged
  server-side and rendered as a generic `503` page that invites the user to retry.
- `/health` exposes only the application name, version, phase and current timestamp. It does not verify the
  database connection.
- Bootstrap is served from the packaged WebJar, so the UI does not depend on a third-party CDN.

## Phase 14 completion

Phase 14 validation is complete for the planned automated/live scope. Repeatable commands are `.\scripts\phase14-smoke.ps1` (25/25 live checks) and `.\scripts\phase14-admin-isolated-test.ps1` (creates a disposable MySQL/Tomcat environment, tests signed-in admin pages, then cleans up). The isolated DB suite passed 163/163 tests; the live browser suite passed 26/26 checks; the admin suite passed 15/15 checks. Results are in `docs/phase14-validation.md`, `docs/phase14-integration-result.md`, `docs/phase14-browser-validation.md`, and `docs/phase14-admin-isolated-browser.md`. The earlier browser-discovered `/teacher/progress` EL failure is fixed. Before public production deployment, still configure HTTPS appropriately and perform a production-specific security review; Jitsi’s public demo service remains a prototype.
