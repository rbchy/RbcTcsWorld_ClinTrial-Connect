# Automated Test Suite — Traceability

This document maps every manual Test Case ID in `ClinTrial_Connect_Test_Cases.xlsx` to the
automated test that exercises it, so the automated suite can be audited against the matrix
(and vice versa) the way a QA automation engineer would hand it off to a team.

## Where things live

```
src/test/java/com/clintrial/selenium/
  pages/                          Page Object Model — one class per screen/component
  BaseSeleniumTest.java           shared WebDriver lifecycle + seeded-account login helpers
  TestDataFactory.java            unique email/license/phone/title generator per test run
  PatientJourneySeleniumTest.java pre-existing smoke test (kept as-is)
  automation/
    HttpTestClient.java           raw-HTTP helper (status codes, CSRF, sessions) for the
                                   security/RBAC cases a rendered browser can't observe directly
    RegistrationAuthAutomationTest.java   AUTH-01..12
    RoleBasedAccessAutomationTest.java    RBAC-01..08
    TrialManagementAutomationTest.java    TRIAL-01..06
    MatchingAlgorithmAutomationTest.java  MATCH-01..09
    PhysicianReviewAutomationTest.java    PHYS-01..04
    AdminConsoleAutomationTest.java       ADMIN-01..04
    ContactNotificationsAutomationTest.java  CONT-01..04, NOTIF-01
    SecurityAutomationTest.java           SEC-01..08
    AccessibilityAutomationTest.java      A11Y-01..04

src/test/java/com/clintrial/dbintegrity/
  DbSupport.java                        plain-JDBC connection helper
  DatabaseIntegrityAutomationTest.java   DB-01..04

perf/clintrial-load-test.jmx            PERF-01..03 (JMeter, not Selenium — see below)
```

## Running it

Same two preconditions as the rest of the automated suite (see `সেটআপ_গাইড.md` /
`README.md`): the app running under the `demo` profile, and a live MySQL with
`schema-mysql.sql` already applied.

```bash
# start the app once, in its own terminal
mvn spring-boot:run -Dspring-boot.run.profiles=demo

# Selenium UI suite (needs Chrome; Selenium Manager auto-fetches a matching driver)
mvn -Dtest="selenium.**" -DfailIfNoTests=false -Dbase.url=http://localhost:8080 test

# plain-JDBC database-integrity suite
mvn -Dtest="dbintegrity.**" -DfailIfNoTests=false test
```

Both packages are excluded from the default `mvn test` (see `pom.xml`'s surefire
`<excludes>`), same reasoning as the pre-existing `PatientJourneySeleniumTest`: they need a
live app / live database that a plain `mvn test` can't assume.

## Traceability matrix

