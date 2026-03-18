# Expense Tracker Spring

## Improvement & Missing Features Guide

> What's Built | What's Missing | How to Fix It | Priority Order

**Kotlin 2.2 • Spring Boot 4.0 • PostgreSQL • JWT • Docker**

---

## 1. Honest Assessment — Where the Project Stands

> ✅ **The One-Line Summary**
>
> Your project is a fully-featured expense **TRACKER**. Budgets, recurring expenses, in-app notifications, and a dashboard are all live. The next frontier is tests, Swagger, and trend analytics.
>
> Infrastructure: ✅ Solid | Core Business Features: ✅ Complete | Tests: ✅ 45 passing | Swagger: ✅ Live | Export: ✅ CSV + PDF | API Versioning: ✅ /api/v1/ | Tags: ✅ Many-to-Many | Insights: ✅ Built | Error Codes: ✅ Built | Multi-Currency: ✅ Built | Next: Redis Caching + Refresh Tokens

---

## 1.1 Feature Completeness Overview

| Feature | Status | Priority | Notes |
|---|---|---|---|
| **Authentication / JWT** | ✅ Built | Done | Solid implementation with HMAC-SHA256, 24hr expiry |
| **Role System** | ✅ Built | Done | 6 roles, custom annotations, @PreAuthorize |
| **Expense CRUD** | ✅ Built | Done | Create, read, update, delete with ownership checks |
| **Dynamic Filtering** | ✅ Built | Done | JPA Specifications with composable filters |
| **Basic Reports** | ✅ Built | Done | Summary, category-wise, date-wise, custom |
| **Trend Reports** | ✅ Built | ~~Medium~~ | ✅ Built: monthly trend (with category breakdown) + budget performance |
| **Docker / DevOps** | ✅ Built | Done | Multi-stage build, non-root user, env vars |
| **Exception Handling** | ✅ Built | Done | GlobalExceptionHandler with machine-readable ErrorCode + ErrorResponse (path, timestamp, details) |
| **Budgets** | ✅ Done | ~~Critical~~ | ✅ Built: budget check on expense creation, alerts, scheduler |
| **Recurring Expenses** | ✅ Done | ~~Critical~~ | ✅ Built: @Scheduled auto-creation, nextDueDate advancement |
| **Notifications / Alerts** | ✅ Built | ~~Critical~~ | ✅ Built: NotificationService, 4 types, auto-triggered by budget & recurring scheduler |
| **Dashboard API** | ✅ Built | ~~High~~ | ✅ Built: GET /api/dashboard — monthly summary, budgets, recent, upcoming, category breakdown |
| **Soft Deletes** | ✅ Done | ~~High~~ | ✅ Built: deleted_at on expenses, budgets, recurring expenses |
| **Refresh Tokens** | ⬜ Deferred | High | Logout is cosmetic — tokens still work after logout |
| **Rate Limiting** | ✅ Done | ~~High~~ | ✅ Built: bucket4j, 5 attempts/IP/min on /api/auth/login |
| **Audit Logging** | ✅ Done | ~~High~~ | ✅ Built: audit_logs table, V6 migration, wired into all services, 5 controller endpoints |
| **Multi-currency** | ✅ Done | ~~Medium~~ | V10–V12 migrations, ExchangeRateService (open.er-api.com, USD pivot, daily @Scheduled sync), `PUT /api/v1/auth/me/currency`, `GET /api/v1/exchange-rates/convert` |
| **Attachments / Receipts** | ✅ Done | ~~Medium~~ | S3-backed; V14 migration; `POST/GET/DELETE /api/v1/expenses/{id}/receipts`; pre-signed URLs; 5 MB / jpg,png,pdf limit |
| **Tags / Labels** | ✅ Done | ~~Medium~~ | Many-to-many with expenses, per-user, filterable — V9 migration, TagController `/api/v1/tags` |
| **Export (CSV / PDF)** | ✅ Done | ~~Medium~~ | `GET /api/reports/export?format=csv\|pdf` — Apache Commons CSV + OpenPDF |
| **API Versioning** | ✅ Done | ~~Medium~~ | All routes now use `/api/v1/` prefix — Postman collection updated |
| **Flyway Migrations** | ✅ Done | ~~High~~ | ✅ Built: V1–V12 migration scripts, ddl-auto=update (base) / validate (profiles), baseline-on-migrate |
| **Tests** | ✅ Done | ~~High~~ | 45 tests: unit (service layer), integration (security + ownership) |
| **Swagger / OpenAPI** | ✅ Done | ~~Medium~~ | springdoc-openapi 3.0.0 — Swagger UI at `/swagger-ui.html`, JWT bearer auth wired |
| **Redis / Caching** | ⬜ Missing | Medium | No caching layer — reports hit DB every time |

---

## 2. Critical Missing Features (Build These First)

### 2.1 Budgets — ✅ COMPLETE

> ✅ **Built** — Budgets table, budget check on expense creation, alert threshold notifications, @Scheduled period reset, soft delete, all 5 endpoints.

**Completed endpoints:**

| Endpoint | Description |
|---|---|
| `POST /api/budgets` | Create a new budget for a category or overall |
| `GET /api/budgets` | List all active budgets with spent/remaining amounts |
| `GET /api/budgets/{id}/status` | Real-time: `{ limit, spent, remaining, percentUsed }` |
| `PUT /api/budgets/{id}` | Update budget limit or threshold |
| `DELETE /api/budgets/{id}` | Soft-delete a budget |

---

### 2.2 Recurring Expenses — ✅ COMPLETE

> ✅ **Built** — recurring_expenses table, CRUD endpoints, @Scheduled daily job at 00:05, auto-creates Expense via ExpenseService (triggers budget check), advances nextDueDate, auto-deactivates when endDate is passed.

**Completed endpoints:**

| Endpoint | Description |
|---|---|
| `POST /api/recurring-expenses` | Create a recurring expense |
| `GET /api/recurring-expenses` | List all active recurring expenses |
| `GET /api/recurring-expenses/{id}` | Get a single recurring expense |
| `PUT /api/recurring-expenses/{id}` | Update a recurring expense |
| `DELETE /api/recurring-expenses/{id}` | Soft delete a recurring expense |

---

### 2.3 Notifications & Alerts — ✅ COMPLETE

> ✅ **Built** — `notifications` table (V8 migration), `NotificationService`, `NotificationController`, `NotificationType` enum. Automatically triggered by `BudgetService` and `RecurringExpenseService`. Audited on read/delete.

**DB Table (V8__create_notifications.sql)**

