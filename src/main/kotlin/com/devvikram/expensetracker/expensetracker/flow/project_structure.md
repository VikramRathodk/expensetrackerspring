# Expense Tracker — Project Structure

## Overview

A **Spring Boot + Kotlin** backend for personal expense tracking with JWT authentication, role-based access control (RBAC), dynamic filtering, and financial reporting. The app is containerized with Docker and backed by PostgreSQL.

| Attribute | Value |
|---|---|
| Language | Kotlin 2.2.21 |
| Framework | Spring Boot 4.0.0 |
| Java | 21 |
| Database | PostgreSQL |
| Build Tool | Gradle (Kotlin DSL) |
| Auth | JWT (jjwt 0.12.3, HS256, 24h expiry) |

---

## Directory Tree

```
expensetrackerspring/
├── src/
│   ├── main/
│   │   ├── kotlin/com/devvikram/expensetracker/expensetracker/
│   │   │   ├── config/                    # App initialization & env config
│   │   │   ├── controllers/               # REST API layer
│   │   │   ├── dto/
│   │   │   │   ├── request/               # Inbound request payloads
│   │   │   │   └── response/              # Outbound response payloads
│   │   │   ├── entity/                    # JPA entities (DB models)
│   │   │   ├── enums/                     # Shared enumerations
│   │   │   ├── exceptions/                # Custom exceptions + global handler
│   │   │   ├── repository/                # Data access interfaces
│   │   │   ├── security/
│   │   │   │   └── anotation/             # Role-based security annotations
│   │   │   ├── service/                   # Business logic
│   │   │   ├── specifications/            # JPA Specifications for filtering
│   │   │   ├── flow/                      # Documentation files (this folder)
│   │   │   │   └── ExpenseTestings/       # Postman collections & test cases
│   │   │   └── ExpensetrackerApplication.kt
│   │   └── resources/
│   │       ├── application.properties     # Base config
│   │       ├── application-dev.properties # Dev profile
│   │       └── application-prod.properties# Prod profile
│   └── test/
│       └── kotlin/...
│           └── ExpensetrackerApplicationTests.kt
├── build.gradle.kts                       # Gradle build + dependencies
├── settings.gradle.kts
├── Dockerfile                             # Multi-stage Docker build
└── .env                                   # Local env vars (not committed)
```

---

## Layer-by-Layer Breakdown

### `config/`

| File | Responsibility |
|---|---|
| `DataInitializer.kt` | Runs on startup: creates default roles and seeds the super-admin user if they don't exist. |
| `EnvConfig.kt` | Loads `.env` file via `dotenv-kotlin`; validates that all required variables (`JWT_SECRET`, `DATABASE_URL`, `PGUSER`, `PGPASSWORD`, `PORT`) are present before the context starts. |

---

### `controllers/`

REST controllers — thin layer that delegates all logic to services.