| Test Case ID | Automated test | Notes |
|---|---|---|
| AUTH-01 | `RegistrationAuthAutomationTest.auth01_...` | |
| AUTH-02 | `auth02_...` | |
| AUTH-03 | `auth03_...` | |
| AUTH-04 | `auth04_...` | |
| AUTH-05 | `auth05_...` | |
| AUTH-06 | `auth06_...` | |
| AUTH-07 | `auth07_...` | |
| AUTH-08 | `auth08_...` | |
| AUTH-09 | `auth09_...` | |
| AUTH-10 | `auth10_...` | reads `app_user.password_hash` directly over JDBC |
| AUTH-11 | `auth11_...` | **app bug found+fixed**: the header's Log Out link was a GET `<a>`, but Spring Security's default logout filter only matches POST while CSRF is enabled — the link silently did nothing. Changed to a POST `<form>` (`fragments/header.html`, `style.css`). |
| AUTH-12 | `auth12_...` | back-button-after-logout check; relies on Spring Security's default no-store headers to keep the dashboard out of bfcache |
| RBAC-01 | `RoleBasedAccessAutomationTest.rbac01_...` | via `HttpTestClient` (status codes only observable outside a browser) |
| RBAC-02 | `rbac02_...` | |
| RBAC-03 | `rbac03_...` | |
| RBAC-04 | `rbac04_...` | |
| RBAC-05 | `rbac05_...` | |
| RBAC-06 | `rbac06_..._documentedGap` | **discovered gap, documented not fixed**: `PhysicianController#reviewMatch` never checks trial ownership — any physician can review any match. Test asserts the current permissive behavior explicitly, per the manual case's own instruction. Flip the assertion (and remove `_documentedGap`) if/when ownership is enforced. |
| RBAC-07 | `rbac07_...` | |
| RBAC-08 | `rbac08_...` | localStorage/cookie tampering via `JavascriptExecutor` |
| TRIAL-01 | `TrialManagementAutomationTest.trial01_...` | |
| TRIAL-02 | `trial02_...` | |
| TRIAL-03 | `trial03_...` | |
| TRIAL-04 | `trial04_...` | see the MatchingService fix below — this case would have failed before it |
| TRIAL-05 | `trial05_...` | |
| TRIAL-06 | `trial06_...` | |
| MATCH-01 | `MatchingAlgorithmAutomationTest.match01_...` | uses the seeded patient1 + GLP-1 trial (100%) |
| MATCH-02 | `match02_...` | |
| MATCH-03 | `match03_...` | |
| MATCH-04 | `match04_...` | age computed dynamically from `LocalDate.now()`, not hardcoded |
| MATCH-05 | `match05_...` | |
| MATCH-06 | `match06_...` | includes a positive control (matching-gender patient) alongside the negative case |
| MATCH-07 | `match07_...` | |
| MATCH-08 | `match08_...` | **app bug found+fixed** — see below |
| MATCH-09 | `match09_...` | |
| PHYS-01 | `PhysicianReviewAutomationTest.phys01_...` | |
| PHYS-02 | `phys02_...` | verifies both the UI (row disappears) and the DB row (`status`, `reviewed_by_physician_id`) |
| PHYS-03 | `phys03_...` | |
| PHYS-04 | `phys04_..._documentedGap` | **discovered gap, documented not fixed**: neither the URL rule nor `@PreAuthorize` checks `Physician.verified` — an unverified physician can currently publish trials and review matches. Test proves and documents the current behavior. |
| ADMIN-01 | `AdminConsoleAutomationTest.admin01_...` | cross-checks displayed counts against `SELECT COUNT(*)` |
| ADMIN-02 | `admin02_...` | |
| ADMIN-03 | `admin03_...` | |
| ADMIN-04 | `admin04_...` | |
| CONT-01 | `ContactNotificationsAutomationTest.cont01_...` | |
| CONT-02 | `cont02_...` | |
| CONT-03 | `cont03_...` | |
| CONT-04 | `cont04_...` | |
| NOTIF-01 | `notif01_...` | **feature gap noted (not fixed)**: no app code path ever calls `NotificationService#notify(...)` yet, so this test seeds its fixture rows directly over JDBC. Worth raising with the team — a natural first caller would be `PhysicianController#reviewMatch`. |
| SEC-01 | `SecurityAutomationTest.sec01_...` | |
| SEC-02 | `sec02_...` | asserts the payload renders as literal escaped text (never risks actually executing an `alert()` that would hang the WebDriver session) |
| SEC-03 | `sec03_...` | |
| SEC-04 | `sec04_...` | checks every row in `app_user`, not just one fixture |
| SEC-05 | `sec05_...` | two independent `HttpTestClient` "browsers"; assertion is behavior-based, not status-code-based (see file comment for why) |
| SEC-06 | `sec06_...` | proves there is no ID-bearing URL surface at all, rather than testing a lookup that doesn't exist |
| SEC-07 | `sec07_...` | checks both a 404 and a malformed-enum 4xx for leaked stack traces/internal package names |
| SEC-08 | `sec08_...` | two concurrent registrations via `CompletableFuture`, gated on a `CountDownLatch` to maximize the race window |
| PERF-01 | *(JMeter)* `perf/clintrial-load-test.jmx` — "Browse Trials" thread group | not a Selenium/JUnit test — run via `jmeter -n -t perf/clintrial-load-test.jmx` |
| PERF-02 | *(JMeter)* same file — "Login Flow" thread group | |
| PERF-03 | *(JMeter)* same file, longer duration + manual JVM/connection-pool monitoring | inherently a manual-observation case; JMeter provides the sustained load, not the assertion |
| DB-01 | `DatabaseIntegrityAutomationTest.db01_...` | |
| DB-02 | `db02_...` | |
| DB-03 | `db03_...` | |
| DB-04 | `db04_...` | |
| A11Y-01 | `AccessibilityAutomationTest.a11y01_...` | keyboard-only login flow |
| A11Y-02 | `a11y02_...` | `label[for]` ↔ input `id` cross-check across 4 form pages |
| A11Y-03 | `a11y03_...` | 375×667 viewport, `scrollWidth` vs `clientWidth` |
| A11Y-04 | `a11y04_...` | runs a real axe-core audit (loaded from cdnjs at test time); **skips (not fails)** if axe-core can't be reached — see the file's Javadoc |

## Real defects found while building this suite (and how they were handled)

Building automated tests directly against the running app — rather than just reading the
code — surfaced three real issues. Two were unambiguous functional bugs and were fixed
directly; two others are judgment calls the manual test matrix itself flags as ambiguous, so
they were documented and asserted explicitly instead of silently "fixed" on a guess:

1. **Fixed — stale matches never left the list (`MatchingService`).**
   `findAndPersistMatches` returned *every* `TrialMatch` row ever created for a patient,
   including ones for trials that had since moved to `DRAFT`/`CLOSED`. TRIAL-04 and MATCH-08
   both state unconditionally that a closed trial "no longer appears" — this was a real bug
   against that spec, not an ambiguous case. Fixed by filtering the returned list to
   `RECRUITING` trials only, while leaving the historical row in the database (it's still
   needed for the `uq_patient_trial` unique constraint — see DB-02).

2. **Fixed — the "Log Out" link didn't actually log out.** Spring Security's default logout
   filter only matches `POST /logout` while CSRF protection is enabled (the default, and it's
   deliberately left on here). The header used a plain `<a href="/logout">` (GET), which
   never reached the logout filter at all. Changed to a real POST form styled to look
   identical to the other header links.

3. **Documented, not changed — physician match-review has no trial-ownership check
   (RBAC-06).** Any verified-or-not physician can approve/reject any match, not just ones
   tied to trials they created. The manual test case explicitly says to "verify the intended
   behavior with the dev team and assert it explicitly" rather than assuming an answer — so
   `rbac06_..._documentedGap` proves and documents the current behavior instead of guessing.

4. **Documented, not changed — unverified physicians aren't blocked from acting (PHYS-04).**
   Same reasoning as #3: the dashboard *says* "Pending admin verification" but nothing
   actually enforces it. Documented with a test rather than silently patched, since this is a
   product decision (should it block? warn? rate-limit?) not a clear-cut bug.

Also noted but intentionally left alone: **no app code path ever creates a `Notification`**
(NOTIF-01) — the read side (repository, service, dashboard panel) is fully built, but nothing
calls the write side. This is a feature gap, not a defect in the case under test, so it's
called out above rather than "fixed" by guessing where notifications should be triggered.

5. **Fixed — every Thymeleaf `<form method="post">` was missing its CSRF hidden field, so
   Spring Security's `CsrfFilter` rejected every POST with a 403 (login, both registration
   forms, contact, trial creation, match approve/reject, physician verification, trial status
   update, and the logout form added in fix #2).** CSRF protection is deliberately left ON
   (see `SecurityConfig`), and Thymeleaf does **not** auto-inject the `_csrf` hidden field into
   `th:action` forms unless a `RequestDataValueProcessor` bean is explicitly registered — Spring
   Boot does not add that bean automatically, and this project never registered one. This is
   almost certainly why a from-scratch run of the automated suite showed dozens of failures
   (registration/login "succeeding" but silently bouncing back to the same form, trial-creation
   tests timing out waiting for a page that a failed login never reached, etc.) — each affected
   template now has an explicit
   `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>` inside its
   `<form>`. (`trials.html`'s search form is `method="get"` and is correctly CSRF-exempt, so it
   was left as-is.) This required no new bean — `${_csrf}` is already available in every
   Thymeleaf template as a plain request attribute that Spring MVC exposes to the view.

**A separate, non-code issue seen in the same run:** every JDBC-backed test (AUTH-10, SEC-04,
ADMIN-01, NOTIF-01, CONT-01, and all of `DatabaseIntegrityAutomationTest`) failed with
`Access denied for user 'clintrial_app'@'localhost' (using password: YES)`. This was **not** a
CSRF or app bug — it happens whenever `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` were
exported in the terminal that ran `mvn spring-boot:run`, but a *different*, freshly-opened
terminal was then used to run `mvn -Dtest=...` — shell-exported env vars never carry over to a
new terminal window/tab. `DbSupport.connect()` now fails fast with an explicit message telling
you to re-export those vars in that terminal, instead of silently falling back to a wrong
default password and producing a confusing MySQL error.

**One anomaly worth re-testing after these fixes**: that same run also showed `rbac01`,
`rbac02`, and `sec07` (anonymous GET to a protected URL) returning `403` where Spring
Security's documented default behavior — and what `SecurityConfig` here relies on with no
custom `AccessDeniedHandler`/`AuthenticationEntryPoint` — is a `302` redirect to `/login` for
an unauthenticated request. Nothing in this codebase overrides that default, so this doesn't
have a confirmed code-level explanation yet; it may simply have been a downstream symptom of
that run's much larger CSRF-driven breakage. Re-run `RoleBasedAccessAutomationTest` and
`SecurityAutomationTest.sec07` after a clean rebuild + app restart; if `403` still shows up,
that's genuine new signal worth digging into further with the fresh stack trace.