```sql
TABLE: notifications
  id          BIGSERIAL PK
  user_id     BIGINT NOT NULL
  title       VARCHAR(150) NOT NULL
  message     TEXT NOT NULL
  type        VARCHAR(50)   -- NotificationType enum value
  is_read     BOOLEAN DEFAULT false
  entity_type VARCHAR(50)   -- e.g. "Budget", "RecurringExpense"
  entity_id   BIGINT        -- PK of the related entity
  created_at  TIMESTAMP DEFAULT NOW()

INDEXES:
  idx_notifications_user_id       ON notifications(user_id)
  idx_notifications_user_unread   ON notifications(user_id, is_read) WHERE is_read = false  -- partial
  idx_notifications_created_at    ON notifications(created_at DESC)
```

**NotificationType enum values**

| Type | Trigger | Severity |
|---|---|---|
| `BUDGET_ALERT` | Spending after a new expense reaches `alertThreshold` of a budget | Warning |
| `BUDGET_EXCEEDED` | Spending after a new expense meets or exceeds the full budget `amount` | Blocking |
| `RECURRING_EXPENSE_DUE` | Daily scheduler auto-processes a recurring expense at 00:05 | Informational |
| `SYSTEM` | Reserved for generic system-level messages | Varies |

**How notifications are triggered**

- `BudgetService.checkBudgetOnExpense()` — called from `ExpenseService.createExpense()` before saving. Fires `BUDGET_EXCEEDED` (blocks the expense) or `BUDGET_ALERT` (warning) for every applicable budget.
- `RecurringExpenseService.processSingleRecurringExpense()` — fires `RECURRING_EXPENSE_DUE` after advancing `nextDueDate`.
- All `notificationService.send()` calls are wrapped in try-catch — a notification failure never interrupts the primary operation.

**Completed endpoints**

| Endpoint | Auth | Description |
|---|---|---|
| `GET /api/notifications` | Authenticated | All notifications, paginated, newest first |
| `GET /api/notifications/unread` | Authenticated | All unread notifications (not paginated) |
| `GET /api/notifications/unread/count` | Authenticated | Integer count for badge display |
| `PUT /api/notifications/{id}/read` | Authenticated | Mark one as read — audit logged as `NOTIFICATION_READ` |
| `PUT /api/notifications/read-all` | Authenticated | Mark all unread as read, returns count updated |
| `DELETE /api/notifications/{id}` | Authenticated | Permanently delete — audit logged as `NOTIFICATION_DELETED` |

---

### 2.4 Dashboard Summary Endpoint — ✅ COMPLETE

> ✅ **Built** — `DashboardService` aggregates six data points into a single response. `DashboardController` exposes `GET /api/dashboard`.

**Endpoint: `GET /api/dashboard`**

```json
{
  "thisMonthSummary": {
    "month": "March 2026",
    "totalSpent": 12500.00,
    "expenseCount": 14
  },
  "budgetOverview": {
    "totalBudgetLimit": 30000.00,
    "totalSpent": 12500.00,
    "totalRemaining": 17500.00,
    "overallUtilizationPercent": 41.67,
    "budgets": [ ...BudgetStatusResponse per active budget ]
  },
  "recentExpenses": [ ...last 5 expenses by createdAt DESC ],
  "upcomingRecurring": [ ...active recurring where nextDueDate <= today + 7 days ],
  "categoryBreakdown": [
    { "categoryId": 1, "categoryName": "Food", "totalAmount": 6500.0, "percentage": 52.0 }
  ],
  "unreadNotificationsCount": 3
}
```

**Dashboard field definitions**

| Field | Source | Description |
|---|---|---|
| `thisMonthSummary.totalSpent` | ExpenseRepository | Sum of expenses from 1st of current month to now |
| `thisMonthSummary.expenseCount` | ExpenseRepository | Count of expenses in current month |
| `budgetOverview.budgets` | BudgetService.getBudgetStatus() | Real-time status per active budget |
| `recentExpenses` | ExpenseRepository | Last 5 expenses, sorted by createdAt DESC |
| `upcomingRecurring` | RecurringExpenseRepository | Active entries with nextDueDate ≤ today + 7, sorted ASC |
| `categoryBreakdown` | Computed in-memory | This-month expenses grouped by category, sorted by totalAmount DESC |
| `unreadNotificationsCount` | NotificationRepository | COUNT WHERE is_read = false |

---

## 2.5 Bug Fixes Applied

### Duplicate @PreAuthorize on Controllers — ✅ FIXED

> **Problem:** Spring Security 7 enforces that only one `@PreAuthorize` annotation may exist on a method (including annotations inherited from the class level). `ReportController` had `@IsAuthenticated` + `@IsSuperAdmin` stacked at class level, which each expand to a separate `@PreAuthorize`. This caused:
>
> `AnnotationConfigurationException: Please ensure there is one unique annotation of type [@PreAuthorize]`

**Fix:** Removed the redundant `@IsAuthenticated` from `ReportController`. `@IsSuperAdmin` already implies authentication.

> **Rule going forward:** Never stack two security annotations that both resolve to `@PreAuthorize` on the same class or method. Use the most restrictive annotation only.

---

### Category Module Fixes — ✅ FIXED

Six logic and security bugs were found and corrected in the category module:

#### 1. `UserCategoryController` used `@IsSuperAdmin` instead of `@IsAuthenticated` — ✅ FIXED

`/api/v1/categories` was only accessible to `SUPER_ADMIN`. Regular users could not create or view their own categories. Fixed to `@IsAuthenticated`.

#### 2. `CategoryService` missing audit logging — ✅ FIXED

`AuditLogService` was not injected into `CategoryService`. All create/update/delete operations now log `CATEGORY_CREATED`, `CATEGORY_UPDATED`, `CATEGORY_DELETED`.

#### 3. No in-use check before category deletion — ✅ FIXED

Deleting a category referenced by expenses, budgets, or recurring expenses would throw an unhandled `DataIntegrityViolationException`. Service now explicitly checks all three tables before deletion and throws a clean `BadRequestException("Cannot delete: category is referenced by...")`.

#### 4. Entity used directly as request body — ✅ FIXED

Both controllers accepted `@RequestBody category: Category` (the raw JPA entity), allowing clients to supply `isGlobal`, `userId`, and `createdAt`. Replaced with a `CategoryRequest` DTO containing only `name` and `description`.

**New DTO: `dto/request/CategoryRequest.kt`**
```kotlin
data class CategoryRequest(
    @field:NotBlank val name: String,
    val description: String? = null
)
```

