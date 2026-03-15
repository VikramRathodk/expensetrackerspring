# Expense Tracker — Project Structure

## Overview

A production-grade **Spring Boot + Kotlin** backend for personal expense tracking with JWT authentication, role-based access control (RBAC), dynamic filtering, financial reporting, multi-currency support, recurring expenses, budget enforcement, in-app notifications, tags/labels, and audit logging. Containerized with Docker and backed by PostgreSQL.

| Attribute | Value |
|---|---|
| Language | Kotlin 2.2.21 |
| Framework | Spring Boot 4.0.0 / Spring 7 |
| Java | 21 |
| Database | PostgreSQL |
| Build Tool | Gradle (Kotlin DSL) |
| Auth | JWT (jjwt 0.12.3, HS256, 24h expiry) |
| Schema Mgmt | Flyway V1–V12 |
| Scheduling | Spring `@Scheduled` + `@EnableScheduling` |
| HTTP Client | Spring `RestClient` (exchange rate sync) |
| Docs | springdoc-openapi 3.0.0 (Swagger UI) |

---

## Directory Tree

```
expensetrackerspring/
├── src/
│   ├── main/
│   │   ├── kotlin/com/devvikram/expensetracker/expensetracker/
│   │   │   ├── config/                    # App initialization & env config
│   │   │   ├── controllers/               # REST API layer (12 controllers)
│   │   │   ├── dto/
│   │   │   │   ├── request/               # Inbound request payloads
│   │   │   │   └── response/              # Outbound response payloads
│   │   │   ├── entity/                    # JPA entities (DB models)
│   │   │   ├── enums/                     # Shared enumerations
│   │   │   ├── exceptions/                # Custom exceptions + global handler
│   │   │   ├── repository/                # Data access interfaces
│   │   │   ├── security/
│   │   │   │   └── anotation/             # Role-based security annotations
│   │   │   ├── service/                   # Business logic (15 services)
│   │   │   ├── specifications/            # JPA Specifications for filtering
│   │   │   ├── flow/                      # Documentation files (this folder)
│   │   │   │   └── ExpenseTestings/       # Postman collections & test cases
│   │   │   └── ExpensetrackerApplication.kt
│   │   └── resources/
│   │       ├── application.properties     # Base config
│   │       ├── application-dev.properties # Dev profile
│   │       ├── application-prod.properties# Prod profile
│   │       └── db/migration/              # Flyway migration scripts V1–V12
│   └── test/
│       ├── kotlin/.../
│       │   ├── ExpenseServiceTest.kt       # 9 unit tests
│       │   ├── BudgetServiceTest.kt        # 6 unit tests
│       │   ├── NotificationServiceTest.kt  # 7 unit tests
│       │   ├── AuthServiceTest.kt          # 6 unit tests
│       │   ├── SecurityIntegrationTest.kt  # 10 integration tests
│       │   └── ExpenseOwnershipIntegrationTest.kt  # 6 integration tests
│       └── resources/
│           └── application-test.properties # H2 in-memory DB config
├── build.gradle.kts                       # Gradle build + dependencies
├── settings.gradle.kts
├── Dockerfile                             # Multi-stage Docker build
└── .env                                   # Local env vars (not committed)
```

---

## Flyway Migrations (`resources/db/migration/`)

| Migration | What It Creates |
|---|---|
| `V1__create_roles_and_users.sql` | `roles`, `users`, `user_roles` join table |
| `V2__create_categories.sql` | `categories` with `UNIQUE(name, user_id)` constraint |
| `V3__create_expenses.sql` | `expenses` + indexes on `user_id`, `category_id`, `created_at` |
| `V4__create_budgets.sql` | `budgets` + indexes on `user_id`, `is_active` |
| `V5__create_recurring_expenses.sql` | `recurring_expenses` + indexes on `user_id`, `next_due_date`, `is_active` |
| `V6__create_audit_logs.sql` | `audit_logs` + 4 indexes |
| `V7__ensure_audit_logs.sql` | Idempotent `CREATE TABLE IF NOT EXISTS` guard for `audit_logs` |
| `V8__create_notifications.sql` | `notifications` + 3 indexes (including partial index on unread rows) |
| `V9__create_tags.sql` | `tags` (unique per user) + `expense_tags` join table + 3 indexes |
| `V10__add_currency_to_expenses.sql` | Adds `currency VARCHAR(3)` + `amount_in_base DOUBLE` to `expenses`; backfills existing rows |
| `V11__add_base_currency_to_users.sql` | Adds `base_currency VARCHAR(3) DEFAULT 'INR'` to `users` |
| `V12__create_exchange_rates.sql` | `exchange_rates` table (USD-pivot pair, unique constraint) + 3 indexes |

