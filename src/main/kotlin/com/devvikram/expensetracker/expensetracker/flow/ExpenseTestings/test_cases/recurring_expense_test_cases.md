# Recurring Expense Feature — Test Cases

Covers `RecurringExpenseController`, `RecurringExpenseService`, `RecurringExpenseRepository`, and the daily scheduler.
All test cases are verified against the actual source code.

---

## API Endpoints Reference

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/recurring-expenses` | `@IsAuthenticated` | Create recurring expense → **201 Created** |
| `GET` | `/api/recurring-expenses` | `@IsAuthenticated` | List active recurring expenses → **200 OK** |
| `GET` | `/api/recurring-expenses/{id}` | `@IsAuthenticated` | Get by ID → **200 OK** |
| `PUT` | `/api/recurring-expenses/{id}` | `@IsAuthenticated` | Update → **200 OK** |
| `DELETE` | `/api/recurring-expenses/{id}` | `@IsAuthenticated` | Soft delete → **200 OK** |

**Frequency enum values:** `DAILY` | `WEEKLY` | `MONTHLY` | `YEARLY`

---

## 1. Create Recurring Expense — `POST /api/recurring-expenses`

### TC-RE-001 — Create a monthly recurring expense with endDate (happy path)
**Request body:**
```json
{
  "title": "Netflix Subscription",
  "amount": 499.0,
  "categoryId": 1,
  "frequency": "MONTHLY",
  "startDate": "2026-03-15",
  "endDate": "2026-12-31",
  "note": "Monthly streaming subscription"
}
```
**Preconditions:** Category `id=1` exists.
**Expected HTTP:** `201 Created`
**Expected response:**
```json
{
  "status": true,
  "message": "Recurring expense created successfully",
  "data": {
    "title": "Netflix Subscription",
    "amount": 499.0,
    "frequency": "MONTHLY",
    "nextDueDate": "2026-03-15",
    "endDate": "2026-12-31",
    "isActive": true,
    "note": "Monthly streaming subscription"
  }
}
```
**Note:** `startDate` in the request maps to `nextDueDate` in the entity (`nextDueDate = request.startDate`).

---

### TC-RE-002 — Create a recurring expense with no endDate (runs forever)
**Request body:**
```json
{
  "title": "Gym Membership",
  "amount": 1200.0,
  "categoryId": 2,
  "frequency": "MONTHLY",
  "startDate": "2026-04-01"
}
```
**Expected:** `201 Created`, `data.endDate = null`, `data.isActive = true`.
**Note:** `endDate` is optional (`val endDate: LocalDate? = null`). Null means the scheduler runs it indefinitely.

---

### TC-RE-003 — Create recurring expense with non-existent categoryId
**Input:** `categoryId = 9999` (does not exist)
**Expected:** `ResourceNotFoundException("Category with id 9999 not found")` → `404 Not Found`.

---

### TC-RE-004 — Create recurring expense with DAILY frequency
**Input:** `frequency = "DAILY"`, `startDate = "2026-03-15"`
**Expected:** `201 Created`, `data.frequency = "DAILY"`.
**Side effect:** Scheduler will advance `nextDueDate` by `plusDays(1)` each time it fires.

---

### TC-RE-005 — Create recurring expense with WEEKLY frequency
**Input:** `frequency = "WEEKLY"`, `startDate = "2026-03-17"`
**Expected:** `201 Created`, `data.frequency = "WEEKLY"`.
**Side effect:** Scheduler will advance `nextDueDate` by `plusWeeks(1)` each time it fires.

---

### TC-RE-006 — Create recurring expense with YEARLY frequency
**Input:** `frequency = "YEARLY"`, `startDate = "2026-01-01"`, `endDate = "2030-01-01"`
**Expected:** `201 Created`, `data.frequency = "YEARLY"`.
**Side effect:** Scheduler will advance `nextDueDate` by `plusYears(1)` each time it fires.

---

### TC-RE-007 — Unauthenticated request is rejected
**Request:** `POST /api/recurring-expenses` with no `Authorization` header.
**Expected:** `401 Unauthorized` from `JwtAuthenticationEntryPoint`.

---

## 2. Get All Recurring Expenses — `GET /api/recurring-expenses`

### TC-RE-008 — Returns only active, non-deleted recurring expenses for the requesting user
**Preconditions (same user `userId=1`):**
- Entry A: `isActive=true`, `deletedAt=null`   → **included**
- Entry B: `isActive=false`, `deletedAt=null`  → excluded (inactive)
- Entry C: `isActive=true`, `deletedAt=now()`  → excluded (soft-deleted)
- Entry D: belongs to `userId=2`               → excluded (wrong user)

**Repository method:** `findByUserIdAndIsActiveTrueAndDeletedAtIsNull(userId)`
**Expected:** `200 OK`, `data` contains only Entry A.

---

### TC-RE-009 — Returns empty list when user has no active recurring expenses
**Preconditions:** No recurring expenses for the current user.
**Expected:** `200 OK`, `data = []`.

---

## 3. Get Recurring Expense By ID — `GET /api/recurring-expenses/{id}`

### TC-RE-010 — Fetch a specific recurring expense by ID (happy path)
**Preconditions:** Recurring expense `id=1` exists for the authenticated user.
**Expected:** `200 OK`, all fields populated (`id`, `title`, `amount`, `categoryId`, `categoryName`, `frequency`, `nextDueDate`, `endDate`, `isActive`, `note`, `createdAt`).

---

### TC-RE-011 — Non-existent ID returns 404
**Call:** `GET /api/recurring-expenses/9999` where `id=9999` does not exist.
**Expected:** `ResourceNotFoundException("Recurring expense with id 9999 not found")` → `404 Not Found`.

---

### TC-RE-012 — Soft-deleted entry returns 404
**Preconditions:** Recurring expense `id=5` has `deletedAt` set.
**Expected:** `404 Not Found`.
**Reason:** `findByIdAndUserIdAndDeletedAtIsNull` filters out records where `deletedAt IS NOT NULL`.

---

### TC-RE-013 — Entry belonging to another user returns 404
**Preconditions:** Recurring expense `id=5` belongs to `userId=2`.
**Call:** `GET /api/recurring-expenses/5` authenticated as `userId=1`.
**Expected:** `404 Not Found`.
**Reason:** Repository method includes `userId` in the lookup — ownership-safe by design.

---

## 4. Update Recurring Expense — `PUT /api/recurring-expenses/{id}`

### TC-RE-014 — Partial update — only `amount` changes
**Request body:** `{ "amount": 599.0 }`
**Expected:** `amount` updated to `599.0`; `title`, `frequency`, `nextDueDate`, `endDate`, `isActive`, `note` retain original values.
**Reason:** `UpdateRecurringExpenseRequest` fields are all nullable; service uses `?: recurring.field` fallback.

---

### TC-RE-015 — Partial update — only `frequency` changes
**Request body:** `{ "frequency": "YEARLY" }`
**Expected:** `frequency = "YEARLY"`; all other fields unchanged.

---

### TC-RE-016 — Manual override of `nextDueDate`
**Request body:** `{ "nextDueDate": "2026-05-01" }`
**Expected:** `nextDueDate = "2026-05-01"`; the scheduler will use this date on its next run.
**Use case:** Skip a billing cycle or fix a missed due date.

---

### TC-RE-017 — Deactivate a recurring expense
**Request body:** `{ "isActive": false }`
**Expected:** `isActive = false`; entry no longer returned by `GET /api/recurring-expenses`; scheduler skips it.

---

### TC-RE-018 — Reactivate a recurring expense
**Preconditions:** Entry has `isActive=false`.
**Request body:** `{ "isActive": true }`
**Expected:** `isActive = true`; entry appears again in `GET /api/recurring-expenses`; scheduler resumes processing it.

---

### TC-RE-019 — Set endDate on an existing entry
**Request body:** `{ "endDate": "2026-09-30" }`
**Expected:** `endDate = "2026-09-30"`; the scheduler will auto-deactivate the entry once `nextDueDate > endDate`.

---

### TC-RE-020 — Update categoryId to a valid different category
**Request body:** `{ "categoryId": 3 }`
**Preconditions:** Category `id=3` exists.
**Expected:** `categoryId = 3`, `categoryName` updated to match the new category.

---

### TC-RE-021 — Update categoryId to a non-existent category
**Request body:** `{ "categoryId": 9999 }`
**Expected:** `ResourceNotFoundException("Category with id 9999 not found")` → `404 Not Found`.

---

### TC-RE-022 — Update non-existent recurring expense
**Call:** `PUT /api/recurring-expenses/9999` with any body.
**Expected:** `ResourceNotFoundException` → `404 Not Found`.

---

### TC-RE-023 — Cannot update another user's recurring expense
**Preconditions:** Recurring expense `id=5` belongs to `userId=2`.
**Call:** Authenticated as `userId=1`, `PUT /api/recurring-expenses/5`
**Expected:** `ResourceNotFoundException` → `404 Not Found`.

---

## 5. Delete Recurring Expense — `DELETE /api/recurring-expenses/{id}`

### TC-RE-024 — Soft delete sets `deletedAt`
**Expected HTTP:** `200 OK`
**Expected response:** `{ "status": true, "message": "Recurring expense deleted successfully", "data": null }`
**Side effects:**
- `RecurringExpense.deletedAt` is set to `LocalDateTime.now()`.
- Entry no longer appears in `GET /api/recurring-expenses`.
- `GET /api/recurring-expenses/{id}` returns `404`.
- The daily scheduler skips it (`deletedAt IS NULL` filter).

---

### TC-RE-025 — Delete non-existent recurring expense
**Expected:** `ResourceNotFoundException` → `404 Not Found`.

---

### TC-RE-026 — Delete already-deleted recurring expense
**Preconditions:** Recurring expense `deletedAt` is already set.
**Expected:** `ResourceNotFoundException` — `findByIdAndUserIdAndDeletedAtIsNull` returns `null` for records with non-null `deletedAt`.

---

## 6. Daily Scheduler — `processRecurringExpenses()` (runs at 00:05 daily)

> Cron: `0 5 0 * * *` — fires 5 minutes after midnight, after budget reset at `00:00`.
> Finds all entries due today via `findAllDueToday(today)` and calls `processSingleRecurringExpense` for each.
> Each entry is processed independently — one failure does NOT stop the rest.

### TC-SCH-001 — Entry due today is processed: expense created and nextDueDate advanced
**Preconditions:** Recurring expense `frequency=MONTHLY`, `nextDueDate=2026-03-14`, `today=2026-03-14`
**Expected:**
1. `expenseService.createExpense(...)` is called with `title`, `amount`, `categoryId`, `userId` from the recurring entry.
2. `note` on the new expense = `"[Auto] {note}"` (or `"[Auto] {title}"` if note is null).
3. `nextDueDate` advanced to `2026-04-14` (`today.plusMonths(1)`).
4. `isActive` remains `true` (endDate is null or `2026-04-14 <= endDate`).

---

### TC-SCH-002 — nextDueDate advancement per frequency
| Frequency | `today` | New `nextDueDate` |
|-----------|---------|-------------------|
| `DAILY`   | `2026-03-14` | `2026-03-15` (`plusDays(1)`) |
| `WEEKLY`  | `2026-03-14` | `2026-03-21` (`plusWeeks(1)`) |
| `MONTHLY` | `2026-03-14` | `2026-04-14` (`plusMonths(1)`) |
| `YEARLY`  | `2026-03-14` | `2027-03-14` (`plusYears(1)`) |

---

### TC-SCH-003 — Entry is auto-deactivated when nextDueDate passes endDate
**Preconditions:** `endDate=2026-03-31`, `today=2026-03-14`, `frequency=MONTHLY`
**After processing:** `nextDueDate = 2026-04-14`
**Check:** `isStillActive = endDate == null || nextDueDate <= endDate` → `2026-04-14 <= 2026-03-31` → `false`
**Expected:** `isActive = false` after save.

---

### TC-SCH-004 — Entry stays active when nextDueDate is still within endDate
**Preconditions:** `endDate=2026-12-31`, `today=2026-03-14`, `frequency=MONTHLY`
**After processing:** `nextDueDate = 2026-04-14`
**Check:** `2026-04-14 <= 2026-12-31` → `true`
**Expected:** `isActive = true` after save.

---

### TC-SCH-005 — Entry with `endDate=null` always stays active after processing
**Preconditions:** `endDate=null`, `frequency=YEARLY`
**Check:** `endDate == null` → short-circuit to `true`
**Expected:** `isActive = true` after every scheduler run.

---

### TC-SCH-006 — Inactive entries are excluded from scheduler
**Preconditions:** Recurring expense with `isActive=false`, `nextDueDate <= today`.
**Repository query:** `findAllDueToday` filters `r.isActive = true` → entry not returned.
**Expected:** No expense created, `nextDueDate` unchanged.

---

### TC-SCH-007 — Soft-deleted entries are excluded from scheduler
**Preconditions:** Recurring expense with `deletedAt` set, `nextDueDate <= today`.
**Repository query:** `findAllDueToday` filters `r.deletedAt IS NULL` → entry not returned.
**Expected:** No expense created.

---

### TC-SCH-008 — Entries with future nextDueDate are not processed
**Preconditions:** `nextDueDate = 2026-03-20`, `today = 2026-03-14`
**Repository query:** `r.nextDueDate <= :today` → `2026-03-20 <= 2026-03-14` → false → not returned.
**Expected:** Not processed.

---

### TC-SCH-009 — Entries past their endDate are excluded from scheduler
**Preconditions:** `nextDueDate=2026-03-14`, `endDate=2026-03-10`, `today=2026-03-14`
**Repository query condition:** `r.endDate IS NULL OR r.endDate >= :today` → `2026-03-10 >= 2026-03-14` → false → excluded.
**Expected:** Entry not processed by scheduler.

---

### TC-SCH-010 — One entry failure does not stop other entries
**Preconditions:** Entry A raises an exception during `processSingleRecurringExpense`; Entry B is valid.
**Expected:** Exception for Entry A is caught and logged (`log.error(...)`); Entry B is processed normally.
**Code reference:** `try { processSingleRecurringExpense(...) } catch (ex: Exception) { log.error(...) }`

---

### TC-SCH-011 — Auto-created expense triggers budget check
**Preconditions:** Recurring expense `amount=3000.0`, `categoryId=1`. Active category budget `amount=2000.0`, `spent=1900.0`.
**Expected:** `expenseService.createExpense(...)` is called, which calls `budgetService.checkBudgetOnExpense(...)`.
**Note:** Whether the expense is blocked depends on the budget check result — the behavior is inherited from `ExpenseService`, not re-implemented in `RecurringExpenseService`.

---

### TC-SCH-012 — Auto-expense note format
**Code:** `note = "[Auto] ${recurring.note ?: recurring.title}"`

| `recurring.note` | Auto-expense note |
|---|---|
| `"Monthly streaming"` | `"[Auto] Monthly streaming"` |
| `null` | `"[Auto] Netflix Subscription"` (falls back to title) |

---

## 7. RecurringExpenseRepository Queries

### TC-RR-001 — `findAllDueToday` — entry due exactly today is included
**Preconditions:** `nextDueDate = today`, `isActive=true`, `deletedAt=null`, `endDate=null`
**Expected:** Entry returned.

---

### TC-RR-002 — `findAllDueToday` — overdue entry (nextDueDate in the past) is included
**Preconditions:** `nextDueDate = today - 2 days`, `isActive=true`, `deletedAt=null`
**Expected:** Entry returned (`nextDueDate <= today` is satisfied).
**Note:** Overdue entries accumulate and are processed in a single batch — they do NOT create multiple expenses for missed days.

---

### TC-RR-003 — `findAllDueToday` — entry with future nextDueDate is excluded
**Preconditions:** `nextDueDate = today + 1 day`
**Expected:** Entry NOT returned.

---

### TC-RR-004 — `findAllDueToday` — entry with expired endDate is excluded
**Preconditions:** `nextDueDate = today`, `endDate = today - 1 day`
**Query condition:** `r.endDate IS NULL OR r.endDate >= :today` → `(today-1) >= today` → false → excluded.
**Expected:** Entry NOT returned.

---

### TC-RR-005 — `findAllDueToday` — entry with `endDate = today` is included
**Preconditions:** `nextDueDate = today`, `endDate = today`
**Query condition:** `r.endDate >= :today` → `today >= today` → true → included.
**Expected:** Entry returned (last day of billing is still processed).

---

### TC-RR-006 — `findByUserIdAndIsActiveTrueAndDeletedAtIsNull` — cross-user isolation
**Preconditions:** `userId=1` has 2 active entries; `userId=2` has 3 active entries.
**Call:** Query with `userId=1`.
**Expected:** Returns exactly 2 entries belonging to `userId=1`.

---

### TC-RR-007 — `findByIdAndUserIdAndDeletedAtIsNull` — returns null for wrong userId
**Preconditions:** Entry `id=5` belongs to `userId=2`.
**Call:** `findByIdAndUserIdAndDeletedAtIsNull(5, 1)`
**Expected:** Returns `null`.

---

## 8. Edge Cases

### TC-E-001 — `amount=0.0` recurring expense is valid
**Input:** `amount=0.0`
**Expected:** `201 Created`. The scheduler will create a zero-amount expense which passes through `ExpenseService`.

---

### TC-E-002 — `startDate` in the past is accepted
**Input:** `startDate = "2026-01-01"`, `today = "2026-03-14"`
**Expected:** `201 Created`. `nextDueDate` is set to `"2026-01-01"`. On the next scheduler run the entry is immediately processed (overdue).

---

### TC-E-003 — `endDate` before `startDate` is a logical error (no server-side guard)
**Input:** `startDate = "2026-06-01"`, `endDate = "2026-03-01"`
**Expected:** `201 Created` (no validation guard in service). The scheduler query `r.endDate >= :today` will immediately exclude this entry since `endDate` is already in the past.

---

### TC-E-004 — Note is null when not provided
**Input:** Request without `note` field.
**Expected:** `data.note = null`; auto-expense note = `"[Auto] {title}"`.

---

## Test Data Reference

```kotlin
// Minimal valid CreateRecurringExpenseRequest
val monthlyRecurring = CreateRecurringExpenseRequest(
    title      = "Netflix Subscription",
    amount     = 499.0,
    categoryId = 1L,
    frequency  = RecurringFrequency.MONTHLY,
    startDate  = LocalDate.of(2026, 3, 15)
    // endDate defaults to null (runs forever)
    // note defaults to null
)