#### 5. No `GET /api/v1/admin/categories` endpoint — ✅ FIXED

`AdminCategoryController` had no list endpoint. Added `GET /api/v1/admin/categories` returning all global categories.

#### 6. `updateGlobalCategory` / `deleteGlobalCategory` not passing `adminId` — ✅ FIXED

Audit log calls for update and delete were missing the `adminId`. Updated service signatures to accept `adminId` from the controller.

**Updated endpoints:**

| Endpoint | Auth | Description |
|---|---|---|
| `GET /api/v1/admin/categories` | ADMIN | List all global categories |
| `POST /api/v1/admin/categories` | ADMIN | Create a global category |
| `PUT /api/v1/admin/categories/{id}` | ADMIN | Update a global category |
| `DELETE /api/v1/admin/categories/{id}` | ADMIN | Delete (blocked if in use) |
| `GET /api/v1/categories` | Authenticated | List user's own + global categories |
| `POST /api/v1/categories` | Authenticated | Create a user-specific category |
| `PUT /api/v1/categories/{id}` | Authenticated | Update own category |
| `DELETE /api/v1/categories/{id}` | Authenticated | Delete own category (blocked if in use) |

**Repository methods added:**
- `ExpenseRepository.existsByCategoryId(categoryId)`
- `BudgetRepository.existsByCategoryIdAndDeletedAtIsNull(categoryId)`
- `RecurringExpenseRepository.existsByCategoryIdAndDeletedAtIsNull(categoryId)`

---

## 3. Security Improvements

### 3.1 Refresh Token Flow — ⬜ DEFERRED

> ❌ **Current Problem**
>
> Right now, logout is cosmetic. The JWT token remains valid for 24 hours after logout. Anyone who intercepts the token can use it until it expires — you have no way to revoke it.

**Fix: Short-Lived Access Token + Refresh Token** *(deferred for later)*

| Component | Detail |
|---|---|
| **Access Token** | Reduce expiry to 15 minutes (not 24 hours) |
| **Refresh Token** | Long-lived (7–30 days), stored in `refresh_tokens` DB table |
| **On Logout** | Delete refresh token from DB + add access token to Redis blacklist |
| **Token Refresh** | `POST /api/auth/refresh` — validates refresh token, issues new access token |
| **JwtAuthFilter Update** | Check Redis blacklist before accepting any token |

---

### 3.2 Rate Limiting — ✅ COMPLETE

> ✅ **Built** — bucket4j in-memory rate limiter on `POST /api/auth/login`. Max 5 attempts per IP per minute. Returns HTTP 429 with `Retry-After` header.

---

### 3.3 Audit Logging — ✅ COMPLETE

> ✅ **Built** — `audit_logs` table (V6 migration), `AuditLogService`, `AuditLogController`, `AuditAction` enum. Wired into `ExpenseService`, `BudgetService`, `RecurringExpenseService`, and `AuthService`.

**DB Table (V6__create_audit_logs.sql)**

```sql
TABLE: audit_logs
  id          BIGSERIAL PK
  user_id     BIGINT NOT NULL
  action      VARCHAR(50)   -- AuditAction enum value
  entity_type VARCHAR(50)   -- "Expense" / "Budget" / "RecurringExpense" / "User"
  entity_id   BIGINT NULLABLE
  old_value   TEXT NULLABLE (JSON snapshot before change)
  new_value   TEXT NULLABLE (JSON snapshot after change)
  ip_address  VARCHAR(45) NULLABLE (null for scheduler-triggered actions)
  created_at  TIMESTAMP
```

**AuditAction enum values**

| Category | Actions |
|---|---|
| Expense | `EXPENSE_CREATED`, `EXPENSE_UPDATED`, `EXPENSE_DELETED`, `EXPENSE_AUTO_CREATED` |
| Budget | `BUDGET_CREATED`, `BUDGET_UPDATED`, `BUDGET_DELETED` |
| Recurring | `RECURRING_EXPENSE_CREATED`, `RECURRING_EXPENSE_UPDATED`, `RECURRING_EXPENSE_DELETED`, `RECURRING_EXPENSE_PROCESSED` |
| Category | `CATEGORY_CREATED`, `CATEGORY_UPDATED`, `CATEGORY_DELETED` |
| Auth | `USER_REGISTERED`, `USER_LOGIN`, `ROLE_ASSIGNED` |
| Notification | `NOTIFICATION_READ`, `NOTIFICATION_DELETED` |
| Reports | `REPORT_EXPORTED` |
| Tags | `TAG_CREATED`, `TAG_UPDATED`, `TAG_DELETED` |
| Currency | `EXCHANGE_RATE_SYNCED`, `USER_CURRENCY_UPDATED` |

**Completed endpoints**

| Endpoint | Auth | Description |
|---|---|---|
| `GET /api/audit-logs` | ADMIN | All logs, paginated |
| `GET /api/audit-logs/me` | Authenticated | Current user's own audit trail |
| `GET /api/audit-logs/user/{userId}` | SUPER_ADMIN | Any user's audit trail |
| `GET /api/audit-logs/entity/{entityType}/{entityId}` | ADMIN | Full change history for one entity |
| `GET /api/audit-logs/action/{action}` | ADMIN | Logs filtered by action type |

**Design notes**
- Audit failure never crashes the main operation — `AuditLogService.log()` catches and logs all exceptions.
- `AuditLogService.log()` is annotated `@Transactional(propagation = REQUIRES_NEW)` — it always runs in its own separate transaction, so a read-only or rolled-back caller transaction never blocks the audit write.
- IP address uses `X-Forwarded-For` header (proxy-aware); falls back to `remoteAddr`; stored as `null` for scheduler context.
- `old_value` / `new_value` are JSON strings serialized via `tools.jackson.databind.ObjectMapper` (Jackson 3.x — Spring Boot 4.0 package, NOT `com.fasterxml.jackson`).

---

## 4. Data Integrity Fixes

### 4.1 Schema Management — ✅ COMPLETE

