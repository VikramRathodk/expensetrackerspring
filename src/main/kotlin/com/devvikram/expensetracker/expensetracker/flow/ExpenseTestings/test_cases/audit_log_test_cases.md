# Audit Log Feature — Test Cases

Covers `AuditLogController`, `AuditLogService`, `AuditLogRepository`, and audit integration across
`ExpenseService`, `BudgetService`, `RecurringExpenseService`, and `AuthService`.
All test cases are verified against the actual source code.

---

## API Endpoints Reference

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/audit-logs` | `@IsAdmin` | All logs paginated → **200 OK** |
| `GET` | `/api/audit-logs/me` | `@IsAuthenticated` | Current user's own audit trail → **200 OK** |
| `GET` | `/api/audit-logs/user/{userId}` | `@IsSuperAdmin` | Any user's audit trail → **200 OK** |
| `GET` | `/api/audit-logs/entity/{entityType}/{entityId}` | `@IsAdmin` | Entity change history → **200 OK** |
| `GET` | `/api/audit-logs/action/{action}` | `@IsAdmin` | Logs filtered by action type → **200 OK** |

**AuditAction enum values:**
`EXPENSE_CREATED` | `EXPENSE_UPDATED` | `EXPENSE_DELETED` | `EXPENSE_AUTO_CREATED`
`BUDGET_CREATED` | `BUDGET_UPDATED` | `BUDGET_DELETED`
`RECURRING_EXPENSE_CREATED` | `RECURRING_EXPENSE_UPDATED` | `RECURRING_EXPENSE_DELETED` | `RECURRING_EXPENSE_PROCESSED`
`CATEGORY_CREATED` | `CATEGORY_UPDATED` | `CATEGORY_DELETED`
`USER_REGISTERED` | `USER_LOGIN` | `ROLE_ASSIGNED`

---

## 1. Get All Logs — `GET /api/audit-logs`

### TC-AL-001 — ADMIN fetches all logs (happy path)
**Preconditions:** Several audit log entries exist across multiple users.
**Expected:** `200 OK`, `data.content` is a non-null array, ordered newest first.
**Required fields per log:** `id`, `userId`, `action`, `entityType`, `createdAt`.

---

### TC-AL-002 — Response is paginated
**Request:** `GET /api/audit-logs?page=0&size=5`
**Expected:** `data.content.length <= 5`, `data.totalElements` reflects full count, `data.number = 0`.

---

### TC-AL-003 — Logs are ordered newest first
**Preconditions:** At least 2 log entries with different `createdAt`.
**Expected:** `content[0].createdAt >= content[1].createdAt` (descending).
**Repository method:** `findAllByOrderByCreatedAtDesc(pageable)`.

---

### TC-AL-004 — Regular USER role returns 403
**Call:** `GET /api/audit-logs` with a USER token.
**Expected:** `403 Forbidden` — endpoint requires `@IsAdmin`.

---

### TC-AL-005 — No token returns 401
**Call:** `GET /api/audit-logs` with no `Authorization` header.
**Expected:** `401 Unauthorized` from `JwtAuthenticationEntryPoint`.

---

## 2. Get My Logs — `GET /api/audit-logs/me`

### TC-AL-006 — Authenticated user sees only their own logs (happy path)
**Preconditions:** User A has 3 logs; User B has 5 logs.
**Call:** `GET /api/audit-logs/me` authenticated as User A.
**Expected:** `200 OK`, all returned logs have `userId = User A's id`.
**Repository method:** `findByUserIdOrderByCreatedAtDesc(userId, pageable)`.

---

### TC-AL-007 — Returns empty page when user has no audit logs
**Preconditions:** New user with no activity.
**Expected:** `200 OK`, `data.content = []`, `data.totalElements = 0`.

---

### TC-AL-008 — Available to any authenticated role (USER, ADMIN, SUPER_ADMIN)
**Call:** `GET /api/audit-logs/me` authenticated as USER role.
**Expected:** `200 OK` — this endpoint uses `@IsAuthenticated`, not `@IsAdmin`.

---

## 3. Get Logs By User ID — `GET /api/audit-logs/user/{userId}`