---

## Layer-by-Layer Breakdown

### `config/`

| File | Responsibility |
|---|---|
| `DataInitializer.kt` | `@Order(1)` init roles, `@Order(2)` seed super-admin user on startup. Order matters — roles must exist before the admin is created. |
| `EnvConfig.kt` | `EnvironmentPostProcessor` — loads `.env` via `dotenv-kotlin`; validates required vars (`JWT_SECRET`, `DATABASE_URL`, `PGUSER`, `PGPASSWORD`); skipped entirely when `test` profile is active. |
| `OpenApiConfig.kt` | Configures springdoc-openapi: `@OpenAPIDefinition` with app info + `@SecurityScheme` for JWT Bearer. Swagger UI at `/swagger-ui.html`. |

---

### `controllers/`

REST controllers — thin layer that delegates all logic to services. All routes use the `/api/v1/` prefix.

| File | Base Path | Auth | Key Endpoints |
|---|---|---|---|
| `AuthController.kt` | `/api/v1/auth` | Public / Authenticated | `POST /register`, `POST /login`, `GET /me`, `POST /assign-roles` (SUPER_ADMIN), `GET /roles`, `PUT /me/currency` |
| `ExpenseController.kt` | `/api/v1/expenses` | Authenticated | Full CRUD + `GET /search`, `GET /filter/category`, `GET /filter/amount`, `GET /filter/date-range`, `POST /filter` (advanced with tagIds) |
| `AdminCategoryController.kt` | `/api/v1/admin/categories` | ADMIN | `POST`, `PUT /{id}`, `DELETE /{id}` — global categories |
| `UserCategoryController.kt` | `/api/v1/categories` | SUPER_ADMIN | `POST`, `GET`, `PUT /{id}`, `DELETE /{id}` — per-user categories |
| `ReportController.kt` | `/api/v1/reports` | SUPER_ADMIN | `GET /summary`, `/category-wise`, `/date-wise`, `POST /custom`, `GET /trends`, `/budget-performance`, `/insights`, `/top-expenses`, `/export` |
| `BudgetController.kt` | `/api/v1/budgets` | Authenticated | `POST`, `GET`, `GET /{id}/status`, `PUT /{id}`, `DELETE /{id}` |
| `RecurringExpenseController.kt` | `/api/v1/recurring-expenses` | Authenticated | `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |
| `NotificationController.kt` | `/api/v1/notifications` | Authenticated | `GET`, `GET /unread`, `GET /unread/count`, `PUT /{id}/read`, `PUT /read-all`, `DELETE /{id}` |
| `DashboardController.kt` | `/api/v1/dashboard` | Authenticated | `GET` — single aggregated response |
| `AuditLogController.kt` | `/api/v1/audit-logs` | ADMIN / Authenticated | `GET`, `GET /me`, `GET /user/{userId}`, `GET /entity/{type}/{id}`, `GET /action/{action}` |
| `TagController.kt` | `/api/v1/tags` | Authenticated | `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |
| `ExchangeRateController.kt` | `/api/v1/exchange-rates` | Authenticated / ADMIN | `GET` (list rates), `GET /convert`, `POST /sync` (ADMIN) |

---

### `dto/`

#### Request DTOs (`dto/request/`)

