# TrustVault Security Threat Model

## 1. Overview

TrustVault is a secure document management and sharing application. It supports user authentication, document uploads, document access permissions, and document activity tracking.

## 2. Assets to Protect

- User credentials and authentication sessions
- Uploaded documents and their contents
- Document ownership and sharing permissions
- Audit logs and activity records
- Database configuration and secrets

## 3. Threats and Mitigations

### Unauthorized Document Access
**Threat:** A user attempts to view, download, edit, or delete another user's document without permission.

**Mitigation:** Enforce authentication, document ownership checks, and explicit document permissions on the server for every protected operation.

### Broken Access Control
**Threat:** A user changes a document ID or URL to access another document.

**Mitigation:** Verify the current user's permissions for the requested document on every request. Do not rely only on hiding UI controls.

### Credential Theft
**Threat:** An attacker obtains a user's password.

**Mitigation:** Store passwords using a strong one-way password hash such as BCrypt, never as plaintext. Use secure session management and restrict authentication attempts where implemented.

### Malicious File Upload
**Threat:** A user uploads an oversized, malformed, or dangerous file.

**Mitigation:** Validate file size, file names, and permitted file types; generate server-side storage names; and store uploaded files outside publicly accessible directories.

### Path Traversal
**Threat:** An attacker submits a crafted file name or path to read files outside the permitted storage directory.

**Mitigation:** Use server-generated storage names, normalize paths, and verify that resolved file paths remain within the configured storage directory.

### Cross-Site Request Forgery (CSRF)
**Threat:** A malicious website tricks an authenticated user into submitting an unwanted state-changing request.

**Mitigation:** Keep Spring Security CSRF protection enabled for browser-based state-changing requests.

### Session Abuse
**Threat:** An attacker reuses a compromised session.

**Mitigation:** Use framework-managed sessions, invalidate sessions at logout, and use secure cookie settings appropriate to the deployment environment.

### Audit Log Tampering
**Threat:** A user attempts to modify or remove records of sensitive document activity.

**Mitigation:** Restrict audit-log access to authorized administrators and prevent ordinary users from modifying audit records. Stronger tamper-evident or append-only storage may be required for higher assurance.

### Information Leakage
**Threat:** Error messages or logs reveal credentials, document contents, or internal paths.

**Mitigation:** Return safe error messages, avoid logging secrets, and keep sensitive configuration in environment variables.

## 4. Security Verification

The project should be tested for:

- Access by unauthenticated users
- Cross-user document access
- Unauthorized downloads, edits, and deletions
- Permission granting and revocation
- Invalid and oversized uploads
- Path traversal attempts
- Audit events for sensitive operations

## 5. Limitations and Deployment Notes

This document describes intended threats and mitigations; it is not a claim that every control has been independently audited. Verify the actual implementation and automated tests before submission.

Use HTTPS in deployment, keep database credentials out of source control, restrict production database access, and configure backups and appropriate file-storage permissions.
