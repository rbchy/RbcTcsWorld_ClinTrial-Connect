# RbcTcsWorld_ClinTrial-Connect
ClinTrial Connect

Java Spring Boot MySQL Maven JUnit5 Cucumber Allure License

Author: Ranajit Baran Chowdhury — QA Automation Engineer Email: chyranajit@gmail.com GitHub: @rbchy

A health-sector clinical-trial patient-matching web application built with **Java (Spring Boot 3, Java 17+) + MySQL**, modeled loosely on real-world platforms like siterx.com. Patients register with their condition and location, physicians publish and manage trials, and a scoring algorithm matches the two — all behind role-based login (ADMIN / PHYSICIAN / PATIENT). Unlike PPES (a production-shaped tool built to run a real packaging line), this one exists purpose-built as a **QA/testing practice project**: a fully self-owned system that can legitimately take a full security, load, and automation test suite — including the kind of testing (SQLi attempts, forced browsing, scraping, load testing) that would never be appropriate to run against someone else's live site.

📋 Quick Overview

| | | |---|---| | Backend | Java 17+, Spring Boot 3.3 (Web MVC, Spring Data JPA, Spring Security, Bean Validation) | | Database | MySQL 8 for real runs (schema-mysql.sql) — H2 in-memory for the entire test suite | | Views | Thymeleaf (server-rendered), plain CSS | | Auth | Form login, BCrypt password hashing, role-based access control, CSRF on every form | | Testing | JUnit 5 + MockMvc + Cucumber BDD + Selenium + JMeter, reported via Allure | | Build | Maven |

✨ Key Features

🔐 Login / Auth & Role-Based Access Control

Form login backed by CustomUserDetailsService, BCrypt-hashed passwords, CSRF protection on every state-changing form
Three roles — ADMIN, PHYSICIAN, PATIENT — enforced both at the URL level (SecurityConfig) and again per-method (@PreAuthorize) as defense in depth
Every real route in the app is enumerated explicitly; anything else falls through to a normal 404 instead of being masked behind a login redirect
🧑‍⚕️ Patients, Physicians & Clinical Trials

Patient registration: condition, date of birth, location, consent-to-contact
Physician registration: license number, specialty — stays unverified until an admin approves them
Clinical trials: eligibility criteria (age range, gender, condition, location), DRAFT → RECRUITING → CLOSED lifecycle, admin/physician-managed
🧮 Matching Algorithm

Scores every RECRUITING trial against a patient's profile (0–100): condition match (exact/partial), state/city bonuses, hard gates on age range and gender eligibility (MatchingService)
Runs live on each visit to a patient's Matches page and persists results, filtering out any trial that has since left RECRUITING
📬 Contact, Notifications & Admin Console

Public contact form (validated, admin-reviewable)
Per-user in-app notification feed
Admin dashboard: live counts, physician verification, trial status control, contact request review
Audit log for key account/security events
✅ Testing — the real point of this project

As of the latest local run: 115 automated tests across 23 suites, 100% passing, reported through Allure. The suite spans every layer on purpose:

Pure unit tests (matching algorithm, Bean Validation constraints) — no Spring context
@DataJpaTest repository-slice tests for every custom query method
@SpringBootTest service-layer integration tests against H2
MockMvc controller tests (routing, form binding/validation, redirects, persisted effects)
Security tests: forced-browsing / role-escalation checks, SQL-injection-safety of the login form
Cucumber BDD scenarios (Gherkin .feature files) running as part of the same mvn test
Selenium + raw-HTTP end-to-end automation mapped 1:1 to a manual test-case matrix
Plain-JDBC database-integrity checks and an Apache JMeter load-test plan
One combined Allure report across the JUnit and Cucumber results
See Getting Started below to run it, and Docs in this repo for the full traceability matrix and defect log.

📂 Repository Structure

Clintrial-Connect/
├── pom.xml                            Maven build (Spring Boot 3.3, Java 17, Allure + Cucumber wiring)
├── src/main/java/com/clintrial/
│   ├── controller/                     7 MVC controllers (Home, Trial, Contact, Registration, Admin, Physician, Patient)
│   ├── service/                        Business logic (Matching, Trial, Registration, Contact, Notification, Audit)
│   ├── model/                          JPA entities (AppUser, Patient, Physician, ClinicalTrial, TrialMatch, ...)
│   ├── repository/                     Spring Data JPA repositories
│   ├── dto/                            Form-backing objects with Bean Validation constraints
│   ├── security/                       UserPrincipal / CustomUserDetailsService
│   └── config/                         SecurityConfig, DemoDataSeeder ("demo" profile only)
├── src/main/resources/
│   ├── templates/                       Thymeleaf views
│   └── schema-mysql.sql                 MySQL schema (see "Running it locally")
├── src/test/java/com/clintrial/
│   ├── dto/                             Bean Validation unit tests (*ValidationTest)
│   ├── service/                         Service-layer integration tests
│   ├── repository/                      @DataJpaTest repository-slice tests
│   ├── controller/                      MockMvc controller tests + PublicPagesTest
│   ├── security/                        RoleAccessSecurityTest (RBAC / SQLi-safety)
│   ├── cucumber/                        Step definitions + RunCucumberTest (JUnit Suite entry point)
│   ├── selenium/                        Page-Object-Model UI automation (excluded from default `mvn test`)
│   └── dbintegrity/                     Plain-JDBC DB-integrity checks (needs live MySQL)
├── src/test/resources/
│   ├── features/                        Gherkin .feature files (Cucumber scenarios)
│   └── application-test.properties      H2 test datasource config
├── perf/                                Apache JMeter load-test plan
├── README-AUTOMATION.md                 Manual-test-case <-> automated-test traceability matrix
├── সেটআপ_গাইড.md                        Bangla setup/run guide
└── ClinTrial_Connect_Test_Cases.xlsx     Manual test case matrix
🚀 Getting Started

