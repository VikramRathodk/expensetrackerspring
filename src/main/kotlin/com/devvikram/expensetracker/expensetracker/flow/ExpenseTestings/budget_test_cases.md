# Budget Feature — Test Cases

Covers `BudgetController`, `BudgetService`, `BudgetRepository`, and `ExpenseRepository` queries.
All test cases are verified against the actual source code.

---

## API Endpoints Reference

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/budgets` | `@IsAuthenticated` | Create budget → **201 Created** |
| `GET` | `/api/budgets` | `@IsAuthenticated` | List active budgets → **200 OK** |
| `GET` | `/api/budgets/{id}/status` | `@IsAuthenticated` | Budget spend status → **200 OK** |
| `PUT` | `/api/budgets/{id}` | `@IsAuthenticated` | Update budget → **200 OK** |
| `DELETE` | `/api/budgets/{id}` | `@IsAuthenticated` | Soft delete → **200 OK** |

---

## 1. Create Budget — `POST /api/budgets`

### TC-B-001 — Create a category-scoped monthly budget (happy path)
**Request body:**
```json
{
  "categoryId": 1,
  "amount": 5000.0,
  "period": "MONTHLY",
  "startDate": "2026-03-01",
  "alertThreshold": 0.80
}
```
**Preconditions:** Category `id=1` exists.
**Expected HTTP:** `201 Created`
**Expected response:**
```json
{
  "status": true,
  "message": "Budget created successfully",
  "data": {
    "categoryId": 1,
    "amount": 5000.0,
    "period": "MONTHLY",
    "alertThreshold": 0.80,
    "isActive": true,
    "spent": 0.0,
    "remaining": 5000.0,
    "percentUsed": 0.0
  }
}
```

---

### TC-B-002 — Create an overall (no-category) budget
**Request body:**
```json
{
  "categoryId": null,
  "amount": 20000.0,
  "period": "MONTHLY",
  "startDate": "2026-03-01"
}
```
**Expected:** `201 Created`, `data.categoryId = null`, `data.categoryName = null`.

---

### TC-B-003 — Create budget with non-existent categoryId
**Input:** `categoryId = 9999` (does not exist)
**Expected:** `ResourceNotFoundException("Category with id 9999 not found")` → `404 Not Found`.

---

### TC-B-004 — `alertThreshold` defaults to `0.80` when not provided
**Input:** Request without `alertThreshold` field.
**Expected:** `data.alertThreshold = 0.80` (default from `CreateBudgetRequest`).

---

### TC-B-005 — `getSpentForPeriod` date window per period type
The `to` boundary is computed as `startDate + period`; `endDate` in the entity is user-supplied and separate.

| Period    | `startDate`    | Computed `to` (exclusive upper bound) |
|-----------|----------------|---------------------------------------|
| `DAILY`   | `2026-03-13`   | `2026-03-14`                          |
| `WEEKLY`  | `2026-03-10`   | `2026-03-17`                          |
| `MONTHLY` | `2026-03-01`   | `2026-04-01`                          |
| `YEARLY`  | `2026-01-01`   | `2027-01-01`                          |

**Expected:** Each `getSpentForPeriod` call queries expenses within `[startDate, startDate + period)`.

---

### TC-B-006 — Unauthenticated request is rejected
**Request:** `POST /api/budgets` with no `Authorization` header.
**Expected:** `401 Unauthorized` from `JwtAuthenticationEntryPoint`.

---

## 2. Get All Budgets — `GET /api/budgets`

### TC-B-007 — Returns only active, non-deleted budgets for the requesting user
**Preconditions (same user `userId=1`):**
- Budget A: `isActive=true`, `deletedAt=null`  → **included**
- Budget B: `isActive=false`, `deletedAt=null` → excluded
- Budget C: `isActive=true`, `deletedAt=now()` → excluded
- Budget D: belongs to `userId=2`              → excluded

**Expected:** `200 OK`, `data` contains only Budget A.

---

### TC-B-008 — Returns empty list when user has no active budgets
**Preconditions:** No budgets for the current user.
**Expected:** `200 OK`, `data = []`.

---

## 3. Budget Status — `GET /api/budgets/{id}/status`

### TC-B-009 — Status shows correct spend values
**Preconditions:**
- Budget: `amount=5000.0`, `period=MONTHLY`, `startDate=2026-03-01`, `alertThreshold=0.80`
- Expenses within period: `₹1000` + `₹500` → total spent = `₹1500`

**Expected (`BudgetStatusResponse`):**
```
limit        = 5000.0    ← field is named "limit", not "amount"
spent        = 1500.0
remaining    = 3500.0
percentUsed  = 30.0      ← (1500 / 5000) * 100
isOverBudget = false     ← 1500 < 5000
isNearLimit  = false     ← 30.0 < (0.80 * 100 = 80.0)
```

---

### TC-B-010 — `isNearLimit = true` when `percentUsed >= alertThreshold * 100`
**Code:** `isNearLimit = percentUsed >= budget.alertThreshold * 100`
**Preconditions:** `amount=5000.0`, `alertThreshold=0.80`, `spent=4100.0`
- `percentUsed = (4100 / 5000) * 100 = 82.0`
- `82.0 >= 80.0` → `isNearLimit = true`

**Expected:** `isNearLimit = true`, `isOverBudget = false`.

---

### TC-B-011 — `isOverBudget = true` and `isNearLimit = true` when `spent >= amount`
**Code:** `isOverBudget = spent >= budget.amount`
**Preconditions:** `amount=5000.0`, `spent=5000.0`
- `5000.0 >= 5000.0` → `isOverBudget = true`
- `percentUsed = (5000 / 5000) * 100 = 100.0`
- `100.0 >= 80.0` → `isNearLimit = true` as well

**Expected:** Both `isOverBudget = true` AND `isNearLimit = true`.

---

### TC-B-012 — Budget belonging to a different user returns not found
**Preconditions:** Budget `id=5` belongs to `userId=2`.
**Call:** `GET /api/budgets/5/status` authenticated as `userId=1`
**Expected:** `ResourceNotFoundException` → `404 Not Found`.

---

### TC-B-013 — Soft-deleted budget returns not found
**Preconditions:** Budget `id=5`, `deletedAt` is set (soft deleted).
**Expected:** `ResourceNotFoundException` → `404 Not Found`.
**Reason:** `findByIdAndUserIdAndDeletedAtIsNull` filters out records where `deletedAt IS NOT NULL`.

---

## 4. Update Budget — `PUT /api/budgets/{id}`

### TC-B-014 — Partial update — only `amount` changes
**Request body:** `{ "amount": 8000.0 }`
**Expected:** `amount` updated to `8000.0`; `alertThreshold`, `endDate`, `isActive` retain their original values.
**Reason:** `UpdateBudgetRequest` fields are all nullable; service uses `?: budget.field` fallback.

---

### TC-B-015 — Partial update — only `alertThreshold` changes
**Request body:** `{ "alertThreshold": 0.90 }`
**Expected:** `alertThreshold = 0.90`; all other fields unchanged.

---

### TC-B-016 — Deactivate a budget
**Request body:** `{ "isActive": false }`
**Expected:** Budget saved with `isActive=false`; no longer returned by `GET /api/budgets`.

---

### TC-B-017 — Update non-existent budget
**Call:** `PUT /api/budgets/9999` with any body.
**Expected:** `ResourceNotFoundException` → `404 Not Found`.

---

### TC-B-018 — Cannot update another user's budget
**Preconditions:** Budget `id=5` belongs to `userId=2`.
**Call:** Authenticated as `userId=1`, `PUT /api/budgets/5`
**Expected:** `ResourceNotFoundException` → `404 Not Found`.

---

## 5. Delete Budget — `DELETE /api/budgets/{id}`

### TC-B-019 — Soft delete sets `deletedAt`
**Expected HTTP:** `200 OK`
**Expected response:** `{ "status": true, "message": "Budget deleted successfully", "data": null }`
**Side effects:** `Budget.deletedAt` is set to `LocalDateTime.now()`; budget no longer appears in `getAllBudgets` or `getBudgetStatus`.

---

### TC-B-020 — Delete non-existent budget
**Expected:** `ResourceNotFoundException` → `404 Not Found`.

---

### TC-B-021 — Delete already-deleted budget
**Preconditions:** Budget `deletedAt` is already set.
**Expected:** `ResourceNotFoundException` — `findByIdAndUserIdAndDeletedAtIsNull` returns `null` for records with a non-null `deletedAt`.

---

## 6. Budget Check on Expense — `checkBudgetOnExpense(userId, categoryId, expenseAmount)`

> Called from `ExpenseService.createExpense()` **before** saving the expense.
> Returns `BudgetCheckResult(shouldBlock: Boolean, warnings: List<String>)`.

**Warning message formats (exact from code):**
- Over budget: `"This expense exceeds your {label} budget of ₹{amount}."`
- Near limit: `"You've used {X}% of your {label} budget of ₹{amount}."`
- `label` = `"category"` when budget has a category, `"overall"` otherwise.
- `percentUsed` inside this method is a **ratio (0–1)**, multiplied by 100 only in the warning string.

---

### TC-B-022 — No active budgets → no block, no warnings
**Preconditions:** User has no active budgets.
**Expected:** `BudgetCheckResult(shouldBlock=false, warnings=[])`.

---

### TC-B-023 — Expense stays well within budget → no warning
**Preconditions:** Category budget `amount=5000.0`, `alertThreshold=0.80`, current `spent=1000.0`
**New expense:** `500.0` → `newSpent=1500.0`, `ratio=0.30`
`0.30 >= 0.80` → false → no warning.
**Expected:** `shouldBlock=false`, `warnings=[]`.

---

### TC-B-024 — Expense crosses `alertThreshold` → warning, no block
**Preconditions:** Category budget `amount=5000.0`, `alertThreshold=0.80`, `spent=3800.0`
**New expense:** `500.0` → `newSpent=4300.0`, `ratio = 4300/5000 = 0.86`
- `newSpent (4300) >= amount (5000)` → false → no block
- `ratio (0.86) >= alertThreshold (0.80)` → true → warning

**Expected:** `shouldBlock=false`, `warnings=["You've used 86% of your category budget of ₹5000.0."]`.

---

### TC-B-025 — Expense meets or exceeds budget → block
**Preconditions:** Category budget `amount=5000.0`, `spent=4800.0`
**New expense:** `300.0` → `newSpent=5100.0`
- `5100.0 >= 5000.0` → true → block

**Expected:** `shouldBlock=true`, `warnings=["This expense exceeds your category budget of ₹5000.0."]`.

---

### TC-B-026 — Exact budget limit → block (boundary condition)
**Preconditions:** `amount=5000.0`, `spent=4700.0`
**New expense:** `300.0` → `newSpent=5000.0`
- `5000.0 >= 5000.0` → true → block (uses `>=`, not `>`)

**Expected:** `shouldBlock=true`.

---

### TC-B-027 — Both category and overall budgets evaluated; one blocks
**Preconditions:**
- Category budget: `amount=2000.0`, `spent=1800.0`; expense `300.0` → `newSpent=2100.0 >= 2000.0` → **blocks**
- Overall budget: `amount=10000.0`, `spent=7000.0`; expense `300.0` → `ratio=0.73 < 0.80` → no warning

**Expected:** `shouldBlock=true`, `warnings` has 1 entry (category exceeded only).

---

### TC-B-028 — Overall budget triggers block even if category budget is fine
**Preconditions:**
- Category budget: `amount=5000.0`, `spent=500.0` → well within limit
- Overall budget: `amount=1000.0`, `spent=900.0`; expense `200.0` → `newSpent=1100.0 >= 1000.0` → **blocks**

**Expected:** `shouldBlock=true`, warning references `"overall budget"`.

---

### TC-B-029 — `expenseAmount=0.0` when no budget is over-limit → no warning
**Preconditions:** No existing budget has `currentSpent >= amount`.
**New expense:** `0.0` → `newSpent = currentSpent + 0 = currentSpent`
Since `currentSpent < amount`, neither condition triggers.
**Expected:** `shouldBlock=false`, `warnings=[]`.

> **Note:** If a budget is already at `currentSpent >= amount`, adding `0.0` still triggers a block because `newSpent >= amount` evaluates to `true`.

---

## 7. ExpenseRepository Queries

### TC-R-001 — `sumAmountByUserIdAndCategoryIdAndDateBetween` — correct sum within window
**Preconditions:** `userId=1`, `categoryId=2`, expenses `₹200`, `₹300`, `₹500` all with `createdAt` in `[2026-03-01, 2026-04-01)`.
**Expected:** `1000.0`

---

### TC-R-002 — Excludes expenses from other users
**Preconditions:** Same category and date window but belonging to `userId=2`.
**Expected:** `0.0` (COALESCE returns 0.0 when SUM is over empty set).

---

### TC-R-003 — Excludes expenses from other categories
**Preconditions:** `userId=1`, but `categoryId=99` (different category).
**Expected:** `0.0`.

---

### TC-R-004 — Excludes expenses outside the date window (before `from`)
**Preconditions:** Expenses for correct `userId`/`categoryId` but `createdAt=2026-02-15` (before `from=2026-03-01`).
**Expected:** `0.0`.

---

### TC-R-005 — Excludes expenses at or after the `to` boundary (exclusive upper bound)
**Preconditions:** Expense with `createdAt=2026-04-01T00:00:00` and `to=2026-04-01`.
**Expected:** `0.0` — query uses `CAST(createdAt AS LocalDate) < :to` (strictly less than).

---

### TC-R-006 — `sumAmountByUserIdAndDateBetween` — sums across all categories
**Preconditions:** `userId=1`, category A `₹1000` + category B `₹2000`, both within window.
**Expected:** `3000.0`.

---

### TC-R-007 — Returns `0.0` (not null) when no matching expenses
**Preconditions:** User has no expenses in the given window.
**Expected:** Query returns `0.0` directly due to `COALESCE(SUM(...), 0.0)`. The `?: 0.0` in `getSpentForPeriod` is a safety net but the DB already returns `0.0`.

---

## 8. BudgetRepository Queries

### TC-BR-001 — `findAllActiveBudgets` excludes budgets whose `endDate` has passed
**Preconditions:** Budget with `isActive=true`, `deletedAt=null`, `startDate=2026-01-01`, `endDate=2026-02-28`, `today=2026-03-13`.
- `b.endDate (2026-02-28) >= today (2026-03-13)` → false → excluded

**Expected:** Budget not returned by `findAllActiveBudgets`.

---

### TC-BR-002 — `findAllActiveBudgets` includes budgets with `endDate=null`
**Preconditions:** Budget with `endDate=null` and `startDate <= today`.
- Condition: `(b.endDate IS NULL OR b.endDate >= today)` → `null` branch → included

**Expected:** Budget returned.

---

### TC-BR-003 — `findAllActiveBudgets` excludes budgets with future `startDate`
**Preconditions:** Budget with `startDate=2026-04-01`, `today=2026-03-13`.
- `b.startDate (2026-04-01) <= today (2026-03-13)` → false → excluded

**Expected:** Budget not returned.

---

## 9. Scheduled Reset — `resetPeriodicBudgets()` (runs at midnight daily)

### TC-S-001 — Budget whose period window has elapsed gets `startDate` advanced to today
**Code:**
```kotlin
val nextPeriodStart = budget.startDate.plusDays(1)  // for DAILY
if (!today.isBefore(nextPeriodStart)) {
    budgetRepository.save(budget.copy(startDate = today))
}
```
**Preconditions:** `period=DAILY`, `startDate=2026-03-12`, `today=2026-03-13`
- `nextPeriodStart = 2026-03-13`
- `!today.isBefore(2026-03-13)` = `!(false)` = `true` → reset fires

**Expected:** `startDate` updated to `2026-03-13` (today).

---

### TC-S-002 — Budget still within its period window is NOT reset
**Preconditions:** `period=MONTHLY`, `startDate=2026-03-01`, `today=2026-03-13`
- `nextPeriodStart = 2026-04-01`
- `!today.isBefore(2026-04-01)` = `!(true)` = `false` → reset skipped

**Expected:** `startDate` unchanged at `2026-03-01`.

---

### TC-S-003 — Inactive or deleted budgets are excluded from reset
**Reason:** `findAllActiveBudgets` filters `isActive=true AND deletedAt IS NULL`.
**Expected:** Inactive/deleted budgets are never evaluated or saved.

---

### TC-S-004 — Budget with expired `endDate` is excluded from reset
**Preconditions:** Budget `endDate=2026-02-28`, `today=2026-03-13`.
**Reason:** `findAllActiveBudgets` requires `endDate IS NULL OR endDate >= today`.
**Expected:** Budget not included in the reset loop.

---

## 10. Edge Cases

### TC-E-001 — `amount=0.0` does not cause division by zero
**Relevant code (both `getBudgetStatus` and `toResponse`):**
```kotlin
val percentUsed = if (budget.amount > 0) (spent / budget.amount) * 100 else 0.0
```
**Expected:** `percentUsed = 0.0`; no `ArithmeticException`.

---

### TC-E-002 — `remaining` is floored at `0.0`, never negative
**Relevant code:**
```kotlin
val remaining = (budget.amount - spent).coerceAtLeast(0.0)
```
**Preconditions:** `amount=5000.0`, `spent=5500.0` (over-budget)
**Expected:** `remaining = 0.0`, not `-500.0`.

---

### TC-E-003 — `checkBudgetOnExpense` with `amount=0.0` budget (div-by-zero guard)
**Relevant code:**
```kotlin
val percentUsed = if (budget.amount > 0) newSpent / budget.amount else 0.0
```
**Preconditions:** Budget with `amount=0.0`.
**Expected:** `percentUsed = 0.0`; no exception. Alert threshold check `0.0 >= 0.80` → false → no warning.
Block check: `newSpent >= 0.0` → **always true for any positive expense** → `shouldBlock=true`.

---

## Test Data Reference

```kotlin
// Minimal valid CreateBudgetRequest — category-scoped
val monthlyBudget = CreateBudgetRequest(
    categoryId     = 1L,
    amount         = 5000.0,
    period         = BudgetPeriod.MONTHLY,
    startDate      = LocalDate.of(2026, 3, 1),
    alertThreshold = 0.80
)