### TC-AL-009 — SUPER_ADMIN fetches any user's logs
**Preconditions:** User with `id=2` has audit entries.
**Call:** `GET /api/audit-logs/user/2` as SUPER_ADMIN.
**Expected:** `200 OK`, all returned logs have `userId = 2`.

---

### TC-AL-010 — ADMIN (not SUPER_ADMIN) returns 403
**Call:** `GET /api/audit-logs/user/2` with ADMIN token.
**Expected:** `403 Forbidden` — endpoint requires `@IsSuperAdmin`.

---

### TC-AL-011 — Non-existent userId returns empty page (not 404)
**Call:** `GET /api/audit-logs/user/99999`
**Expected:** `200 OK`, `data.content = []` — repository returns empty page, no exception thrown.

---

## 4. Get Entity History — `GET /api/audit-logs/entity/{entityType}/{entityId}`

### TC-AL-012 — Full change history for an Expense (happy path)
**Preconditions:** Expense `id=1` was created then updated.
**Expected:** `200 OK`, `data` is a list with at minimum `EXPENSE_CREATED` and `EXPENSE_UPDATED` entries, all with `entityType="Expense"` and `entityId=1`, ordered newest first.

---

### TC-AL-013 — Entity history for a deleted entity still returns logs
**Preconditions:** Expense `id=3` was created then soft-deleted.
**Expected:** List contains `EXPENSE_CREATED` and `EXPENSE_DELETED` entries — audit log is never deleted.

---

### TC-AL-014 — Entity history for a RecurringExpense includes scheduler logs
**Preconditions:** RecurringExpense `id=2` was created and has been processed by the scheduler.
**Expected:** List includes `RECURRING_EXPENSE_CREATED` and one or more `RECURRING_EXPENSE_PROCESSED` entries.
**Note:** `RECURRING_EXPENSE_PROCESSED` entries will have `ipAddress = null`.

---

### TC-AL-015 — Entity history for non-existent entity returns empty list
**Call:** `GET /api/audit-logs/entity/Expense/99999`
**Expected:** `200 OK`, `data = []`.

---

### TC-AL-016 — entityType is case-sensitive
**Call:** `GET /api/audit-logs/entity/expense/1` (lowercase `e`)
**Expected:** `200 OK`, `data = []` — entity type is stored as `"Expense"` (PascalCase) in the DB.

---

## 5. Get Logs By Action — `GET /api/audit-logs/action/{action}`

### TC-AL-017 — Filter by EXPENSE_CREATED
**Expected:** `200 OK`, all returned logs have `action = "EXPENSE_CREATED"`.

---

### TC-AL-018 — Filter by USER_LOGIN
**Expected:** All returned logs have `action = "USER_LOGIN"`, and `oldValue` and `newValue` are both `null` (login logs no snapshot data).

---

### TC-AL-019 — Filter by RECURRING_EXPENSE_PROCESSED — ipAddress is always null
**Expected:** All returned logs have `ipAddress = null` — these are generated by the `@Scheduled` job, which runs outside an HTTP context.

---

### TC-AL-020 — Invalid action enum value returns 400
**Call:** `GET /api/audit-logs/action/NOT_A_REAL_ACTION`
**Expected:** `400 Bad Request` — Spring cannot resolve the path variable to a valid `AuditAction` enum value.

---

## 6. AuditLogService — Failure Safety

### TC-AL-021 — Audit write failure does not crash the main operation
**Precondition:** Simulate `auditLogRepository.save()` throwing a `RuntimeException`.
**Expected:** The `ExpenseService.createExpense()` call completes successfully and returns the saved expense. The exception is caught and logged inside `AuditLogService.log()` — it does NOT propagate.
**Code reference:** `try { auditLogRepository.save(...) } catch (ex: Exception) { log.error(...) }`

---

### TC-AL-022 — IP address resolved from X-Forwarded-For header
**Request:** Includes `X-Forwarded-For: 203.0.113.5, 10.0.0.1`
**Expected:** `ipAddress = "203.0.113.5"` (first value in the comma-separated list).
**Code reference:** `forwarded.split(",").first().trim()`

---

### TC-AL-023 — IP address falls back to remoteAddr when X-Forwarded-For is absent
**Request:** No `X-Forwarded-For` header; `remoteAddr = "127.0.0.1"`.
**Expected:** `ipAddress = "127.0.0.1"`.

---