| File | Key Fields |
|---|---|
| `LoginRequest.kt` | `email`, `password` |
| `RegisterRequest.kt` | `name`, `email`, `password`, `roles?` |
| `ExpenseRequest.kt` | `title`, `amount`, `categoryId`, `userId`, `note?`, `tagIds: List<Long>`, `currency: String (default INR)` |
| `ExpenseFilterRequest.kt` | `searchTitle?`, `categoryId?`, `minAmount?`, `maxAmount?`, `startDate?`, `endDate?`, `year?`, `month?`, `tagIds?` |
| `CustomReportRequest.kt` | `startDate`, `endDate`, `categoryIds`, `minAmount?`, `maxAmount?` |
| `AssignRoleRequest.kt` | `userId`, `roles: Set<RoleType>` |
| `BudgetRequest.kt` | `CreateBudgetRequest`: `categoryId?`, `amount`, `period`, `startDate`, `endDate?`, `alertThreshold=0.80` — `UpdateBudgetRequest`: all fields nullable |
| `RecurringExpenseRequest.kt` | `CreateRecurringExpenseRequest`: `title`, `amount`, `categoryId`, `frequency`, `startDate`, `endDate?`, `note?` — `UpdateRecurringExpenseRequest`: all fields nullable + `nextDueDate?`, `isActive?` |
| `TagRequest.kt` | `name: @NotBlank`, `color: @Pattern(hex) = "#6366f1"` |
| `UpdateBaseCurrencyRequest.kt` | `baseCurrency: @Size(3) @Pattern([A-Z]{3})` |

#### Response DTOs (`dto/response/`)

| File | Key Fields |
|---|---|
| `ApiResponse<T>` | Generic wrapper — `status: Boolean`, `message: String`, `data: T?` |
| `ErrorResponse.kt` | `status=false`, `code: String` (ErrorCode name), `message`, `timestamp`, `path?`, `details?` |
| `AuthResponse.kt` | `token`, `user: UserResponse` |
| `UserResponse.kt` | `id`, `name`, `email`, `roles`, `isActive`, `createdAt`, `baseCurrency` |
| `ExpenseResponse.kt` | `id`, `title`, `amount`, `currency`, `amountInBase`, `categoryId`, `categoryName`, `note`, `createdAt`, `tags: List<TagResponse>` |
| `TagResponse.kt` | `id`, `name`, `color`, `userId`, `createdAt` |
| `SummaryReportResponse.kt` | `totalExpenses`, `count`, `averageAmount` |
| `CategoryWiseReportResponse.kt` | `categoryName`, `total`, `count`, `percentage` |
| `DateWiseReportResponse.kt` | `date`, `total`, `count` |
| `InsightReportResponse.kt` | `totalThisMonth`, `totalLastMonth`, `monthOverMonthChange`, `spendingVelocity`, `averageDailySpendLast30Days`, `highestSpendDay`, `highestSpendDayAmount`, `biggestExpense`, `mostUsedCategory`, `mostUsedCategoryCount` |
| `MonthlyTrendResponse.kt` | `year`, `month`, `monthLabel`, `totalAmount`, `expenseCount`, `categoryBreakdown: List<CategoryTrendItem>` |
| `BudgetPerformanceResponse.kt` | `budgetId`, `categoryId?`, `categoryName?`, `period`, `budgetLimit`, `spent`, `remaining`, `percentUsed`, `isOverBudget`, `isNearLimit`, `status` |
| `BudgetResponse.kt` | `id`, `categoryId?`, `categoryName?`, `amount`, `period`, `startDate`, `endDate?`, `alertThreshold`, `isActive`, `spent`, `remaining`, `percentUsed`, `createdAt` |
| `BudgetStatusResponse.kt` | `id`, `categoryId?`, `categoryName?`, `limit`, `spent`, `remaining`, `percentUsed`, `isOverBudget`, `isNearLimit` |
| `RecurringExpenseResponse.kt` | `id`, `title`, `amount`, `categoryId`, `categoryName`, `frequency`, `nextDueDate`, `endDate?`, `isActive`, `note?`, `createdAt` |
| `NotificationResponse.kt` | `id`, `userId`, `title`, `message`, `type`, `isRead`, `entityType?`, `entityId?`, `createdAt` |
| `AuditLogResponse.kt` | `id`, `userId`, `action`, `entityType`, `entityId?`, `oldValue?`, `newValue?`, `ipAddress?`, `createdAt` |
| `DashboardResponse.kt` | `thisMonthSummary`, `budgetOverview`, `recentExpenses`, `upcomingRecurring`, `categoryBreakdown`, `unreadNotificationsCount` |
| `ExchangeRateResponse.kt` | `id`, `baseCurrency`, `targetCurrency`, `rate`, `fetchedAt` |
| `CurrencyConversionResponse.kt` | `fromCurrency`, `toCurrency`, `originalAmount`, `convertedAmount` |

