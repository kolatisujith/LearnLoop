# Browser End-to-End Validation

- Executed: 2026-10-28T17:06:06.278Z
- Base URL: http://localhost:8080/learnloop
- Results: **26 passed, 0 failed, 0 blocked/manual-follow-up**
- Disposable material ID: 9
- Temporary material cleanup: deleted through teacher workspace and verified absent

| Check | Result | Detail |
|---|---|---|
| public page / | PASS | HTTP 200; final /learnloop/ |
| Landing platform-status pill updates | PASS | Platform online appeared after the health request |
| Landing page section anchor scrolls | PASS | hash=#features; target=true; visible=true |
| teacher browser sign-in | PASS | final path /learnloop/teacher/dashboard |
| teacher session cookie is HttpOnly/SameSite | PASS | JSESSIONID present=true; HttpOnly and SameSite=Lax inspected |
| teacher page /teacher/dashboard | PASS | HTTP 200; final /learnloop/teacher/dashboard |
| teacher page /teacher/classes | PASS | HTTP 200; final /learnloop/teacher/classes |
| teacher page /teacher/classes/2 | PASS | HTTP 200; final /learnloop/teacher/classes/2 |
| teacher page /teacher/classes/2?tab=overview | PASS | HTTP 200; final /learnloop/teacher/classes/2?tab=overview |
| teacher page /teacher/classes/2?tab=materials | PASS | HTTP 200; final /learnloop/teacher/classes/2?tab=materials |
| teacher page /teacher/classes/2?tab=assessments | PASS | HTTP 200; final /learnloop/teacher/classes/2?tab=assessments |
| teacher page /teacher/classes/2?tab=live | PASS | HTTP 200; final /learnloop/teacher/classes/2?tab=live |
| teacher page /teacher/requests | PASS | HTTP 200; final /learnloop/teacher/requests |
| teacher page /teacher/materials | PASS | HTTP 200; final /learnloop/teacher/materials |
| teacher page /teacher/assessments | PASS | HTTP 200; final /learnloop/teacher/assessments |
| teacher page /teacher/assessments/7 | PASS | HTTP 200; final /learnloop/teacher/assessments/7 |
| teacher page /teacher/progress | PASS | HTTP 200; final /learnloop/teacher/progress |
| teacher page /meetings/2 | PASS | HTTP 200; final /learnloop/meetings/2 |
| Jitsi iframe is present in protected meeting page | PASS | iframe title=(provider default) |
| Jitsi page CSP has no observed violations | PASS | securitypolicyviolation events=0; CSP console messages=0; details=[] |
| Teacher can upload disposable allowlisted file | PASS | final URL http://localhost:8080/learnloop/teacher/classes/2?tab=materials |
| Teacher can publish temporary material | PASS | material id=9 |
| Class owner can download uploaded file | PASS | HTTP 200; type=text/plain;charset=UTF-8; attachment=true; nosniff=nosniff; content-match=true |
| Anonymous user cannot download file | PASS | HTTP 200; redirected=true; final path=/learnloop/login |
| Signed-in admin classroom tabs (isolated browser suite) | PASS | 15/15 passed in the separate disposable-database browser suite; see docs/phase14-admin-isolated-browser.md |
| No uncaught application JavaScript errors | PASS | no uncaught page errors or HTTP 5xx responses observed |

## CSP observations

- No CSP refusal messages were observed in browser console output.

Jitsi external API responses:
- HTTP 200: https://meet.jit.si/external_api.js

This run uses isolated browser contexts. The only application data change is a uniquely named temporary material in class 2, removed through the teacher workspace in cleanup. Existing demo assessment, enrollment, and class records are not changed.