> ✅ **Built** — Flyway enabled with `baseline-on-migrate=true`. Base profile uses `ddl-auto=update`; dev and prod profiles use `ddl-auto=validate`. Migration files live in `src/main/resources/db/migration/`:
> - `V1__create_roles_and_users.sql` — `roles`, `users`, `user_roles` join table
> - `V2__create_categories.sql` — `categories` with `UNIQUE(name, user_id)` constraint
> - `V3__create_expenses.sql` — `expenses` + indexes on `user_id`, `category_id`, `created_at`
> - `V4__create_budgets.sql` — `budgets` + indexes on `user_id`, `is_active`
> - `V5__create_recurring_expenses.sql` — `recurring_expenses` + indexes on `user_id`, `next_due_date`, `is_active`
> - `V6__create_audit_logs.sql` — `audit_logs` + 4 indexes
> - `V7__ensure_audit_logs.sql` — idempotent `CREATE TABLE IF NOT EXISTS` guard for `audit_logs`
> - `V8__create_notifications.sql` — `notifications` + 3 indexes (including partial index on unread rows)
> - `V9__create_tags.sql` — `tags` (unique per user) + `expense_tags` join table + 3 indexes
> - `V10__add_currency_to_expenses.sql` — `currency VARCHAR(3)` + `amount_in_base DOUBLE` on `expenses`, backfills existing rows
> - `V11__add_base_currency_to_users.sql` — `base_currency VARCHAR(3) DEFAULT 'INR'` on `users`
> - `V12__create_exchange_rates.sql` — `exchange_rates` table with USD-pivot rate pairs + 3 indexes

---

### 4.2 Soft Deletes — ✅ COMPLETE

> ✅ **Built** — `deleted_at` added to `expenses`, `budgets`, `recurring_expenses`, `categories`. All queries filter by `deletedAt IS NULL`.

---

### 4.3 DB Indexes — ✅ COMPLETE

> ✅ **Built** — indexes added in Flyway migrations V3–V5.

```sql
-- V3 (expenses)
CREATE INDEX idx_expenses_user_id      ON expenses(user_id);
CREATE INDEX idx_expenses_category_id  ON expenses(category_id);
CREATE INDEX idx_expenses_created_at   ON expenses(created_at);

-- V4 (budgets)
CREATE INDEX idx_budgets_user_id       ON budgets(user_id);
CREATE INDEX idx_budgets_is_active     ON budgets(is_active);

-- V5 (recurring_expenses)
CREATE INDEX idx_recurring_user_id       ON recurring_expenses(user_id);
CREATE INDEX idx_recurring_next_due_date ON recurring_expenses(next_due_date);
CREATE INDEX idx_recurring_is_active     ON recurring_expenses(is_active);
```

---

## 5. Reporting & Analytics Improvements

Your current reports are basic aggregates. Real value comes from trend analysis and comparative data.

### 5.1 Report Types — Status

| Report | Endpoint | Status |
|---|---|---|
| **Summary** | `GET /api/reports/summary` | ✅ Built |
| **Category-Wise** | `GET /api/reports/category-wise` | ✅ Built |
| **Date-Wise** | `GET /api/reports/date-wise` | ✅ Built |
| **Custom (multi-filter)** | `POST /api/reports/custom` | ✅ Built |
| **Month-over-Month Trends** | `GET /api/reports/trends?months=6` | ✅ Built |
| **Budget vs Actual** | `GET /api/reports/budget-performance` | ✅ Built |
| **Spending Insights** | `GET /api/v1/reports/insights` | ✅ Built |
| **Top Expenses** | `GET /api/v1/reports/top-expenses?limit=10&categoryId=` | ✅ Built |
| **Export to CSV** | `GET /api/v1/reports/export?format=csv` | ✅ Built |
| **Export to PDF** | `GET /api/v1/reports/export?format=pdf` | ✅ Built |

**Monthly Trend (`GET /api/reports/trends?months=6`)**

- Returns one entry per calendar month for the last N months (default 6, max 24)
- Months with zero spending are included with `totalAmount: 0`
- Each month has a `categoryBreakdown` list with category amounts and percentages
- Backed by two native PostgreSQL queries using `EXTRACT(YEAR/MONTH FROM created_at)`

**Budget Performance (`GET /api/reports/budget-performance`)**

- Returns all active budgets with real-time spent/remaining data
- `status` field: `ON_TRACK` | `NEAR_LIMIT` | `EXCEEDED`
- Delegates spending computation to `BudgetService.getBudgetStatus()` to avoid duplication
- Works for both category-scoped and overall (null category) budgets

**Spending Insights (`GET /api/v1/reports/insights`)**

Aggregates all of a user's expenses in-memory to produce a single insight snapshot:

| Field | Description |
|---|---|
| `totalThisMonth` | Sum of expenses from 1st of current month to now |
| `totalLastMonth` | Sum of expenses in the previous calendar month |
| `monthOverMonthChange` | `totalThisMonth - totalLastMonth` (positive = up, negative = down) |
| `spendingVelocity` | `INCREASING` / `DECREASING` / `STABLE` (threshold: ±0.01) |
| `averageDailySpendLast30Days` | Total spend in last 30 days ÷ 30 |
| `highestSpendDay` | `LocalDate` of the single day with most spend in last 30 days |
| `highestSpendDayAmount` | Total amount on that day |
| `biggestExpense` | Full `ExpenseResponse` of the largest single expense ever |
| `mostUsedCategory` | Category name used most often (by count, all-time) |
| `mostUsedCategoryCount` | How many times that category appears |

**Top Expenses (`GET /api/v1/reports/top-expenses?limit=10&categoryId=`)**

- Returns expenses sorted by `amount DESC`
- `limit` capped between 1 and 100 (default: 10)
- Optional `categoryId` query param narrows to one category
- Returns `List<ExpenseResponse>` including tags

### 5.2 Export to CSV / PDF — ✅ COMPLETE

> ✅ **Built** — `ExportService` with `exportCsv()` and `exportPdf()`. `ReportController` exposes `GET /api/reports/export?format=csv|pdf`. Both log `REPORT_EXPORTED` to the audit log.

**Endpoint: `GET /api/reports/export?format=csv|pdf`**

| format | Content-Type | Filename | Library |
|---|---|---|---|
| `csv` (default) | `text/csv` | `expenses.csv` | Apache Commons CSV 1.12.0 |
| `pdf` | `application/pdf` | `expenses.pdf` | OpenPDF 1.3.30 (`com.lowagie.text.*`) |

- Requires `SUPER_ADMIN` role (inherits from `@IsSuperAdmin` on `ReportController`)
- Expenses sorted by `createdAt DESC`
- PDF includes styled table: indigo header row, alternating row shading, 6 columns
- Both formats annotated `@Transactional(readOnly = true)` — keeps Hibernate session open for lazy-loaded `category`
- `AuditAction.REPORT_EXPORTED` logged with format, count, and timestamp

### 5.3 Add Redis Caching — ⬜ NOT YET BUILT

Reports that aggregate months of data should not hit the DB on every request.