---

### `entity/`

JPA-annotated classes mapped to PostgreSQL tables.

| File | Table | Key Fields / Relationships |
|---|---|---|
| `User.kt` | `users` | `email (unique)`, `name`, `password (BCrypt)`, `isActive`, `baseCurrency (default INR)`, `createdAt`, `updatedAt` — M:M → `Role` |
| `Role.kt` | `roles` | `name: RoleType (enum)` — M:M → `User` |
| `Category.kt` | `categories` | `name`, `description`, `isGlobal: Boolean`, `userId (nullable)` — global categories have `userId = null` |
| `Expense.kt` | `expenses` | `title`, `amount`, `currency`, `amountInBase`, `note`, `createdAt`, `userId` — M:1 → `Category`, M:M → `Tag` (EAGER, `expense_tags` join table) |
| `Tag.kt` | `tags` | `name`, `color`, `userId`, `createdAt` — UNIQUE(`name`, `user_id`) |
| `Budget.kt` | `budgets` | `userId`, `amount`, `period: BudgetPeriod`, `startDate`, `endDate?`, `alertThreshold=0.80`, `isActive`, `deletedAt?` — M:1 → `Category` |
| `RecurringExpense.kt` | `recurring_expenses` | `userId`, `title`, `amount`, `frequency: RecurringFrequency`, `nextDueDate`, `endDate?`, `isActive`, `note?`, `deletedAt?` — M:1 → `Category` |
| `Notification.kt` | `notifications` | `userId`, `title`, `message`, `type: NotificationType`, `isRead=false`, `entityType?`, `entityId?`, `createdAt` |
| `AuditLog.kt` | `audit_logs` | `userId`, `action: AuditAction`, `entityType`, `entityId?`, `oldValue? (JSON)`, `newValue? (JSON)`, `ipAddress?`, `createdAt` |
| `ExchangeRate.kt` | `exchange_rates` | `baseCurrency`, `targetCurrency`, `rate`, `fetchedAt` — UNIQUE(`base_currency`, `target_currency`) |

---

### `enums/`

| File | Values |
|---|---|
| `RoleType.kt` | `USER`, `ADMIN`, `SUPER_ADMIN`, `MODERATOR`, `ACCOUNTANT`, `VIEWER` |
| `BudgetPeriod.kt` | `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` |
| `RecurringFrequency.kt` | `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` |
| `NotificationType.kt` | `BUDGET_ALERT`, `BUDGET_EXCEEDED`, `RECURRING_EXPENSE_DUE`, `SYSTEM` |
| `AuditAction.kt` | Expense: `EXPENSE_CREATED/UPDATED/DELETED/AUTO_CREATED` — Budget: `BUDGET_CREATED/UPDATED/DELETED` — Recurring: `RECURRING_EXPENSE_CREATED/UPDATED/DELETED/PROCESSED` — Category: `CATEGORY_CREATED/UPDATED/DELETED` — Auth: `USER_REGISTERED/LOGIN`, `ROLE_ASSIGNED` — Notification: `NOTIFICATION_READ/DELETED` — Reports: `REPORT_EXPORTED` — Tags: `TAG_CREATED/UPDATED/DELETED` — Currency: `EXCHANGE_RATE_SYNCED`, `USER_CURRENCY_UPDATED` |
| `ErrorCode.kt` | `RESOURCE_NOT_FOUND`, `USER_NOT_FOUND`, `TAG_NOT_FOUND`, `VALIDATION_FAILED`, `BAD_REQUEST`, `BUDGET_EXCEEDED`, `CONFLICT`, `EMAIL_ALREADY_EXISTS`, `TAG_ALREADY_EXISTS`, `INVALID_CREDENTIALS`, `ACCOUNT_DEACTIVATED`, `TOKEN_EXPIRED`, `TOKEN_INVALID`, `TOKEN_SIGNATURE_INVALID`, `AUTHENTICATION_FAILED`, `UNAUTHORIZED`, `INTERNAL_ERROR` |