### TC-AL-024 — IP address is null for scheduler-triggered logs
**Trigger:** `RecurringExpenseService.processRecurringExpenses()` scheduler fires.
**Expected:** `RECURRING_EXPENSE_PROCESSED` log entry has `ipAddress = null`.
**Reason:** `RequestContextHolder.currentRequestAttributes()` throws in scheduler context; caught and returns null.

---

## 7. Audit Integration — ExpenseService

### TC-AL-025 — EXPENSE_CREATED logged after successful expense creation
**Action:** `POST /api/expenses` with valid body.
**Expected audit log:** `action=EXPENSE_CREATED`, `entityType="Expense"`, `entityId=<new expense id>`, `oldValue=null`, `newValue=<JSON snapshot of ExpenseResponse>`, `ipAddress` set.

---

### TC-AL-026 — EXPENSE_CREATED NOT logged when budget blocks the expense
**Action:** `POST /api/expenses` where budget check returns `shouldBlock=true`.
**Expected:** `BadRequestException` thrown, no expense saved, no audit log written.

---

### TC-AL-027 — EXPENSE_UPDATED logged with old and new snapshots
**Action:** `PUT /api/expenses/{id}` changing amount from `200.0` to `350.0`.
**Expected audit log:** `action=EXPENSE_UPDATED`, `oldValue` contains `"amount":200.0`, `newValue` contains `"amount":350.0`.

---

### TC-AL-028 — EXPENSE_DELETED logged with old snapshot, no newValue
**Action:** `DELETE /api/expenses/{id}`.
**Expected audit log:** `action=EXPENSE_DELETED`, `oldValue=<JSON of deleted expense>`, `newValue=null`.

---

### TC-AL-029 — No audit log written when expense not found (updateExpense returns null)
**Action:** `PUT /api/expenses/9999` for non-existent id (returns null).
**Expected:** No audit log written — `auditLogService.log()` is only called when `expenseRepository.save()` succeeds.

---

## 8. Audit Integration — BudgetService

### TC-AL-030 — BUDGET_CREATED logged on budget creation
**Expected:** `action=BUDGET_CREATED`, `entityType="Budget"`, `oldValue=null`, `newValue=<BudgetResponse JSON>`.

---

### TC-AL-031 — BUDGET_UPDATED logged with before/after snapshots
**Action:** `PUT /api/budgets/{id}` changing `amount` from `5000.0` to `8000.0`.
**Expected:** `oldValue` has `"amount":5000.0`, `newValue` has `"amount":8000.0`.

---

### TC-AL-032 — BUDGET_DELETED logged after soft delete
**Action:** `DELETE /api/budgets/{id}`.
**Expected:** `action=BUDGET_DELETED`, `oldValue=<pre-delete BudgetResponse>`, `newValue=null`.
**Note:** The entity is soft-deleted (`deletedAt` set), not removed from DB — but the audit log still captures `oldValue` before deletion.

---

## 9. Audit Integration — RecurringExpenseService

### TC-AL-033 — RECURRING_EXPENSE_CREATED logged on creation
**Expected:** `action=RECURRING_EXPENSE_CREATED`, `entityType="RecurringExpense"`, `oldValue=null`.

---

### TC-AL-034 — RECURRING_EXPENSE_UPDATED logged with snapshots
**Action:** `PUT /api/recurring-expenses/{id}` changing frequency from `MONTHLY` to `YEARLY`.
**Expected:** `oldValue` has `"frequency":"MONTHLY"`, `newValue` has `"frequency":"YEARLY"`.

---

### TC-AL-035 — RECURRING_EXPENSE_DELETED logged after soft delete
**Expected:** `action=RECURRING_EXPENSE_DELETED`, `oldValue=<pre-delete snapshot>`, `newValue=null`.

---

### TC-AL-036 — RECURRING_EXPENSE_PROCESSED logged by scheduler
**Trigger:** `processRecurringExpenses()` runs and calls `processSingleRecurringExpense()`.
**Expected:** `action=RECURRING_EXPENSE_PROCESSED`, `entityId=<recurring id>`, `newValue` contains `processedOn`, `nextDueDate`, `isStillActive`.
**Note:** `ipAddress=null` since no HTTP context exists in scheduler.

---