- Add `spring-boot-starter-data-redis` to Gradle
- Annotate heavy service methods with `@Cacheable("monthly-summary")`
- On new expense creation: `@CacheEvict("monthly-summary")` to invalidate stale cache
- Summary reports cached for 5 minutes, dashboard for 1 minute

---

## 6. API Design Improvements

### 6.1 API Versioning — ✅ COMPLETE

> ✅ **Built** — All 10 controllers updated to `/api/v1/` prefix. Postman collection updated (52 URL occurrences). SecurityConfig `permitAll` paths updated. Integration tests updated. `DataInitializer` `@Order(1)/(2)` added to fix `CommandLineRunner` race condition surfaced during re-test.

**Updated controllers:**

| Controller | Old prefix | New prefix |
|---|---|---|
| `AuthController` | `/api/auth` | `/api/v1/auth` |
| `ExpenseController` | `/api/expenses` | `/api/v1/expenses` |
| `BudgetController` | `/api/budgets` | `/api/v1/budgets` |
| `ReportController` | `/api/reports` | `/api/v1/reports` |
| `NotificationController` | `/api/notifications` | `/api/v1/notifications` |
| `RecurringExpenseController` | `/api/recurring-expenses` | `/api/v1/recurring-expenses` |
| `DashboardController` | `/api/dashboard` | `/api/v1/dashboard` |
| `UserCategoryController` | `/api/categories` | `/api/v1/categories` |
| `AdminCategoryController` | `/api/admin/categories` | `/api/v1/admin/categories` |
| `AuditLogController` | `/api/audit-logs` | `/api/v1/audit-logs` |

When a breaking change is needed in future, add `/api/v2/` controllers — old clients keep using v1.

### 6.2 Swagger / OpenAPI — ✅ COMPLETE

> ✅ **Built** — `springdoc-openapi-starter-webmvc-ui:3.0.0` (Spring Boot 4.0 / Spring 7 compatible). Global JWT bearer auth configured via `@OpenAPIDefinition` + `@SecurityScheme`. Swagger paths added to `SecurityConfig.permitAll()`.

- Interactive docs at `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON at `http://localhost:8081/v3/api-docs`
- Click **Authorize** in Swagger UI → enter `Bearer <your-token>` → all endpoints callable
- Config class: `config/OpenApiConfig.kt`

### 6.3 Better Error Response Format — ✅ COMPLETE

> ✅ **Built** — `enums/ErrorCode.kt` enum + `exceptions/ErrorResponse.kt` DTO. All handlers in `GlobalExceptionHandler` now return `ErrorResponse` with a machine-readable `code`, `path` (injected via `HttpServletRequest`), `timestamp`, and optional `details` for validation field errors.

**Error codes defined in `ErrorCode` enum:**

| Category | Codes |
|---|---|
| Not Found | `RESOURCE_NOT_FOUND`, `USER_NOT_FOUND`, `TAG_NOT_FOUND` |
| Validation | `VALIDATION_FAILED`, `BAD_REQUEST` |
| Conflict | `CONFLICT`, `EMAIL_ALREADY_EXISTS`, `TAG_ALREADY_EXISTS` |
| Auth | `INVALID_CREDENTIALS`, `ACCOUNT_DEACTIVATED`, `AUTHENTICATION_FAILED`, `UNAUTHORIZED` |
| JWT | `TOKEN_EXPIRED`, `TOKEN_INVALID`, `TOKEN_SIGNATURE_INVALID` |
| Server | `INTERNAL_ERROR` |

**Error response shape:**

```json
{
  "status": false,
  "code": "RESOURCE_NOT_FOUND",
  "message": "Expense not found",
  "timestamp": "2026-03-15T10:30:00",
  "path": "/api/v1/expenses/42",
  "details": null
}
```

Validation errors populate `details` with a map of `{ fieldName: "error message" }`.

**Files:**
- `enums/ErrorCode.kt` — all machine-readable codes
- `exceptions/ErrorResponse.kt` — response DTO (status, code, message, timestamp, path, details)
- `exceptions/GlobalExceptionHandler.kt` — all 9 handlers updated; `ApiResponse<Nothing>` fully replaced

---

## 7. Additional Features Worth Adding

### 7.1 Multi-Currency Support — ✅ COMPLETE

> ✅ **Built** — Per-expense currency field, per-user base currency preference, `exchange_rates` table, daily auto-sync from `open.er-api.com` (free, no API key), USD-pivot cross-rate conversion, manual sync endpoint, and a convert helper endpoint.

**Database Migrations**

| Migration | Change |
|---|---|
| `V10__add_currency_to_expenses.sql` | Adds `currency VARCHAR(3) DEFAULT 'INR'` and `amount_in_base DOUBLE` to `expenses`. Backfills existing rows: `amount_in_base = amount`. |
| `V11__add_base_currency_to_users.sql` | Adds `base_currency VARCHAR(3) DEFAULT 'INR'` to `users`. |
| `V12__create_exchange_rates.sql` | Creates `exchange_rates(id, base_currency, target_currency, rate, fetched_at)` with UNIQUE constraint on the pair. Indexed on base, target, and pair. |

**How conversion works (USD-pivot)**

All rates are stored relative to USD:
```
base_currency = "USD", target_currency = "INR", rate = 83.5
→ 1 USD = 83.5 INR
```

Cross-rate for any A → B conversion:
```
convertedAmount = amount × ( rateUSD→B / rateUSD→A )
```

When an expense is created/updated, `amountInBase` is computed automatically:
```
1. Fetch user's baseCurrency (e.g. "INR")
2. Look up rate(USD→expense.currency) and rate(USD→user.baseCurrency)
3. amountInBase = amount × (rateUSD→baseCurrency / rateUSD→expenseCurrency)
4. Store both amount (original) and amountInBase (converted)
```

Same-currency expenses: `amountInBase = amount` (no rate lookup needed).

**Exchange rate sync**

- Source: `https://open.er-api.com/v6/latest/USD` — free, no API key required
- `@Scheduled(cron = "0 0 1 * * *")` — runs at 01:00 AM daily
- On sync: fetches all ~160 currencies, upserts each rate in DB
- `POST /api/v1/exchange-rates/sync` — manual trigger (requires ADMIN role)
- Sync failure never throws — logged and swallowed to prevent startup failure

**Files added / modified:**