---

### `exceptions/`

| File | Type / HTTP Status |
|---|---|
| `ResourceNotFoundException.kt` | Custom — mapped to **404** |
| `BadRequestException.kt` | Custom — mapped to **400** |
| `ConflictException.kt` | Custom — mapped to **409** |
| `ErrorResponse.kt` | Error envelope DTO: `status=false`, `code` (ErrorCode name), `message`, `timestamp`, `path`, `details` |
| `GlobalExceptionHandler.kt` | `@RestControllerAdvice` — all handlers inject `HttpServletRequest` for `path`; returns `ErrorResponse` with machine-readable `ErrorCode` values. Handles: `ResourceNotFoundException` (404), `MethodArgumentNotValidException` (400 + field map in `details`), `BadRequestException` (400), `ConflictException` (409), `UsernameNotFoundException` (404), `AuthenticationException` (401), `ExpiredJwtException` (401), `MalformedJwtException` (400), `SignatureException` (401), `IllegalArgumentException` (401), `Exception` (500). |

---

### `repository/`

All interfaces extend `JpaRepository`. `ExpenseRepository` also extends `JpaSpecificationExecutor`.

| Interface | Notable Custom Methods |
|---|---|
| `UserRepository` | `findByEmail()`, `existsByEmail()`, `findByEmailWithRoles()`, `findByRoleType()` |
| `ExpenseRepository` | `findByUserId()`, `sumAmountByUserIdAndCategoryIdAndDateBetween()`, `sumAmountByUserIdAndDateBetween()` — filtering via Specifications |
| `CategoryRepository` | Global categories, user-specific categories, combined lists |
| `RoleRepository` | `findByName()`, `existsByName()` |
| `ReportRepository` | Native JPQL for `summaryReport()`, `categoryWiseReport()`, `getDateWiseReport()`, `monthlyTrend()`, `monthlyTrendByCategory()` |
| `BudgetRepository` | `findAllActiveBudgets(userId, today)`, `findByIdAndUserIdAndDeletedAtIsNull()`, `findByUserIdAndIsActiveTrueAndDeletedAtIsNull()` |
| `RecurringExpenseRepository` | `findAllDueToday(today)`, `findByUserIdAndIsActiveTrueAndDeletedAtIsNull()`, `findByIdAndUserIdAndDeletedAtIsNull()` |
| `NotificationRepository` | `findByUserIdOrderByCreatedAtDesc()`, `findByUserIdAndIsReadFalse()`, `countByUserIdAndIsReadFalse()`, `findByUserIdAndIsReadFalseOrderByCreatedAtDesc()` |
| `AuditLogRepository` | `findByUserId()`, `findByEntityTypeAndEntityId()`, `findByAction()` — all paginated |
| `TagRepository` | `findByUserId()`, `findByIdAndUserId()`, `existsByNameAndUserId()` |
| `ExchangeRateRepository` | `findByBaseCurrency()`, `findByBaseCurrencyAndTargetCurrency()` |

---

### `security/`

| File | Responsibility |
|---|---|
| `SecurityConfig.kt` | Permits `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/roles`, Swagger paths, Actuator health; all others require authentication; stateless session; plugs in `JwtAuthenticationFilter`. |
| `JwtUtil.kt` | Generates and validates JWT tokens; extracts claims (`userId`, `email`, `roles`); HS256; 24-hour expiry. |
| `JwtAuthenticationFilter.kt` | `OncePerRequestFilter` — extracts `Bearer` token, validates, sets `SecurityContextHolder`. |
| `JwtAuthenticationEntryPoint.kt` | Returns JSON `401` when authentication fails. |
| `SecurityUtil.kt` | Helper for getting the current authenticated user from the security context. |
| `CustomUserDetailsService.kt` | Implements `UserDetailsService`; bridges `User` entity with Spring Security. |