// Overall budget (no category)
val overallBudget = CreateBudgetRequest(
    categoryId = null,
    amount     = 20000.0,
    period     = BudgetPeriod.MONTHLY,
    startDate  = LocalDate.of(2026, 3, 1)
    // alertThreshold defaults to 0.80
)

// Partial update — only amount
val updateAmount = UpdateBudgetRequest(amount = 8000.0)

// Deactivate
val deactivate = UpdateBudgetRequest(isActive = false)
```

---

## Coverage Checklist

| Area | Test Cases |
|---|---|
| Create budget (category + overall + validation) | TC-B-001 to TC-B-006 |
| Get all budgets (active/inactive/deleted/cross-user) | TC-B-007, TC-B-008 |
| Budget status (spend, threshold, over-budget, auth) | TC-B-009 to TC-B-013 |
| Update (partial fields, ownership, not found) | TC-B-014 to TC-B-018 |
| Soft delete (deletedAt, re-delete, not found) | TC-B-019 to TC-B-021 |
| Budget check on expense (block, warn, boundary) | TC-B-022 to TC-B-029 |
| ExpenseRepository sum queries | TC-R-001 to TC-R-007 |
| BudgetRepository `findAllActiveBudgets` | TC-BR-001 to TC-BR-003 |
| Scheduled reset (advance, skip, exclude) | TC-S-001 to TC-S-004 |
| Edge cases (div-by-zero, coerce, zero budget) | TC-E-001 to TC-E-003 |