Prerequisites

JDK 17+ and Maven (running the app or its test suite)
MySQL 8.x (only needed to run the real app — the test suite uses in-memory H2 and needs nothing extra)
Run it locally (with demo data)

# 1. Create the database + app user, then load the schema (see the header of schema-mysql.sql —
#    it deliberately uses the DEFAULT auth plugin, not "IDENTIFIED WITH mysql_native_password",
#    since that plugin is disabled/removed on MySQL 8.4+/9.x; ERROR 1524 means follow the
#    plain "IDENTIFIED BY" version in that header instead)
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS clintrial_connect CHARACTER SET utf8mb4;"
mysql -u root -p -e "DROP USER IF EXISTS 'clintrial_app'@'%'; CREATE USER 'clintrial_app'@'%' IDENTIFIED BY 'ClinTrialQA@2026'; GRANT ALL PRIVILEGES ON clintrial_connect.* TO 'clintrial_app'@'%'; FLUSH PRIVILEGES;"
mysql -u clintrial_app -p clintrial_connect < src/main/resources/schema-mysql.sql

# 2. Point the app at it (defaults shown; override via env vars or application-mysql.properties)
export DB_HOST=localhost DB_PORT=3306 DB_NAME=clintrial_connect DB_USER=clintrial_app DB_PASSWORD='ClinTrialQA@2026'

# 3. Run with demo accounts + 2 sample trials seeded
mvn spring-boot:run -Dspring-boot.run.profiles=mysql,demo
Demo accounts (password Password123! for all three): admin@clintrial.test (ADMIN), dr.patel@clintrial.test (PHYSICIAN, pre-verified), patient1@clintrial.test (PATIENT). Full Bangla step-by-step walkthrough, including common connection errors: `সেটআপ_গাইড.md`.

Run the test suite (H2, no MySQL needed)

mvn test                                   # everything: unit, repository, service, MockMvc,
                                            # security, and Cucumber BDD -- 115/115 as of the
                                            # last run
mvn -Dtest=MatchingServiceTest test        # just the matching-algorithm unit tests
mvn -Dtest=RunCucumberTest test             # just the Cucumber BDD scenarios

# Selenium + raw-HTTP end-to-end suite (needs a running app; excluded from the default run):
mvn spring-boot:run -Dspring-boot.run.profiles=mysql,demo &
mvn -Dtest="selenium.**" -DfailIfNoTests=false -Dbase.url=http://localhost:8080 test

# Plain-JDBC database-integrity suite (needs a live MySQL with schema-mysql.sql applied):
mvn -Dtest="dbintegrity.**" -DfailIfNoTests=false test
For the last two commands, DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD must be exported in the same terminal that runs them — shell-exported env vars don't carry over from another tab, even one where the app itself is already running against the same database.

Allure report (JUnit + Cucumber combined)

mvn test                                                    # (re)generates target/allure-results
mvn io.qameta.allure:allure-maven:2.15.0:serve              # builds + opens the report (Ctrl+C to stop)
The fully-qualified goal is used deliberately instead of the shorter mvn allure:serve — that short form depends on Maven resolving the allure plugin-prefix from Central, which doesn't reliably succeed on every setup. The fully-qualified goal always works.

📚 Docs in this repo

`README.md` (this file) — what the app is, how to run it, how to run the tests.
`README-AUTOMATION.md` — full traceability matrix (every manual Test Case ID → automated test), plus a log of real defects the automated suite found in the app and how each was handled (fixed outright, or documented as an intentional, judgment-call assertion).
`সেটআপ_গাইড.md` — step-by-step install/run/test guide in Bangla.
`ClinTrial_Connect_Test_Cases.xlsx` — the manual test case matrix everything above maps to.
Known open item: an earlier from-scratch Selenium/HTTP run showed every page-dependent test failing at once (forms not found, CSRF field missing) — current source looks correct (SecurityConfig should return 302 for anonymous requests, CSRF fields are in place), so this was most likely the app not actually being up yet at localhost:8080, or serving a stale build. Before re-running that suite: confirm curl http://localhost:8080/login returns the real login form from a fresh mvn spring-boot:run.

🛠️ Tech Stack

App: Java 17, Spring Boot 3.3 (Web MVC, Spring Data JPA, Spring Security, Validation), Thymeleaf, MySQL 8 / H2, Maven.

Testing: JUnit 5, MockMvc, Spring Security Test, Cucumber 7 (cucumber-spring), Selenium 4 (Page Object Model), Apache JMeter, Allure 2 (JUnit 5 + Cucumber listeners).

📄 License

MIT — matching this author's other repositories (add a LICENSE file before making this repository public).