| File | Base Path | Key Endpoints |
|---|---|---|
| `AuthController.kt` | `/api/auth` | `POST /register`, `POST /login`, `GET /me`, `POST /assign-roles`, `GET /roles` |
| `ExpenseController.kt` | `/api/expenses` | Full CRUD + `GET /search`, `GET /filter/category`, `GET /filter/amount`, `GET /filter/date-range`, `POST /filter` (advanced) |
| `AdminCategoryController.kt` | `/api/admin/categories` | `POST`, `PUT /{id}`, `DELETE /{id}` — global categories (ADMIN role) |
| `UserCategoryController.kt` | `/api/categories` | `POST`, `GET`, `PUT /{id}`, `DELETE /{id}` — per-user categories |
| `ReportController.kt` | `/api/reports` | `GET /summary`, `GET /category-wise`, `GET /date-wise`, `POST /custom` (SUPER_ADMIN role) |
| `BudgetController.kt` | `/api/budgets` | `POST`, `GET`, `GET /{id}/status`, `PUT /{id}`, `DELETE /{id}` |
| `RecurringExpenseController.kt` | `/api/recurring-expenses` | `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |

---

### `dto/`

Plain data classes with no business logic, used to decouple the API surface from internal entities.

#### Request DTOs (`dto/request/`)

| File | Fields |
|---|---|
| `LoginRequest.kt` | `email`, `password` |
| `RegisterRequest.kt` | `name`, `email`, `password`, `roles?` |
| `ExpenseRequest.kt` | `title`, `amount`, `categoryId`, `userId`, `note` |
| `ExpenseFilterRequest.kt` | `searchTitle?`, `categoryId?`, `minAmount?`, `maxAmount?`, `startDate?`, `endDate?`, `year?`, `month?` |
| `CustomReportRequest.kt` | `startDate`, `endDate`, `categoryIds`, `minAmount?`, `maxAmount?` |
| `AssignRoleRequest.kt` | `userId`, `roles: Set<RoleType>` |
| `BudgetRequest.kt` | `CreateBudgetRequest`: `categoryId?`, `amount`, `period`, `startDate`, `endDate?`, `alertThreshold=0.80` — `UpdateBudgetRequest`: `amount?`, `alertThreshold?`, `endDate?`, `isActive?` |
| `RecurringExpenseRequest.kt` | `CreateRecurringExpenseRequest`: `title`, `amount`, `categoryId`, `frequency`, `startDate`, `endDate?`, `note?` — `UpdateRecurringExpenseRequest`: all fields nullable + `nextDueDate?`, `isActive?` |

#### Response DTOs (`dto/response/`)

| File | Fields |
|---|---|
| `ApiResponse<T>` | Generic wrapper — `status: Boolean`, `message: String`, `data: T` |
| `AuthResponse.kt` | `token`, `user: UserResponse` |
| `UserResponse.kt` | `id`, `name`, `email`, `roles`, `isActive`, `createdAt` |
| `ExpenseResponse.kt` | `id`, `title`, `amount`, `categoryId`, `categoryName`, `note`, `createdAt` |
| `SummaryReportResponse.kt` | `totalExpenses`, `count`, `averageAmount` |
| `CategoryWiseReportResponse.kt` | `categoryName`, `total`, `count`, `percentage` |
| `DateWiseReportResponse.kt` | `date`, `total`, `count` |
| `BudgetResponse.kt` | `id`, `categoryId?`, `categoryName?`, `amount`, `period`, `startDate`, `endDate?`, `alertThreshold`, `isActive`, `spent`, `remaining`, `percentUsed`, `createdAt` |
| `BudgetStatusResponse.kt` | `id`, `categoryId?`, `categoryName?`, `limit`, `spent`, `remaining`, `percentUsed`, `isOverBudget`, `isNearLimit` |
| `RecurringExpenseResponse.kt` | `id`, `title`, `amount`, `categoryId`, `categoryName`, `frequency`, `nextDueDate`, `endDate?`, `isActive`, `note?`, `createdAt` |

---

### `entity/`

JPA-annotated classes mapped to PostgreSQL tables.

| File | Table | Key Fields / Relationships |
|---|---|---|
| `User.kt` | `users` | `email (unique)`, `name`, `password (BCrypt)`, `isActive`, `createdAt`, `updatedAt` — M:M → `Role` |
| `Role.kt` | `roles` | `name: RoleType (enum)` — M:M → `User` |
| `Category.kt` | `categories` | `name`, `description`, `isGlobal: Boolean`, `userId (nullable)` — global categories have `userId = null` |
| `Expense.kt` | `expenses` | `title`, `amount (positive)`, `note`, `createdAt`, `userId` — M:1 → `Category` |
| `Budget.kt` | `budgets` | `userId`, `categoryId? (nullable = overall)`, `amount`, `period: BudgetPeriod`, `startDate`, `endDate?`, `alertThreshold=0.80`, `isActive`, `deletedAt?`, `createdAt` — M:1 → `Category` |
| `RecurringExpense.kt` | `recurring_expenses` | `userId`, `title`, `amount`, `frequency: RecurringFrequency`, `nextDueDate`, `endDate?`, `isActive`, `note?`, `deletedAt?`, `createdAt` — M:1 → `Category` |

---

### `enums/`

| File | Values |
|---|---|
| `RoleType.kt` | `USER`, `ADMIN`, `SUPER_ADMIN`, `MODERATOR`, `ACCOUNTANT`, `VIEWER` |
| `BudgetPeriod.kt` | `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` |
| `RecurringFrequency.kt` | `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` |

---

### `exceptions/`

| File | Type / HTTP Status |
|---|---|
| `ResourceNotFoundException.kt` | Custom — mapped to **404** |
| `BadRequestException.kt` | Custom — mapped to **400** |
| `ConflictException.kt` | Custom — mapped to **409** (e.g. duplicate category) |
| `GlobalExceptionHandler.kt` | `@RestControllerAdvice` — catches all the above plus Spring/JWT exceptions and returns a consistent `ApiResponse` JSON error body |

**JWT exceptions handled:**
- `ExpiredJwtException` → 401
- `MalformedJwtException` → 400
- `SignatureException` → 401
- `AuthenticationException` → 401

---

### `repository/`

All interfaces extend `JpaRepository`; `ExpenseRepository` also extends `JpaSpecificationExecutor` for dynamic queries.

| Interface | Notable Custom Methods |
|---|---|
| `UserRepository` | `findByEmail()`, `existsByEmail()`, `findByEmailWithRoles()`, `findByRoleType()` |
| `ExpenseRepository` | `findByUserId()`, `sumAmountByUserIdAndCategoryIdAndDateBetween()`, `sumAmountByUserIdAndDateBetween()` — filtering uses Specifications |
| `CategoryRepository` | Queries for global categories, user-specific categories, and combined lists |
| `RoleRepository` | `findByName()`, `existsByName()` |
| `ReportRepository` | Custom JPQL/native queries for `summaryReport()`, `categoryWiseReport()`, `getDateWiseReport()` |
| `BudgetRepository` | `findAllActiveBudgets(userId, today)` — filters `isActive=true`, `deletedAt=null`, `startDate<=today`, `endDate>=today OR null`; `findByIdAndUserIdAndDeletedAtIsNull()` |
| `RecurringExpenseRepository` | `findByUserIdAndIsActiveTrueAndDeletedAtIsNull()`, `findByIdAndUserIdAndDeletedAtIsNull()`, `findAllDueToday(today)` — JPQL query picks up entries where `nextDueDate <= today` and `endDate >= today OR null` |

---

### `security/`

| File | Responsibility |
|---|---|
| `SecurityConfig.kt` | Declares public endpoints (`/api/auth/register`, `/api/auth/login`, `/api/auth/roles`); all others require authentication; stateless session; plugs in `JwtAuthenticationFilter`. |
| `JwtUtil.kt` | Generates and validates JWT tokens; extracts claims (`userId`, `email`, `roles`); uses HS256; 24-hour expiry. |
| `JwtAuthenticationFilter.kt` | `OncePerRequestFilter` — extracts `Bearer` token from `Authorization` header, validates it, and sets `SecurityContextHolder`. |
| `JwtAuthenticationEntryPoint.kt` | Returns a JSON `401` response when authentication fails. |
| `SecurityUtil.kt` | Helper methods for getting the current authenticated user from the security context. |
| `CustomUserDetailsService.kt` | Implements `UserDetailsService`; loads user by email or ID; bridges JPA `User` entity with Spring Security. |

#### Security Annotations (`security/anotation/`)

Custom meta-annotations that combine `@PreAuthorize` with a role check, keeping controllers clean.

| Annotation | Required Role / Condition |
|---|---|
| `@IsAuthenticated` | Any authenticated user |
| `@IsAdmin` | `ADMIN` |
| `@IsSuperAdmin` | `SUPER_ADMIN` |
| `@IsModerator` | `MODERATOR` |
| `@IsAccountant` | `ACCOUNTANT` |
| `@IsOwner` | Resource ownership verified at runtime |

---

### `service/`

| File | Key Responsibilities |
|---|---|
| `AuthService.kt` | `register()` (hashes password, assigns default role), `login()` (validates credentials, issues JWT), `assignRoles()` (SUPER_ADMIN only) |
| `ExpenseService.kt` | CRUD for expenses; runs `checkBudgetOnExpense()` before every create; uses `ExpenseSpecifications` for dynamic filter queries; pagination via `Pageable` |
| `CategoryService.kt` | Manages both global (admin-controlled) and user-specific categories; prevents duplicate names within the same scope |
| `RoleService.kt` | Role lookup, creation, default role initialization |
| `ReportService.kt` | Aggregates data via `ReportRepository`: summary totals, category breakdown, date breakdown, custom-range reports |
| `CustomUserDetailsService.kt` | Spring Security integration — see Security section above |
| `BudgetService.kt` | CRUD for budgets; `getBudgetStatus()` computes live `spent/remaining/percentUsed`; `checkBudgetOnExpense()` evaluates all active budgets for a user before an expense is saved — returns `BudgetCheckResult(shouldBlock, warnings)`; `resetPeriodicBudgets()` scheduler runs at `00:00` daily to advance `startDate` for elapsed periods |
| `BudgetCheckResult.kt` | Simple data holder: `shouldBlock: Boolean`, `warnings: List<String>` — returned by `BudgetService.checkBudgetOnExpense()` |
| `RecurringExpenseService.kt` | CRUD for recurring expenses; `processRecurringExpenses()` scheduler runs at `00:05` daily — finds all entries due today, auto-creates an `Expense` via `ExpenseService` (triggering budget checks), then advances `nextDueDate` and auto-deactivates entries past their `endDate`; failures are isolated per entry |

---

### `specifications/`

| File | Purpose |
|---|---|
| `ExpenseSpecifications.kt` | Produces `Specification<Expense>` predicates for each filter dimension (user, title, category, amount range, date range, month/year). `buildFilterSpecification()` combines them with `and` for use in `ExpenseRepository.findAll(spec, pageable)`. |

---

## Configuration Files

### `application.properties` (base)

```properties
spring.application.name=expensetracker
server.port=${PORT:8081}
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${PGUSER}
spring.datasource.password=${PGPASSWORD}
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000          # 24 hours in ms
spring.jpa.database-platform=PostgreSQLDialect
# HikariCP pool: max=10, minIdle=5, connectionTimeout=20s
```

### `application-dev.properties`
- `ddl-auto=update` — Hibernate creates/alters tables automatically
- SQL logging at `DEBUG` level
- Full error details exposed in responses

### `application-prod.properties`
- `ddl-auto=validate` — schema must match entities; no auto-migration
- SQL logging at `WARN` level
- Error details hidden from API responses

---

## Dockerfile (Multi-stage)

```
Stage 1 — Builder (gradle:8.14-jdk21)
  └── gradle clean bootJar --no-daemon