| File | Change |
|---|---|
| `entity/ExchangeRate.kt` | New entity: `baseCurrency`, `targetCurrency`, `rate`, `fetchedAt` |
| `repository/ExchangeRateRepository.kt` | `findByBaseCurrency`, `findByBaseCurrencyAndTargetCurrency` |
| `service/ExchangeRateService.kt` | `convert()`, `syncRates()`, `@Scheduled` daily sync, `RestClient` HTTP call |
| `controllers/ExchangeRateController.kt` | 3 endpoints at `/api/v1/exchange-rates` |
| `entity/User.kt` | Added `baseCurrency: String = "INR"` |
| `entity/Expense.kt` | Added `currency: String = "INR"`, `amountInBase: Double` |
| `dto/request/ExpenseRequest.kt` | Added `currency: String = "INR"` |
| `dto/request/UpdateBaseCurrencyRequest.kt` | New: `baseCurrency` with `@Size(3)` + `@Pattern([A-Z]{3})` |
| `dto/response/ExpenseResponse.kt` | Added `currency`, `amountInBase` |
| `dto/response/UserResponse.kt` | Added `baseCurrency` |
| `dto/response/ExchangeRateResponse.kt` | New: id, baseCurrency, targetCurrency, rate, fetchedAt |
| `dto/response/CurrencyConversionResponse.kt` | New: fromCurrency, toCurrency, originalAmount, convertedAmount |
| `service/ExpenseService.kt` | Injected `ExchangeRateService` + `UserRepository`; computes `amountInBase` on create/update |
| `service/AuthService.kt` | Added `updateBaseCurrency()`; `toUserResponse()` includes `baseCurrency` |
| `controllers/AuthController.kt` | Added `PUT /api/v1/auth/me/currency` |
| `enums/AuditAction.kt` | Added `EXCHANGE_RATE_SYNCED`, `USER_CURRENCY_UPDATED` |
| `ExpensetrackerApplication.kt` | Added `@EnableScheduling` |

**Completed endpoints:**

| Endpoint | Auth | Description |
|---|---|---|
| `GET /api/v1/exchange-rates` | Authenticated | List all stored rates (USD-base) |
| `GET /api/v1/exchange-rates/convert?from=USD&to=INR&amount=100` | Authenticated | Convert amount between any two currencies |
| `POST /api/v1/exchange-rates/sync` | ADMIN | Manually trigger full rate sync from open.er-api.com |
| `PUT /api/v1/auth/me/currency` | Authenticated | Update the user's base currency preference |

### 7.2 Receipt Attachments — ✅ COMPLETE

> ✅ **Built** — `receipts` table (V14 migration), `ReceiptService`, `ReceiptController`. Files stored directly in the database as `BYTEA`. A dedicated download endpoint streams the raw bytes. Audit logged. Supports **batch upload** (multiple files per request) and a combined **create-expense-with-receipts** endpoint.

**DB Table (V14/V15 migrations)**

```sql
TABLE: receipts
  id           BIGSERIAL PK
  expense_id   BIGINT NOT NULL REFERENCES expenses(id) ON DELETE CASCADE
  user_id      BIGINT NOT NULL
  file_name    VARCHAR(255)     -- original name from client
  file_data    BYTEA NOT NULL   -- raw file bytes stored in DB
  file_size    BIGINT           -- bytes
  content_type VARCHAR(100)     -- image/jpeg | image/png | application/pdf
  uploaded_at  TIMESTAMP DEFAULT NOW()

INDEXES:
  idx_receipts_expense_id  ON receipts(expense_id)
  idx_receipts_user_id     ON receipts(user_id)
```

**Upload rules:**
- Allowed types: `image/jpeg`, `image/png`, `application/pdf`
- Max file size: 5 MB per file
- Max receipts per expense: 5 (checked against existing count before batch)
- All files are validated **before** any are saved — fail fast, no partial writes

**Completed endpoints:**

| Endpoint | Auth | Description |
|---|---|---|
| `POST /api/v1/expenses/{id}/receipts` | Authenticated | Upload one or more receipts (multipart/form-data, field: `files`). Returns `List<ReceiptResponse>`. |
| `POST /api/v1/expenses/with-receipt` | Authenticated | Create an expense + attach receipts in **one call** (multipart: `expense` JSON part + optional `files`). |
| `GET /api/v1/expenses/{id}/receipts` | Authenticated | List all receipts with download URL per receipt |
| `GET /api/v1/expenses/{id}/receipts/{receiptId}/download` | Authenticated | Stream the raw file bytes with correct Content-Type header |
| `DELETE /api/v1/expenses/{id}/receipts/{receiptId}` | Authenticated | Delete a receipt from the DB |

**Postman — how to send multiple files:**

In the Body → form-data tab, use the **same key** (`files`) for each row and change the type dropdown to **File**:
```
Key: files  Type: File  →  select bill1.jpg
Key: files  Type: File  →  select bill2.png
```

**Combined create + upload (`POST /api/v1/expenses/with-receipt`):**
```
Key: expense  Type: Text (Content-Type: application/json)
  Value: {"title":"Lunch","amount":250,"categoryId":1,"userId":0,"currency":"INR"}
Key: files    Type: File  →  select file(s)
```

**Files added / modified:**

| File | Change |
|---|---|
| `entity/Receipt.kt` | New entity — `fileData: ByteArray` mapped as `BYTEA` (no `@Lob`) |
| `repository/ReceiptRepository.kt` | `findByExpenseIdAndUserId`, `findByIdAndUserId`, `countByExpenseId` |
| `service/ReceiptService.kt` | Saves `file.bytes` directly to DB; `downloadUrl` points to `/download` endpoint; `getReceiptForDownload()` for streaming |
| `controllers/ReceiptController.kt` | Upload, list, download (`GET /{id}/download`), delete |
| `service/ExpenseService.kt` | `createExpenseWithReceipt(request, List<MultipartFile>)` — creates expense then calls `ReceiptService`; `@Lazy` inject to avoid circular dep |
| `controllers/ExpenseController.kt` | `POST /with-receipt` — multipart with `expense` JSON part + optional `files` list |
| `dto/response/ReceiptResponse.kt` | id, expenseId, fileName, fileSize, contentType, uploadedAt, downloadUrl |
| `enums/AuditAction.kt` | Added `RECEIPT_UPLOADED`, `RECEIPT_DELETED` |
| `enums/ErrorCode.kt` | Added `RECEIPT_NOT_FOUND`, `RECEIPT_INVALID_TYPE`, `RECEIPT_SIZE_EXCEEDED`, `RECEIPT_LIMIT_EXCEEDED` |
| `application.properties` | Multipart size limits (5 MB) |
| `db/migration/V14__create_receipts.sql` | Creates receipts table with `file_data BYTEA` |
| `db/migration/V15__receipts_use_file_data.sql` | Migration for existing DBs: drops `s3_key`, adds `file_data BYTEA` |
| `db/migration/V16__audit_logs_add_receipt_actions.sql` | Extends `audit_logs_action_check` constraint with `RECEIPT_UPLOADED`, `RECEIPT_DELETED` |

