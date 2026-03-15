# Expense Tracker Spring

## Improvement & Missing Features Guide

> What's Built | What's Missing | How to Fix It | Priority Order

**Kotlin 2.2 • Spring Boot 4.0 • PostgreSQL • JWT • Docker**

---

## 1. Honest Assessment — Where the Project Stands

> ⚠️ **The One-Line Summary**
>
> Your project is a well-built expense **LOGGER**. To become a real expense **TRACKER**, it needs budgets, recurring entries, and alerts. Those three things are the entire difference.
>
> Infrastructure: ✅ Solid | Core Business Features: ✅ Foundation Complete | Next: Notifications + Tests

---

## 1.1 Feature Completeness Overview

| Feature | Status | Priority | Notes |
|---|---|---|---|
| **Authentication / JWT** | ✅ Built | Done | Solid implementation with HMAC-SHA256, 24hr expiry |
| **Role System** | ✅ Built | Done | 6 roles, custom annotations, @PreAuthorize |
| **Expense CRUD** | ✅ Built | Done | Create, read, update, delete with ownership checks |
| **Dynamic Filtering** | ✅ Built | Done | JPA Specifications with composable filters |
| **Basic Reports** | ⚠️ Partial | Medium | Summary, category-wise, date-wise — but no trends |
| **Docker / DevOps** | ✅ Built | Done | Multi-stage build, non-root user, env vars |
| **Exception Handling** | ✅ Built | Done | GlobalExceptionHandler with consistent ApiResponse |
| **Budgets** | ✅ Done | ~~Critical~~ | ✅ Built: budget check on expense creation, alerts, scheduler |
| **Recurring Expenses** | ✅ Done | ~~Critical~~ | ✅ Built: @Scheduled auto-creation, nextDueDate advancement |
| **Notifications / Alerts** | ⬜ Missing | Critical | Budget alerts fire but NotificationService not yet built |
| **Dashboard API** | ⬜ Missing | High | No single summary endpoint for frontend use |
| **Soft Deletes** | ✅ Done | ~~High~~ | ✅ Built: deleted_at on expenses, budgets, recurring expenses |
| **Refresh Tokens** | ⬜ Deferred | High | Logout is cosmetic — tokens still work after logout |
| **Rate Limiting** | ✅ Done | ~~High~~ | ✅ Built: bucket4j, 5 attempts/IP/min on /api/auth/login |
| **Audit Logging** | ✅ Done | ~~High~~ | ✅ Built: audit_logs table, V6 migration, wired into all services, 5 controller endpoints |
| **Multi-currency** | ⬜ Missing | Medium | Single currency only — unusable internationally |
| **Attachments / Receipts** | ⬜ Missing | Medium | No way to attach receipt images to expenses |
| **Tags / Labels** | ⬜ Missing | Medium | Categories alone are too rigid for real use |
| **Export (CSV / PDF)** | ⬜ Missing | Medium | No way to download expense data |
| **API Versioning** | ⬜ Missing | Medium | No /api/v1/ prefix — breaking changes will hurt |
| **Flyway Migrations** | ✅ Done | ~~High~~ | ✅ Built: V1–V7 migration scripts, ddl-auto=update (base) / validate (profiles), baseline-on-migrate |
| **Tests** | ⬜ Missing | High | No unit, integration, or security tests |
| **Swagger / OpenAPI** | ⬜ Missing | Medium | No auto-generated API docs |
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

### 2.3 Notifications & Alerts — ⬜ NOT YET BUILT

> ❌ **Why This Matters**
>
> Budget alerts already fire in `BudgetService` but `NotificationService` doesn't exist yet — those calls will fail at runtime. Budget alerts, due-date reminders, and monthly summaries are all standard features.

**Required DB Table**

```sql
TABLE: notifications
  id         BIGINT PK AUTO_INCREMENT
  user_id    BIGINT NOT NULL
  type       VARCHAR  -- BUDGET_ALERT / RECURRING_DUE / MONTHLY_SUMMARY
  title      VARCHAR NOT NULL
  message    VARCHAR NOT NULL
  is_read    BOOLEAN DEFAULT false
  created_at TIMESTAMP
```

**Alert Types to Implement**