Stage 2 — Runtime (eclipse-temurin:21-jre-alpine)
  ├── Non-root user: spring:spring
  ├── Exposes port 8080
  └── JVM flags: -Xmx512m -Xms256m
```

All secrets are passed as environment variables at runtime; nothing is baked into the image.

---

## Required Environment Variables

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | Signing key for JWT tokens |
| `DATABASE_URL` | Full JDBC URL for PostgreSQL |
| `PGUSER` | Database username |
| `PGPASSWORD` | Database password |
| `PORT` | HTTP port (default `8081`) |

---

## Architecture Flow

```
HTTP Request
     │
     ▼
JwtAuthenticationFilter          ← validates Bearer token, sets SecurityContext
     │
     ▼
SecurityConfig (route guards)    ← enforces role annotations
     │
     ▼
Controller                       ← parses request DTO, calls service
     │
     ▼
Service                          ← business logic, validation, orchestration
     │         │
     │         └── BudgetService.checkBudgetOnExpense()   ← called by ExpenseService
     │               on every expense create (block or warn)
     │
     ▼
Repository / Specifications      ← database queries (JPA + dynamic specs)
     │
     ▼
PostgreSQL
     │
     ▼
Response DTO → ApiResponse<T>    ← wrapped in generic envelope, returned as JSON