### 7.3 Tags / Labels — ✅ COMPLETE

> ✅ **Built** — Many-to-many relationship between expenses and tags. Tags are per-user. V9 migration creates the `tags` and `expense_tags` tables. Full CRUD at `/api/v1/tags`. Expenses can be filtered by `tagIds`. Audit logged.

**DB Tables (V9__create_tags.sql)**

```sql
TABLE: tags
  id         BIGSERIAL PK
  name       VARCHAR(100) NOT NULL
  color      VARCHAR(7) DEFAULT '#6366f1'
  user_id    BIGINT NOT NULL
  created_at TIMESTAMP DEFAULT NOW()
  UNIQUE (name, user_id)

TABLE: expense_tags
  expense_id BIGINT NOT NULL REFERENCES expenses(id) ON DELETE CASCADE
  tag_id     BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE
  PRIMARY KEY (expense_id, tag_id)

INDEXES:
  idx_tags_user_id              ON tags(user_id)
  idx_expense_tags_expense_id   ON expense_tags(expense_id)
  idx_expense_tags_tag_id       ON expense_tags(tag_id)
```

**Files added / modified:**

| File | Change |
|---|---|
| `entity/Tag.kt` | New entity — `@Entity`, `@Table(uniqueConstraints = [name, user_id])` |
| `repository/TagRepository.kt` | `findByUserId`, `findByIdAndUserId`, `existsByNameAndUserId` |
| `service/TagService.kt` | Create/get/update/delete with `ConflictException` on duplicate name; full audit logging |
| `controllers/TagController.kt` | `@IsAuthenticated`, 5 endpoints at `/api/v1/tags` |
| `entity/Expense.kt` | Added `@ManyToMany(EAGER)` `tags` field with `@JoinTable(expense_tags)` |
| `dto/request/ExpenseRequest.kt` | Added `tagIds: List<Long> = emptyList()` |
| `dto/request/ExpenseFilterRequest.kt` | Added `tagIds: List<Long>? = null` |
| `dto/response/ExpenseResponse.kt` | Added `tags: List<TagResponse>` field; `fromEntity()` maps tags |
| `specifications/ExpenseSpecifications.kt` | Added `filterByTagIds()` using INNER JOIN; wired into `buildFilterSpecification` |
| `service/ExpenseService.kt` | Injected `TagRepository`; `createExpense`/`updateExpense` resolve tags by ownership |
| `enums/AuditAction.kt` | Added `TAG_CREATED`, `TAG_UPDATED`, `TAG_DELETED` |
| `dto/response/TagResponse.kt` | New DTO: id, name, color, userId, createdAt |
| `dto/request/TagRequest.kt` | New request: `@NotBlank` name + `@Pattern` hex color |

**Completed endpoints:**

| Endpoint | Auth | Description |
|---|---|---|
| `POST /api/v1/tags` | Authenticated | Create a new tag (conflict on duplicate name) |
| `GET /api/v1/tags` | Authenticated | List all tags for the current user |
| `GET /api/v1/tags/{id}` | Authenticated | Get one tag (ownership enforced) |
| `PUT /api/v1/tags/{id}` | Authenticated | Update name/color (conflict check on rename) |
| `DELETE /api/v1/tags/{id}` | Authenticated | Delete tag (cascades to expense_tags) |

To filter expenses by tags: `POST /api/v1/expenses/filter` with `tagIds: [1, 3]` in the request body.

### 7.4 OAuth2 / Social Login — ⬜ NOT YET BUILT

- Add `spring-boot-starter-oauth2-client` to Gradle
- Support Google and GitHub login
- On OAuth2 success: create user if not exists, assign USER role, return JWT
- Users authenticated via OAuth2 have no password — handle in registration checks

---

## 8. Testing — ✅ COMPLETE

> ✅ **Built** — 45 tests across unit, integration, and security layers. All passing. H2 in-memory DB for tests; Flyway disabled; `EnvConfig` post-processor skipped via `test` profile guard.

### 8.1 Test Infrastructure

| File | Purpose |
|---|---|
| `src/test/resources/application-test.properties` | H2 datasource, Flyway disabled, `create-drop` DDL, test JWT secret |
| `config/EnvConfig.kt` | Patched to skip env-var enforcement when `test` profile is active |

**Dependencies added to `build.gradle.kts`:**
```kotlin
testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
testImplementation("org.mockito:mockito-core:5.14.2")
testRuntimeOnly("com.h2database:h2")
testImplementation("org.springframework.security:spring-security-test")
```

### 8.2 Test Coverage

| Test Class | Tests | What It Covers |
|---|---|---|
| `ExpenseServiceTest` | 9 | create (success + budget block), get (found/not found/wrong user), update (success/not found), delete (success/wrong user) |
| `BudgetServiceTest` | 6 | create overall budget, list with spent, soft-delete, delete not found throws, no-block when no budgets, block when over limit |
| `NotificationServiceTest` | 7 | send (success + swallows errors), markAsRead (success + wrong user throws), delete (success + not found throws), unread count |
| `AuthServiceTest` | 6 | register (success + duplicate email throws), login (success + wrong password + not found + deactivated account) |
| `SecurityIntegrationTest` | 10 | Swagger UI / OpenAPI accessible without token; login/register reachable; 6 protected endpoints return 401 |
| `ExpenseOwnershipIntegrationTest` | 6 | User A can read/update/delete own expense; User B returns null/false for each operation — no cross-user leakage |

### 8.3 Key Test Cases Covered

- ✅ User A cannot GET, PUT, DELETE User B's expense — returns `null`/`false` (controller maps to 404)
- ✅ Creating expense over budget throws `BadRequestException` — expense never saved
- ✅ Login with wrong password returns 401 via `GlobalExceptionHandler`
- ✅ Deactivated account blocks login
- ✅ Duplicate email registration throws `IllegalArgumentException`
- ✅ Swagger UI at `/swagger-ui/index.html` returns 200 without token
- ✅ All protected endpoints return 401 without token

---

## 9. DevOps & Production Readiness

### 9.1 Observability Stack