// With endDate and note
val timedRecurring = CreateRecurringExpenseRequest(
    title      = "Netflix Subscription",
    amount     = 499.0,
    categoryId = 1L,
    frequency  = RecurringFrequency.MONTHLY,
    startDate  = LocalDate.of(2026, 3, 15),
    endDate    = LocalDate.of(2026, 12, 31),
    note       = "Monthly streaming subscription"
)

// Partial update — amount only
val updateAmount = UpdateRecurringExpenseRequest(amount = 599.0)

// Deactivate
val deactivate = UpdateRecurringExpenseRequest(isActive = false)

// Manual nextDueDate override
val skipCycle = UpdateRecurringExpenseRequest(nextDueDate = LocalDate.of(2026, 5, 1))
```

---

## Coverage Checklist

| Area | Test Cases |
|---|---|
| Create (all frequencies, endDate, no endDate, validation) | TC-RE-001 to TC-RE-007 |
| Get all (active/inactive/deleted/cross-user, empty list) | TC-RE-008, TC-RE-009 |
| Get by ID (found, not found, deleted, cross-user) | TC-RE-010 to TC-RE-013 |
| Update (partial fields, ownership, nextDueDate override, category change) | TC-RE-014 to TC-RE-023 |
| Soft delete (deletedAt, re-delete, not found) | TC-RE-024 to TC-RE-026 |
| Scheduler (processing, frequency, deactivation, exclusion, failure isolation) | TC-SCH-001 to TC-SCH-012 |
| RecurringExpenseRepository queries | TC-RR-001 to TC-RR-007 |
| Edge cases (zero amount, past startDate, endDate < startDate, null note) | TC-E-001 to TC-E-004 |
