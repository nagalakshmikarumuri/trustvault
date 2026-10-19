# TrustVault - Secure Document Sharing Application

> **Snovasys Fresher Hiring Challenge 2026**

TrustVault is an enterprise document-sharing web application built with **Java 21**, **Spring Boot 3**, **Spring Security**, **Spring Data JPA**, **MySQL**, and **Thymeleaf**.

---

## Current Status: Phase 1 (Baseline Project Structure)

In this initial phase, the fundamental Spring Boot project structure and layered architecture have been established without business logic, database coupling, or active security locks.

### Layered Architecture
- **`com.trustvault.config`**: Spring bean configuration and environment setup.
- **`com.trustvault.controller`**: REST APIs and MVC web endpoints.
- **`com.trustvault.service`**: Business logic, permission validations, and transaction boundaries.
- **`com.trustvault.repository`**: Spring Data JPA database repositories.
- **`com.trustvault.entity`**: JPA domain entities (User, Role, Document, DocumentPermission, AuditLog).
- **`com.trustvault.security`**: Authentication providers, user details services, and RBAC rules.
- **`com.trustvault.dto`**: Data Transfer Objects for request and response payloads.
- **`com.trustvault.exception`**: Custom exceptions and `@ControllerAdvice` global exception handling.

---

## How to Run the Application

### Option A: Using the Maven Wrapper (Command Line)
In PowerShell or Command Prompt within the project root:

```powershell
# Run the application
.\mvnw.cmd spring-boot:run
```

Or if using Linux / macOS / Git Bash:
```bash
./mvnw spring-boot:run
```

### Option B: Using an IDE (Eclipse / VS Code / IntelliJ)
1. Open Eclipse, VS Code, or IntelliJ.
2. Select **File > Import > Existing Maven Projects** (or open the project folder in VS Code / IntelliJ).
3. Navigate to `c:\Users\hp\OneDrive\Desktop\trustvault`.
4. Run `TrustVaultApplication.java` as a **Java Application** or **Spring Boot App**.

---

## Endpoints to Test

Once the application is running, open your web browser or an API client (like Postman or curl):

1. **Home Page**: [http://localhost:8080/](http://localhost:8080/)
   - Renders the Thymeleaf web dashboard with application status and architecture layout.
2. **Health Check REST API**: [http://localhost:8080/api/health](http://localhost:8080/api/health)
   - Returns a JSON payload:
     ```json
     {
       "status": "UP",
       "application": "TrustVault",
       "version": "0.0.1-SNAPSHOT",
       "environment": "Development",
       "timestamp": "2026-10-19T..."
     }
     ```

---

## Running Automated Tests

To run the automated integration tests:
```powershell
.\mvnw.cmd test
```
All tests verify that the Spring Boot ApplicationContext starts cleanly and that both `/` and `/api/health` return HTTP 200.