| Component | Status | Detail |
|---|---|---|
| **Health Checks** | ✅ Done | Spring Actuator: `/actuator/health` and `/actuator/info` exposed; `show-details=when-authorized` |
| **Metrics** | ⬜ Not built | Micrometer + Prometheus: expose `/actuator/prometheus` endpoint |
| **Dashboards** | ⬜ Not built | Grafana: connect to Prometheus, create expense creation rate, error rate panels |
| **Structured Logs** | ⬜ Not built | Logback: output JSON logs with userId, requestId, duration per request |
| **Log Aggregation** | ⬜ Not built | Ship logs to ELK stack (Elasticsearch + Logstash + Kibana) |

### 9.2 Secrets Management

> ⚠️ **Current Problem**
>
> JWT_SECRET and DB credentials live in a `.env` file. In production, this file can be accidentally committed, leaked, or accessed by the wrong person.

- AWS Secrets Manager or HashiCorp Vault for production secrets
- GitHub Actions Secrets for CI/CD pipeline credentials
- Never commit `.env` to version control — add to `.gitignore`
- Rotate `JWT_SECRET` periodically — have a migration plan for active tokens

### 9.3 CI/CD Pipeline (GitHub Actions)

```
Recommended pipeline: push to main →
  1. Run unit tests (fail fast)
  2. Run integration tests with Testcontainers
  3. Build Docker image
  4. Push to container registry (GHCR or ECR)
  5. Deploy to staging (auto)
  6. Deploy to production (manual approval)
```

---

## 10. Recommended Priority Order

| Priority | Task | Status |
|---|---|---|
| **Priority 1** | Flyway migrations V1–V7 (V6: audit_logs, V7: idempotent guard) | ✅ Done |
| **Priority 2** | Soft deletes on expenses, budgets, recurring expenses | ✅ Done |
| **Priority 3** | DB indexes via Flyway (user_id, created_at, next_due_date, is_active) | ✅ Done |
| **Priority 4** | Budgets table + budget check on expense creation + period reset scheduler | ✅ Done |
| **Priority 5** | Recurring expenses + @Scheduled auto-creation job at 00:05 | ✅ Done |
| **Priority 6** | Notifications table + budget alert firing + recurring notification | ✅ Done |
| **Priority 7** | Dashboard summary endpoint (single call for frontend) | ✅ Done |
| **Priority 8** | Rate limiting on /api/auth/login (bucket4j) | ✅ Done |
| **Priority 9** | Refresh tokens + Redis blacklist for logout | ⬜ Deferred |
| **Priority 10** | Unit tests: ExpenseService, AuthService, BudgetService, NotificationService | ✅ Done |
| **Priority 11** | Integration tests: security (401 enforcement) + expense ownership | ✅ Done |
| **Priority 12** | Swagger / OpenAPI (springdoc 3.0.0, JWT bearer, permitAll in SecurityConfig) | ✅ Done |
| **Priority 13** | Export to CSV / PDF (ExportService + GET /api/reports/export endpoint) | ✅ Done |
| **Priority 14** | Month-over-month trend reports + budget vs actual | ✅ Done |
| **Priority 15** | API versioning: /api/v1/ prefix on all 10 controllers + Postman + tests | ✅ Done |
| **Priority 16** | Spending Insights report (month-over-month, velocity, highest day, biggest expense) | ✅ Done |
| **Priority 17** | Top Expenses report (sorted by amount desc, limit param, optional category filter) | ✅ Done |
| **Priority 18** | Machine-readable error codes (ErrorCode enum + ErrorResponse DTO + GlobalExceptionHandler rewrite) | ✅ Done |
| **Priority 19** | Tags / Labels (many-to-many with expenses, V9 migration, TagController, filter by tagIds) | ✅ Done |
| **Priority 20** | Multi-currency support (V10–V12, ExchangeRateService, USD-pivot, daily sync, convert endpoint) | ✅ Done |
| **Priority 21** | Redis caching for reports | ⬜ Not built |
| **Priority 22** | Receipt attachments (AWS S3) | ✅ Done |
| **Priority 23** | OAuth2 / Google login | ⬜ Not built |

---

> ✅ **Final Note**
>
> Your foundation is genuinely solid. The role system, JPA Specifications, JWT setup, and Docker configuration are well-designed.
>
> ~~Start with Flyway + soft deletes + indexes (data safety)~~ ✅ Done
>
> ~~Build Budgets (budget check, alerts, period reset scheduler)~~ ✅ Done
>
> ~~Build Recurring Expenses (@Scheduled auto-creation, nextDueDate advancement, auto-deactivation)~~ ✅ Done
>
> ~~Build Notifications (NotificationService, 4 types, auto-triggered by budget & scheduler)~~ ✅ Done
>
> ~~Build Dashboard (monthly summary, budget overview, recent expenses, upcoming recurring, category breakdown)~~ ✅ Done
>
> ~~Fix duplicate @PreAuthorize on ReportController + UserCategoryController~~ ✅ Fixed
>
> ~~Build Trend Reports (monthly trend with category breakdown + budget performance GET /api/reports/trends + /budget-performance)~~ ✅ Done
>
> ~~**Now: Tests** — unit + integration tests are the most critical remaining task for a financial app~~ ✅ Done (45 tests, 0 failures)
>
> ~~**Then: Swagger / OpenAPI** — one dependency, interactive docs, massive developer experience improvement~~ ✅ Done (springdoc 3.0.0, JWT bearer auth)
>
> ~~**Then: Export** — CSV/PDF export via Apache Commons CSV + OpenPDF~~ ✅ Done (`GET /api/reports/export?format=csv|pdf`)
>
> ~~**Now: API Versioning** — add `/api/v1/` prefix before the API grows further~~ ✅ Done (all 10 controllers + Postman + tests)
>
> ~~**Then: Spending Insights + Top Expenses** — in-memory aggregation, no extra DB queries~~ ✅ Done (`GET /api/v1/reports/insights` + `/top-expenses`)
>
> ~~**Then: Machine-readable error codes** — `ErrorCode` enum + `ErrorResponse` DTO, `GlobalExceptionHandler` rewritten~~ ✅ Done
>
> ~~**Then: Tags / Labels** — V9 migration, `Tag` entity, `TagController`, many-to-many on `Expense`, filter by tagIds~~ ✅ Done
>
> ~~**Then: Multi-Currency** — V10–V12 migrations, ExchangeRateService, USD-pivot cross-rate conversion, daily @Scheduled sync, convert endpoint, user base currency preference~~ ✅ Done
>
> **Then: Redis Caching** — cache heavy report queries, invalidate on expense create
>
> **Then: Refresh Tokens** — short-lived access tokens (15 min) + DB-backed refresh tokens