### TC-AL-037 — Scheduler failure on one entry does not prevent audit log for next entry
**Preconditions:** Entry A throws during `processSingleRecurringExpense`; Entry B processes normally.
**Expected:** Entry B gets `RECURRING_EXPENSE_PROCESSED` audit log; Entry A's failure is logged as error.

---

## 10. Audit Integration — AuthService

### TC-AL-038 — USER_REGISTERED logged after successful registration
**Action:** `POST /api/auth/register`.
**Expected:** `action=USER_REGISTERED`, `entityType="User"`, `entityId=<new userId>`, `newValue` contains `email` and `roles`.

---

### TC-AL-039 — USER_LOGIN logged after successful login
**Action:** `POST /api/auth/login`.
**Expected:** `action=USER_LOGIN`, `entityType="User"`, `entityId=<userId>`, `oldValue=null`, `newValue=null`.
**Note:** Login intentionally logs no snapshot data — only the fact that the user logged in and the IP.

---

### TC-AL-040 — ROLE_ASSIGNED logged after role assignment
**Action:** `POST /api/auth/assign-roles` (SUPER_ADMIN only).
**Expected:** `action=ROLE_ASSIGNED`, `userId=<adminUserId>` (the admin who performed the action), `entityId=<targetUserId>`, `newValue` contains `assignedRoles`.

---

## 11. AuditLogRepository Queries

### TC-AR-001 — `findAllByOrderByCreatedAtDesc` returns all logs newest first
**Expected:** Results ordered by `createdAt DESC` regardless of `userId` or `entityType`.

---

### TC-AR-002 — `findByUserIdOrderByCreatedAtDesc` returns only that user's logs
**Preconditions:** User 1 has 3 logs, User 2 has 5 logs.
**Call:** Query with `userId=1`.
**Expected:** Returns exactly 3 logs, all with `userId=1`.

---

### TC-AR-003 — `findByEntityTypeAndEntityIdOrderByCreatedAtDesc` returns only matching entity logs
**Preconditions:** Expense `id=5` has 2 logs; Budget `id=5` has 1 log.
**Call:** Query with `entityType="Expense"`, `entityId=5`.
**Expected:** Returns 2 logs — Budget logs not included despite same `entityId`.

---

### TC-AR-004 — `findByActionOrderByCreatedAtDesc` returns only logs with exact action
**Call:** Query with `action=BUDGET_CREATED`.
**Expected:** All returned logs have `action="BUDGET_CREATED"`.

---

## Test Data Reference

```kotlin
// Minimal audit log entry
val auditLog = AuditLog(
    userId     = 1L,
    action     = AuditAction.EXPENSE_CREATED,
    entityType = "Expense",
    entityId   = 5L,
    oldValue   = null,
    newValue   = """{"id":5,"title":"Lunch","amount":250.0}""",
    ipAddress  = "127.0.0.1"
)

// Scheduler-triggered log (no IP)
val schedulerLog = AuditLog(
    userId     = 1L,
    action     = AuditAction.RECURRING_EXPENSE_PROCESSED,
    entityType = "RecurringExpense",
    entityId   = 2L,
    newValue   = """{"processedOn":"2026-03-14","nextDueDate":"2026-04-14","isStillActive":true}""",
    ipAddress  = null
)
```

---

## Coverage Checklist

| Area | Test Cases |
|---|---|
| GET all logs (pagination, ordering, auth) | TC-AL-001 to TC-AL-005 |
| GET /me (own logs, empty, any role) | TC-AL-006 to TC-AL-008 |
| GET by userId (SUPER_ADMIN, 403, empty) | TC-AL-009 to TC-AL-011 |
| GET entity history (full history, deleted, scheduler, empty, case-sensitivity) | TC-AL-012 to TC-AL-016 |
| GET by action (filtering, null IP, invalid enum) | TC-AL-017 to TC-AL-020 |
| AuditLogService failure safety + IP resolution | TC-AL-021 to TC-AL-024 |
| ExpenseService integration | TC-AL-025 to TC-AL-029 |
| BudgetService integration | TC-AL-030 to TC-AL-032 |
| RecurringExpenseService integration + scheduler | TC-AL-033 to TC-AL-037 |
| AuthService integration | TC-AL-038 to TC-AL-040 |
| AuditLogRepository queries | TC-AR-001 to TC-AR-004 |
