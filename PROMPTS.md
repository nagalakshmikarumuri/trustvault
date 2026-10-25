# TrustVault AI Prompt and Usage Log

This document records representative AI-assisted development work on TrustVault. Entries summarize the prompts and the actual development process; they are not verbatim transcripts.

## Entry 1 — 2026-10-19 — Initial Project Structure

**Prompt:** Help build the Snovasys TrustVault secure document-sharing application using Java, Spring Boot, MySQL, Thymeleaf, and Spring Security.

**AI output:** Suggested project structure, entities, repositories, controllers, authentication, and database migrations.

**What was wrong or incomplete:** The initial implementation did not yet cover every required feature and security scenario.

**What I did instead:** Continued development in the existing project and used build and test results to identify missing functionality.

## Entry 2 — 2026-10-20 — Authentication and Access Control

**Prompt:** Review the existing TrustVault implementation and improve authentication and server-side authorization.

**AI output:** Helped implement Spring Security configuration, user details loading, password hashing, and protected routes.

**What was wrong or incomplete:** Route protection alone does not guarantee that users cannot access documents belonging to other users.

**What I did instead:** Focused on document-level ownership and permission checks and ran automated tests to verify access-control behavior.

## Entry 3 — 2026-10-21 — Document Storage and Upload Validation

**Prompt:** Improve secure document upload, storage, listing, and file validation in the existing application.

**AI output:** Helped implement document storage services, metadata persistence, upload restrictions, and document-related tests.

**What was wrong or incomplete:** Upload validation and file storage needed additional edge-case testing, including invalid paths and resource handling.

**What I did instead:** Ran the Maven test suite and addressed the failures reported by the tests.

## Entry 4 — 2026-10-22 — Permissions and Audit Logging

**Prompt:** Add document-specific READ and EDIT permissions, permission management, and audit logging while preserving existing functionality.

**AI output:** Helped develop permission-related operations, authorization checks, and activity-tracking components.

**What was wrong or incomplete:** Permission management must be enforced on the server, and audit coverage must be verified for each sensitive operation.

**What I did instead:** Reviewed the implemented behavior and used automated tests to check authorization and document operations. Further verification is required before claiming complete security coverage.

## Entry 5 — 2026-10-25 — Testing and Submission Preparation

**Prompt:** Finish the remaining TrustVault submission requirements, preserve the existing implementation, and run the full Maven test suite.

**AI output:** Helped identify submission documentation requirements and prepare the project for testing and GitHub publication.

**What was wrong or incomplete:** The required THREATS.md and PROMPTS.md documents were missing from the local project and therefore were not present in the initial GitHub upload.

**What I did instead:** Created THREATS.md locally and prepared this prompt log. The documentation must be committed and pushed to GitHub before submission.

## Verification Note

These entries are summaries, not verbatim prompt transcripts. Dates and descriptions should be checked against actual development history before submission. Do not treat a passing test suite as proof that every security requirement has been independently verified.