| Type | Trigger |
|---|---|
| `BUDGET_ALERT` | Fired when spending crosses 80% or 100% of a budget limit |
| `RECURRING_DUE` | Fired 1 day before a recurring expense is auto-created |
| `MONTHLY_SUMMARY` | Sent on 1st of each month: last month's total, top category, budget performance |
| `LARGE_EXPENSE` | Configurable threshold: 'You just logged an expense of Rs. 5,000' |

---

### 2.4 Dashboard Summary Endpoint — ⬜ NOT YET BUILT

> ❌ **Why This Matters**
>
> Any frontend (mobile app, web app) needs a single call to load the home screen. Right now it would take 4+ API calls. That is unacceptable for a real app.

**New Endpoint: `GET /api/dashboard`**

```json
{
  "thisMonth": {
    "total": 12400,
    "expenseCount": 34,
    "vsLastMonth": "+18%"
  },
  "budgets": [
    { "category": "Food", "limit": 5000, "spent": 4200, "percentUsed": 84 }
  ],
  "topCategories": [{ "name": "Food", "amount": 4200 }],
  "recentExpenses": [ ...last 5 expenses ],
  "unreadNotifications": 3,
  "alerts": ["Food budget at 84%"]
}
```

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

### 5.1 Missing Report Types — ⬜ NOT YET BUILT

| Report | Endpoint |
|---|---|
| **Month-over-Month Trends** | `GET /api/reports/trends` — spending per month for last 6/12 months, per category |
| **Budget vs Actual** | `GET /api/reports/budget-performance` — compare budgeted vs real spend per period |
| **Spending Insights** | `GET /api/reports/insights` — 'You spent 40% more on Food this month vs last month' |
| **Top Expenses** | `GET /api/reports/top-expenses` — highest single expenses in a period |
| **Export to CSV** | `GET /api/reports/export?format=csv` — Apache POI or OpenCSV |
| **Export to PDF** | `GET /api/reports/export?format=pdf` — JasperReports or iText |

### 5.2 Add Redis Caching — ⬜ NOT YET BUILT

Reports that aggregate months of data should not hit the DB on every request.

- Add `spring-boot-starter-data-redis` to Gradle
- Annotate heavy service methods with `@Cacheable("monthly-summary")`
- On new expense creation: `@CacheEvict("monthly-summary")` to invalidate stale cache
- Summary reports cached for 5 minutes, dashboard for 1 minute

---

## 6. API Design Improvements

### 6.1 API Versioning — ⬜ NOT YET BUILT

> ❌ **Current Problem**
>
> All your routes are `/api/expenses`, `/api/auth` etc. with no version prefix. The moment you make a breaking change, every existing client breaks. This is fixable now with zero effort. It becomes painful later.

- Change all routes to `/api/v1/expenses`, `/api/v1/auth`, etc.
- Use `@RequestMapping("/api/v1")` at controller class level
- When breaking changes are needed, add `/api/v2/` controllers — old clients keep working

### 6.2 Swagger / OpenAPI — ⬜ NOT YET BUILT

One dependency, massive developer experience improvement. Auto-generates interactive API docs from your controllers.

```kotlin
// build.gradle.kts — add:
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
```

- Docs available at `http://localhost:8081/swagger-ui.html`
- Add `@Operation` and `@Tag` annotations to controllers for richer docs
- Add to SecurityConfig: permit `/swagger-ui/**`, `/v3/api-docs/**`

### 6.3 Better Error Response Format — ⬜ NOT YET BUILT

Add machine-readable error codes so frontends can handle errors programmatically.

```json
// Current:
{ "status": false, "message": "Expense not found" }

// Improved:
{
  "status": false,
  "code": "EXPENSE_NOT_FOUND",
  "message": "Expense with ID 42 not found",
  "timestamp": "2025-03-12T10:30:00Z",
  "path": "/api/v1/expenses/42"
}
```

---

## 7. Additional Features Worth Adding

### 7.1 Multi-Currency Support — ⬜ NOT YET BUILT

| Change | Detail |
|---|---|
| `expenses` table | Add: `currency VARCHAR(3)`, `amount_in_base DOUBLE` |
| User preference | Add: `base_currency` to users table (default: INR) |
| Exchange rates | Integrate exchangerate-api.com (free tier available) |
| Scheduled sync | `@Scheduled` daily job to refresh exchange rates |
| Report behavior | All reports aggregate in base currency using stored rates |