#### Security Annotations (`security/anotation/`)

| Annotation | Required Condition |
|---|---|
| `@IsAuthenticated` | Any authenticated user |
| `@IsAdmin` | `ADMIN` role |
| `@IsSuperAdmin` | `SUPER_ADMIN` role |
| `@IsModerator` | `MODERATOR` role |
| `@IsAccountant` | `ACCOUNTANT` role |
| `@IsOwner` | Resource ownership verified at runtime |

---

### `service/`

| File | Key Responsibilities |
|---|---|
| `AuthService.kt` | `register()` (hashes password, assigns default role, audit logs), `login()` (validates credentials, issues JWT), `assignRoles()` (SUPER_ADMIN only), `updateBaseCurrency()` (changes user's base currency) |
| `ExpenseService.kt` | CRUD; calls `checkBudgetOnExpense()` before every create; fetches user's `baseCurrency` and calls `ExchangeRateService.convert()` to compute `amountInBase`; resolves tags by ownership; uses `ExpenseSpecifications` for filter queries; pagination via `Pageable` |
| `CategoryService.kt` | Global (admin) and per-user categories; duplicate name prevention |
| `RoleService.kt` | Role lookup, creation, default role initialization |
| `ReportService.kt` | DB-backed reports (summary, category-wise, date-wise, custom, monthly trend, budget performance); in-memory `getInsights()` (month-over-month, velocity, avg daily, highest day, biggest expense, most-used category); `getTopExpenses()` (sorted by amount desc, capped 1–100) |
| `BudgetService.kt` | CRUD; `getBudgetStatus()` (live spent/remaining); `checkBudgetOnExpense()` (evaluates all active budgets, returns `BudgetCheckResult`); `resetPeriodicBudgets()` scheduler at `00:00` daily |
| `BudgetCheckResult.kt` | Data holder: `shouldBlock: Boolean`, `warnings: List<String>` |
| `RecurringExpenseService.kt` | CRUD; `processRecurringExpenses()` scheduler at `00:05` daily — creates Expense via ExpenseService, advances `nextDueDate`, auto-deactivates past `endDate`; each entry is independently fault-tolerant |
| `NotificationService.kt` | `send()` (fire-and-forget, swallows exceptions), `markAsRead()`, `markAllAsRead()`, `delete()`, `getUnread()`, `countUnread()` |
| `DashboardService.kt` | Aggregates: `thisMonthSummary`, `budgetOverview`, `recentExpenses` (last 5), `upcomingRecurring` (7-day window), `categoryBreakdown`, `unreadNotificationsCount` |
| `AuditLogService.kt` | `log()` — `@Transactional(REQUIRES_NEW)`, never throws; stores JSON snapshots of `oldValue`/`newValue`; captures IP from `X-Forwarded-For` or `remoteAddr` |
| `ExportService.kt` | `exportCsv()` (Apache Commons CSV), `exportPdf()` (OpenPDF); both `@Transactional(readOnly=true)`; audit logged as `REPORT_EXPORTED` |
| `TagService.kt` | `createTag()`, `getTagsByUser()`, `getTagById()`, `updateTag()`, `deleteTag()`; throws `ConflictException` on duplicate name; fully audit logged |
| `ExchangeRateService.kt` | `syncRates()` fetches all currencies from `open.er-api.com` with USD pivot, upserts DB; `@Scheduled(cron="0 0 1 * * *")` daily; `convert(amount, from, to)` uses cross-rate formula `amount × (rateUSD→to / rateUSD→from)` |
| `CustomUserDetailsService.kt` | Spring Security integration |

---

### `specifications/`

| File | Purpose |
|---|---|
| `ExpenseSpecifications.kt` | `Specification<Expense>` predicates: `filterByUserId`, `filterByTitle` (LIKE), `filterByCategory`, `filterByMinAmount`, `filterByMaxAmount`, `filterByStartDate`, `filterByEndDate`, `filterByMonthYear`, `filterByTagIds` (INNER JOIN on `expense_tags`). `buildFilterSpecification()` combines all with `.and()`. |

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
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
# HikariCP: max=10, minIdle=5, connectionTimeout=20s
# Actuator: /actuator/health + /actuator/info exposed
```

### `application-test.properties`
- H2 in-memory DB with `MODE=PostgreSQL`
- `spring.flyway.enabled=false`
- `ddl-auto=create-drop`
- `EnvConfig` EnvironmentPostProcessor skipped via `test` profile guard

### `application-dev.properties`
- `ddl-auto=update` — Hibernate creates/alters tables automatically
- SQL logging at `DEBUG` level

### `application-prod.properties`
- `ddl-auto=validate` — schema must match entities exactly
- SQL logging at `WARN` level

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

All secrets are passed as environment variables at runtime — nothing baked into the image.

---

## Required Environment Variables

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | Signing key for JWT tokens (min 32 chars for HS256) |
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
SecurityConfig (route guards)    ← enforces @PreAuthorize / role annotations
     │
     ▼
Controller                       ← parses request DTO, calls service
     │
     ▼
Service                          ← business logic, validation, orchestration
     │         │                 │
     │         │                 └── AuditLogService.log()  ← REQUIRES_NEW tx
     │         │                       stores JSON snapshots of entity state
     │         │
     │         ├── BudgetService.checkBudgetOnExpense()
     │         │     on every expense create (block or warn + notify)
     │         │
     │         └── ExchangeRateService.convert()
     │               on expense create/update: amount → amountInBase
     │               using user's baseCurrency + USD-pivot rates from DB
     │
     ▼
Repository / Specifications      ← database queries (JPA + dynamic specs)
     │
     ▼
PostgreSQL
     │
     ▼
Response DTO → ApiResponse<T>    ← wrapped in generic envelope, returned as JSON
                ErrorResponse    ← on error: { code, message, path, timestamp, details }

─────────────────────────────────────────────────────────────────
Scheduled Jobs (Spring @Scheduled — enabled by @EnableScheduling)
─────────────────────────────────────────────────────────────────
00:00 daily  →  BudgetService.resetPeriodicBudgets()
                  Advances startDate for budgets whose period window has elapsed

00:05 daily  →  RecurringExpenseService.processRecurringExpenses()
                  Finds all due entries → creates Expense via ExpenseService
                  (budget check + currency conversion + audit log included)
                  → advances nextDueDate → auto-deactivates entries past endDate
                  Each entry processed independently (fault-tolerant)

01:00 daily  →  ExchangeRateService.scheduledSync()
                  Fetches ~160 currency rates from open.er-api.com (free, USD pivot)
                  Upserts all rates in exchange_rates table
                  Failure is logged and swallowed — never crashes the app
```

---

## Test Coverage (45 tests)

| Test Class | Count | Scope |
|---|---|---|
| `ExpenseServiceTest` | 9 | Unit — create (success + budget block), get (found/not found/wrong user), update (success/not found), delete (success/wrong user) |
| `BudgetServiceTest` | 6 | Unit — create overall budget, list with spent, soft-delete, delete not found throws, no-block when no budgets, block when over limit |
| `NotificationServiceTest` | 7 | Unit — send (success + swallows errors), markAsRead (success + wrong user throws), delete (success + not found throws), unread count |
| `AuthServiceTest` | 6 | Unit — register (success + duplicate email throws), login (success + wrong password + not found + deactivated) |
| `SecurityIntegrationTest` | 10 | Integration — Swagger UI + OpenAPI accessible without token; login/register reachable; 6 protected endpoints return 401 |
| `ExpenseOwnershipIntegrationTest` | 6 | Integration — User A can read/update/delete own expense; User B returns null/false for each — no cross-user leakage |

**Test infrastructure:**
- H2 in-memory DB (`MODE=PostgreSQL`, `create-drop`)
- Flyway disabled for tests
- `EnvConfig` post-processor skipped via `activeProfiles.contains("test")`
- `DataInitializer` `@Order(1)/(2)` fixes `CommandLineRunner` race condition

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| Stateless JWT | No server-side session storage; horizontally scalable |
| `ApiResponse<T>` + `ErrorResponse` | Success uses generic wrapper; errors use structured `ErrorResponse` with machine-readable `ErrorCode` enum for programmatic frontend handling |
| JPA Specifications | Builds complex filters dynamically at runtime; avoids N separate query methods |
| Global vs. User categories | System-wide defaults managed by admins; users extend with private categories |
| Custom security annotations | Reduces boilerplate; centralises role logic; one annotation per method (Spring Security 7 rejects stacked `@PreAuthorize`) |
| `ddl-auto=validate` in prod | Prevents accidental schema changes; all changes via Flyway |
| Multi-stage Docker build | Final image has JRE only; build tools not shipped to production |
| Budget check before expense save | `BudgetService.checkBudgetOnExpense()` evaluates category-scoped + overall budgets; returns `BudgetCheckResult(shouldBlock, warnings)` — caller decides action |
| Soft delete on budgets & recurring expenses | `deletedAt` timestamp instead of hard delete — preserves history; all queries filter `deletedAt IS NULL` |
| Recurring scheduler at 00:05 | Fires 5 minutes after budget reset (00:00) — ensures period windows are current before auto-expenses are created |
| Per-entry failure isolation | Each recurring expense is processed in its own try/catch — one bad entry cannot block others |
| `nextDueDate` as scheduler trigger | Simple `nextDueDate <= today` query; advanced after each fire — no per-entry cron needed |
| `AuditLogService` in `REQUIRES_NEW` | Runs in its own transaction — a rolled-back or read-only caller transaction never blocks audit writes |
| USD-pivot exchange rate storage | One fetch (`/latest/USD`) covers all ~160 currencies; cross-rate `A→B = rateUSD→B / rateUSD→A` avoids O(N²) pair storage |
| `amountInBase` stored at creation | Rates change daily — snapshot at creation time is historically accurate; avoids re-conversion on every read |
| Tags as `ManyToMany(EAGER)` | Tags are small, per-user sets — EAGER loading avoids N+1 on expense lists |
| `@EnableScheduling` on main class | Required for `@Scheduled` to fire in Spring Boot 4.0; not auto-configured |

---

## flow/ExpenseTestings — Documentation & Testing Artifacts

| File | Purpose |
|---|---|
| `expense_tracker_complete_collection.json` | **Master Postman collection** — all 47+ endpoints across 12 folders; JWT auto-saved from Login; collection variables for all entity IDs |
| `expense_test_cases.md` | Test cases for `ExpenseController` / `ExpenseService` |
| `budget_postman_collection.json` | Postman collection for `/api/v1/budgets` |
| `budget_test_cases.md` | Test cases for budget CRUD, scheduler, and budget check on expense |
| `recurring_expense_postman_collection.json` | Postman collection for `/api/v1/recurring-expenses` |
| `recurring_expense_test_cases.md` | Test cases for recurring expense CRUD and daily scheduler |
| `audit_log_postman_collection.json` | Postman collection for `/api/v1/audit-logs` |
| `audit_log_test_cases.md` | Test cases for `AuditLogController` / `AuditLogService` |
| `notification_postman_collection.json` | Postman collection for `/api/v1/notifications` |
| `notification_test_cases.md` | Test cases for notification send/read/delete |
| `dashboard_postman_collection.json` | Postman collection for `/api/v1/dashboard` |
| `dashboard_test_cases.md` | Test cases for the dashboard aggregation endpoint |

**Postman setup (master collection):**
1. Import `expense_tracker_complete_collection.json`.
2. Set collection variable `base_url = http://localhost:8081`.
3. Run **Login** under Auth — test script auto-saves JWT to `{{token}}`.
4. Run **Sync Exchange Rates** (ADMIN) once to load rates from open.er-api.com.
5. All other requests inherit Bearer auth from the collection root.