─────────────────────────────────────────────────────────────────
Scheduled Jobs (Spring @Scheduled)
─────────────────────────────────────────────────────────────────
00:00 daily  →  BudgetService.resetPeriodicBudgets()
                  Advances startDate for budgets whose period window has elapsed

00:05 daily  →  RecurringExpenseService.processRecurringExpenses()
                  Finds all due recurring entries → creates Expense via ExpenseService
                  (budget check included) → advances nextDueDate → auto-deactivates
                  entries past endDate; each entry processed independently
```

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| Stateless JWT | No server-side session storage; horizontally scalable |
| `ApiResponse<T>` wrapper | Consistent envelope for all responses — success and error |
| JPA Specifications | Avoids N query methods; builds complex filters dynamically at runtime |
| Global vs. User categories | System-wide defaults managed by admins; users can extend with private categories |
| Custom security annotations | Reduces boilerplate; centralises role logic away from controller methods |
| `ddl-auto=validate` in prod | Prevents accidental schema changes; migrations handled manually or via Flyway |
| Multi-stage Docker build | Keeps the final image small (JRE only); build tools not shipped to production |
| Budget check before expense save | `ExpenseService` calls `BudgetService.checkBudgetOnExpense()` before persisting any expense — both category-scoped and overall budgets are evaluated; result carries `shouldBlock` + user-facing `warnings` |
| Soft delete on budgets & recurring expenses | `deletedAt` timestamp instead of hard delete — preserves audit trail; all queries filter `deletedAt IS NULL` |
| `BudgetCheckResult` data holder | Decouples the blocking/warning decision from the caller — `ExpenseService` decides what to do with `shouldBlock` and `warnings` |
| Recurring expense scheduler at 00:05 | Fires 5 minutes after the budget reset scheduler (00:00) to ensure period windows are current before auto-expenses are created |
| Per-entry failure isolation in scheduler | Each recurring expense is processed in its own try/catch — one bad entry (e.g. deleted category) cannot block other users' auto-expenses |
| `nextDueDate` as the scheduler trigger | Simple `nextDueDate <= today` query; the scheduler advances this field after each fire — no cron expression needed per entry |

---

## flow/ExpenseTestings — Documentation & Testing Artifacts

| File | Purpose |
|---|---|
| `expense_test_cases.md` | Test cases for `ExpenseController` and `ExpenseService` |
| `budget_postman_collection.json` | Importable Postman collection for all `/api/budgets` endpoints |
| `budget_test_cases.md` | Test cases for `BudgetController`, `BudgetService`, `BudgetRepository`, scheduler reset, and budget check on expense |
| `recurring_expense_postman_collection.json` | Importable Postman collection for all `/api/recurring-expenses` endpoints |
| `recurring_expense_test_cases.md` | Test cases for `RecurringExpenseController`, `RecurringExpenseService`, `RecurringExpenseRepository`, and the daily scheduler |
| `audit_log_postman_collection.json` | Importable Postman collection for all `/api/audit-logs` endpoints |
| `audit_log_test_cases.md` | Test cases for `AuditLogController`, `AuditLogService`, `AuditLogRepository`, and audit integration across all services |

**Postman setup (all collections):**
1. Import the `.json` file into Postman.
2. Create an environment with `base_url = http://localhost:8081`, `token = ""`, and the relevant id variable (e.g. `budget_id`, `recurring_id`).
3. Run **Login** first — the test script auto-saves the JWT to `{{token}}`.
4. All other requests inherit Bearer auth from the collection root.