### 7.2 Receipt Attachments — ⬜ NOT YET BUILT

- Add `receipts` table: `id`, `expense_id`, `file_path`, `file_name`, `uploaded_at`
- Store files in MinIO (self-hosted S3-compatible) or AWS S3
- `POST /api/expenses/{id}/receipts` — multipart file upload
- `GET /api/expenses/{id}/receipts` — list attachments with pre-signed URLs
- Max file size: 5MB, allowed types: jpg, png, pdf

### 7.3 Tags / Labels — ⬜ NOT YET BUILT

Categories are too rigid. A single expense might be 'Food' AND 'Work trip' AND 'Reimbursable'. Tags solve this.

```sql
TABLE: tags          → id, name, color, user_id
TABLE: expense_tags  → expense_id, tag_id  (Many-to-Many join)
```

- `GET /api/expenses/filter?tagIds=1,3` — filter by tags using Specifications
- Tags are personal (per-user), not global

### 7.4 OAuth2 / Social Login — ⬜ NOT YET BUILT

- Add `spring-boot-starter-oauth2-client` to Gradle
- Support Google and GitHub login
- On OAuth2 success: create user if not exists, assign USER role, return JWT
- Users authenticated via OAuth2 have no password — handle in registration checks

---

## 8. Testing — ⬜ NOT YET BUILT

> ❌ **This is Non-Negotiable for a Financial App**
>
> You have no tests. A bug in `ExpenseService` could let users see each other's expenses. A bug in `ReportService` could show wrong totals. These are financial errors. Add tests before adding more features.

### 8.1 What to Test and How

| Type | Detail |
|---|---|
| **Unit Tests** | Service layer with Mockito. Focus: `ExpenseService`, `AuthService`, `BudgetService`, `ReportService` |
| **Repository Tests** | `@DataJpaTest`: test custom JPQL in `ReportRepository` and all Specifications |
| **Integration Tests** | `@SpringBootTest` + Testcontainers: real PostgreSQL in Docker for end-to-end flow |
| **Security Tests** | Verify a user cannot access another user's expenses (ownership enforcement) |
| **Controller Tests** | `@WebMvcTest`: verify HTTP status codes, request validation, role access |

### 8.2 Key Test Cases to Write First

- User A cannot GET, PUT, DELETE User B's expense — returns 404 not 403
- Creating expense with invalid categoryId returns 404
- Creating expense over budget fires notification
- Expired JWT returns 401 with correct error message
- ADMIN can create global category, USER cannot
- Monthly budget resets correctly on period rollover

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
| **Priority 6** | Notifications table + budget alert firing | ⬜ NotificationService not built |
| **Priority 7** | Dashboard summary endpoint (single call for frontend) | ⬜ Not built |
| **Priority 8** | Rate limiting on /api/auth/login (bucket4j) | ✅ Done |
| **Priority 9** | Refresh tokens + Redis blacklist for logout | ⬜ Deferred |
| **Priority 10** | Unit tests: ExpenseService, AuthService, BudgetService, RecurringExpenseService | ⬜ Not built |
| **Priority 11** | Integration tests: Testcontainers + ownership verification | ⬜ Not built |
| **Priority 12** | Swagger / OpenAPI (one dependency, huge value) | ⬜ Not built |
| **Priority 13** | API versioning: /api/v1/ prefix | ⬜ Not built |
| **Priority 14** | Month-over-month trend reports + budget vs actual | ⬜ Not built |
| **Priority 15** | Redis caching for reports | ⬜ Not built |
| **Priority 16** | Export to CSV / PDF | ⬜ Not built |
| **Priority 17** | Tags / Labels (many-to-many with expenses) | ⬜ Not built |
| **Priority 18** | Receipt attachments (MinIO / S3) | ⬜ Not built |
| **Priority 19** | Multi-currency support + exchange rate sync | ⬜ Not built |
| **Priority 20** | OAuth2 / Google login | ⬜ Not built |

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
> **Now: Notifications + Dashboard** — the last two critical business features
>
> **Then: Tests** — unit + integration tests before shipping any more features