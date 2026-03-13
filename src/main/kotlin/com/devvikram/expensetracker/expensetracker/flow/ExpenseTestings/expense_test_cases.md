# Expense Feature — Test Cases

Covers `ExpenseController`, `ExpenseService`, `ExpenseSpecifications`, and `ExpenseRepository`.
All test cases are verified against the actual source code.

---

## API Endpoints Reference

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/expenses` | `@IsAuthenticated` | Create expense → **201 Created** |
| `GET` | `/api/expenses` | `@IsAuthenticated` | List paginated expenses → **200 OK** |
| `GET` | `/api/expenses/{id}` | `@IsAuthenticated` | Get single expense → **200 OK / 404** |
| `PUT` | `/api/expenses/{id}` | `@IsAuthenticated` | Update expense → **200 OK / 404** |
| `DELETE` | `/api/expenses/{id}` | `@IsAuthenticated` | Delete expense → **200 OK / 404** |
| `GET` | `/api/expenses/search` | `@IsAuthenticated` | Search by keyword → **200 OK** |
| `GET` | `/api/expenses/filter/category` | `@IsAuthenticated` | Filter by category → **200 OK** |
| `GET` | `/api/expenses/filter/amount` | `@IsAuthenticated` | Filter by amount range → **200 OK** |
| `GET` | `/api/expenses/filter/date-range` | `@IsAuthenticated` | Filter by date range → **200 OK** |
| `POST` | `/api/expenses/filter` | `@IsAuthenticated` | Advanced filter (paginated) → **200 OK** |

> **Auth note:** `userId` is always extracted from the JWT via `getUserIdFromAuth(userDetails)` in the controller. Any `userId` in the request body is overwritten with the authenticated user's id: `request.copy(userId = userId)`.

---

## 1. Create Expense — `POST /api/expenses`

### TC-E-001 — Create expense (happy path)
**Request body:**
```json
{
  "title": "Groceries",
  "amount": 500.0,
  "categoryId": 1,
  "userId": 0,
  "note": "Weekly shopping"
}
```
**Preconditions:** Category `id=1` exists. No active budget is exceeded.
**Expected HTTP:** `201 Created`
**Expected response:**
```json
{
  "status": true,
  "message": "Expense created successfully",
  "data": {
    "title": "Groceries",
    "amount": 500.0,
    "categoryId": 1,
    "categoryName": "Food",
    "note": "Weekly shopping"
  }
}
```
**Note:** The `userId` in the body is ignored — the controller always overwrites it from the JWT.

---

### TC-E-002 — `userId` in body is overwritten by authenticated user's id
**Scenario:** Client sends `"userId": 99` in the request body while authenticated as `userId=1`.
**Expected:** Expense is saved with `userId=1` (from JWT), not `99`.
**Reason:** Controller does `request.copy(userId = userId)` before calling service.

---

### TC-E-003 — Create expense with non-existent categoryId
**Input:** `categoryId = 9999` (does not exist)
**Expected:** `ResourceNotFoundException("Category not found")` → `400 Bad Request` (caught by controller try/catch).

---

### TC-E-004 — Budget check blocks expense creation when limit would be exceeded
**Preconditions:** Active budget `amount=1000.0`, current `spent=900.0`. New expense `amount=200.0` → `newSpent=1100.0 >= 1000.0`.
**Expected:** `400 Bad Request`, message: `"This expense exceeds your category budget of ₹1000.0."`.
**Reason:** `budgetService.checkBudgetOnExpense()` is called first; if `shouldBlock=true`, throws `BadRequestException`.

---

### TC-E-005 — Budget warning does NOT block expense (alert only)
**Preconditions:** Budget `amount=1000.0`, `alertThreshold=0.80`, `spent=750.0`. New expense `amount=100.0` → `ratio=0.85 >= 0.80`.
**Expected:** `201 Created` — warnings do not block; only `shouldBlock=true` throws an exception.

---

### TC-E-006 — Title is required (validation)
**Input:** `"title": ""` or missing title.
**Expected:** `400 Bad Request` with validation error: `"Title is required"`.

---

### TC-E-007 — Amount must be positive (validation)
**Input:** `"amount": -100.0` or `"amount": 0`.
**Expected:** `400 Bad Request` with validation error: `"Amount must be greater than 0"`.

---

### TC-E-008 — Note is optional
**Input:** Request with no `note` field.
**Expected:** `201 Created`. `ExpenseResponse.note = null`.

---

### TC-E-009 — Unauthenticated request is rejected
**Request:** `POST /api/expenses` without `Authorization` header.
**Expected:** `401 Unauthorized`.

---

## 2. Get All Expenses (paginated) — `GET /api/expenses`

### TC-E-010 — Returns only expenses belonging to the authenticated user
**Preconditions:**
- Expense A: `userId=1`  → **included**
- Expense B: `userId=2`  → excluded

**Expected:** `200 OK`. Only Expense A appears in `data.content`.

---

### TC-E-011 — Default pagination: page=0, size=10, sorted by `createdAt` DESC
**Preconditions:** 15 expenses for the user.
**Expected:** First page returns 10 expenses, sorted newest first. `data.totalElements=15`, `data.totalPages=2`.

---

### TC-E-012 — Custom pagination parameters
**Request:** `GET /api/expenses?page=1&size=5`
**Expected:** Returns items 6–10 (second page of 5).

---

### TC-E-013 — Returns empty page when user has no expenses
**Expected:** `200 OK`, `data.content=[]`, `data.totalElements=0`.

---

## 3. Get Expense by ID — `GET /api/expenses/{id}`

### TC-E-014 — Returns expense owned by the authenticated user
**Preconditions:** Expense `id=10` belongs to `userId=1`.
**Expected:** `200 OK` with correct `ExpenseResponse`.

---

### TC-E-015 — Returns 404 when expense belongs to a different user
**Preconditions:** Expense `id=10` belongs to `userId=2`.
**Call:** Authenticated as `userId=1`.
**Expected:** `404 Not Found`, message: `"Expense not found"`.
**Reason:** `expenseRepository.findById(id).filter { it.userId == userId }` returns empty if ownership doesn't match.

---

### TC-E-016 — Returns 404 when expense does not exist
**Preconditions:** No expense with `id=9999`.
**Expected:** `404 Not Found`.

---

## 4. Update Expense — `PUT /api/expenses/{id}`

### TC-E-017 — Update all fields (happy path)
**Request body:**
```json
{
  "title": "Rent",
  "amount": 12000.0,
  "categoryId": 2,
  "userId": 0,
  "note": "Monthly rent"
}
```
**Preconditions:** Expense `id=5` exists and belongs to `userId=1`. Category `id=2` exists.
**Expected:** `200 OK`, updated fields reflected in response. `createdAt` is unchanged (only mutable fields are copied).

---

### TC-E-018 — Update with non-existent categoryId
**Input:** `categoryId=9999`
**Expected:** `ResourceNotFoundException("Category not found")` → `400 Bad Request`.

---

### TC-E-019 — Cannot update another user's expense
**Preconditions:** Expense `id=5` belongs to `userId=2`.
**Call:** Authenticated as `userId=1`.
**Expected:** `404 Not Found`, message: `"Expense not found"`.

---

### TC-E-020 — Update non-existent expense
**Call:** `PUT /api/expenses/9999`
**Expected:** `404 Not Found`.

---

### TC-E-021 — `userId` in update request body is ignored
**Scenario:** Client sends `"userId": 99`. Authenticated as `userId=1`, expense `id=5` belongs to `userId=1`.
**Expected:** Update succeeds. Expense still owned by `userId=1`.
**Note:** Service uses the `userId` passed by the controller (from JWT), not from the request body.

---

## 5. Delete Expense — `DELETE /api/expenses/{id}`

### TC-E-022 — Delete owned expense (happy path)
**Preconditions:** Expense `id=5` belongs to `userId=1`.
**Expected:** `200 OK`, message: `"Expense deleted successfully"`. Expense no longer retrievable.

---

### TC-E-023 — Cannot delete another user's expense
**Preconditions:** Expense `id=5` belongs to `userId=2`.
**Call:** Authenticated as `userId=1`.
**Expected:** `404 Not Found`, message: `"Expense not found"`.

---

### TC-E-024 — Delete non-existent expense
**Call:** `DELETE /api/expenses/9999`
**Expected:** `404 Not Found`.

---

## 6. Search Expenses — `GET /api/expenses/search?keyword=`

### TC-E-025 — Case-insensitive title search
**Preconditions:** Expenses with titles: `"Groceries"`, `"GROCERY run"`, `"Taxi"`.
**Request:** `GET /api/expenses/search?keyword=grocer`
**Expected:** Returns `"Groceries"` and `"GROCERY run"` only (not `"Taxi"`).
**Reason:** `filterByTitle` uses `LOWER(title) LIKE %grocer%`.

---

### TC-E-026 — Search returns only the authenticated user's results
**Preconditions:** `userId=1` has `"Groceries"`. `userId=2` also has `"Groceries"`.
**Expected:** Only `userId=1`'s expense is returned (spec always includes `filterByUserId`).

---

### TC-E-027 — Blank keyword returns all expenses (no filter applied)
**Preconditions:** User has 3 expenses.
**Request:** `GET /api/expenses/search?keyword=`
**Expected:** All 3 expenses returned.
**Reason:** `filterByTitle` returns `conjunction()` (no-op) when `title.isNullOrBlank()`.

---

### TC-E-028 — Results sorted by `createdAt` DESC
**Expected:** Newest expense appears first in the list.

---

## 7. Filter by Category — `GET /api/expenses/filter/category?categoryId=`

### TC-E-029 — Returns only expenses in the specified category
**Preconditions:** User has: 2 expenses in `categoryId=1`, 3 in `categoryId=2`.
**Request:** `GET /api/expenses/filter/category?categoryId=1`
**Expected:** Returns 2 expenses, all with `categoryId=1`.

---

### TC-E-030 — Returns empty list for a category with no expenses
**Request:** `GET /api/expenses/filter/category?categoryId=99`
**Expected:** `200 OK`, `data=[]`.

---

### TC-E-031 — Category filter is scoped to authenticated user
**Preconditions:** `userId=2` has an expense in `categoryId=1`. Authenticated as `userId=1`.
**Expected:** `userId=2`'s expense is NOT returned.

---

## 8. Filter by Amount Range — `GET /api/expenses/filter/amount?minAmount=&maxAmount=`

### TC-E-032 — Returns expenses within the amount range (inclusive bounds)
**Preconditions:** User expenses: `₹100`, `₹500`, `₹1000`, `₹2000`.
**Request:** `?minAmount=500&maxAmount=1000`
**Expected:** Returns `₹500` and `₹1000` only.
**Reason:** `filterByMinAmount` uses `>=`, `filterByMaxAmount` uses `<=`.

---

### TC-E-033 — Invalid range throws bad request
**Request:** `?minAmount=1000&maxAmount=500` (min > max)
**Expected:** `400 Bad Request`, message: `"Invalid amount range"`.
**Reason:** `require(minAmount >= 0 && maxAmount >= minAmount)` in service.

---

### TC-E-034 — Negative minAmount throws bad request
**Request:** `?minAmount=-50&maxAmount=500`
**Expected:** `400 Bad Request`, message: `"Invalid amount range"`.

---

### TC-E-035 — Same min and max returns exact match
**Request:** `?minAmount=500&maxAmount=500`
**Expected:** Returns only expenses with `amount=500.0` exactly.

---

## 9. Filter by Date Range — `GET /api/expenses/filter/date-range?startDate=&endDate=`

### TC-E-036 — Returns expenses within the date range (inclusive)
**Preconditions:** Expenses at `2026-03-01T10:00`, `2026-03-10T15:00`, `2026-03-20T09:00`.
**Request:** `?startDate=2026-03-01T00:00:00&endDate=2026-03-10T23:59:59`
**Expected:** Returns `2026-03-01` and `2026-03-10` expenses only.
**Reason:** `filterByStartDate` uses `>=`, `filterByEndDate` uses `<=`.

---

### TC-E-037 — startDate after endDate throws bad request
**Request:** `?startDate=2026-03-10T00:00:00&endDate=2026-03-01T00:00:00`
**Expected:** `400 Bad Request`, message: `"Start date must be before end date"`.

---

### TC-E-038 — startDate equals endDate returns expenses on that exact datetime
**Request:** `?startDate=2026-03-10T15:00:00&endDate=2026-03-10T15:00:00`
**Expected:** Returns only expense with `createdAt=2026-03-10T15:00:00` exactly.

---

## 10. Advanced Filter — `POST /api/expenses/filter`

### TC-E-039 — All filters null returns all user expenses (paginated)
**Request body:** `{}`
**Expected:** All expenses for the user, paginated with default `page=0, size=10`.

---

### TC-E-040 — Title + category combined filter
**Request body:**
```json
{
  "searchTitle": "food",
  "categoryId": 3
}
```
**Expected:** Returns only expenses matching both: title contains "food" AND `categoryId=3`.

---

### TC-E-041 — Amount range + date range combined
**Request body:**
```json
{
  "minAmount": 100.0,
  "maxAmount": 500.0,
  "startDate": "2026-03-01T00:00:00",
  "endDate": "2026-03-31T23:59:59"
}
```
**Expected:** Returns expenses where `100 <= amount <= 500` AND `createdAt` within March 2026.

---

### TC-E-042 — Month/year filter returns expenses for that month only
**Request body:**
```json
{
  "year": 2026,
  "month": 3
}
```
**Expected:** Returns expenses with `createdAt` between `2026-03-01T00:00:00` and `2026-03-31T23:59:59`.
**Reason:** `filterByMonthYear` computes `start = YearMonth.atDay(1).atStartOfDay()` and `end = atEndOfMonth().atTime(23,59,59)`, then uses `BETWEEN`.

---

### TC-E-043 — Only `year` provided without `month` → no month filter applied
**Request body:** `{ "year": 2026 }`
**Expected:** Month/year filter is skipped (returns `conjunction()` when either `year` or `month` is null).

---

### TC-E-044 — Pagination and sort parameters via query params
**Request:** `POST /api/expenses/filter?page=1&size=5&sortBy=amount`
**Expected:** Second page of 5 results, sorted by `amount` DESC.

---

## 11. ExpenseSpecifications — Unit-level Behaviour

### TC-SP-001 — `filterByTitle` — null input returns no-op (conjunction)
**Input:** `title = null`
**Expected:** All expenses pass the spec (no WHERE clause on title).

---

### TC-SP-002 — `filterByCategory` — null input returns no-op
**Input:** `categoryId = null`
**Expected:** All expenses pass the spec.

---

### TC-SP-003 — `filterByMinAmount` — null input returns no-op
**Input:** `minAmount = null`
**Expected:** All expenses pass the spec.

---

### TC-SP-004 — `filterByMaxAmount` — null input returns no-op
**Input:** `maxAmount = null`
**Expected:** All expenses pass the spec.

---

### TC-SP-005 — `filterByStartDate` — null input returns no-op
**Input:** `startDate = null`
**Expected:** All expenses pass the spec.

---

### TC-SP-006 — `filterByEndDate` — null input returns no-op
**Input:** `endDate = null`
**Expected:** All expenses pass the spec.

---

### TC-SP-007 — `filterByMonthYear` — only one of year/month provided returns no-op
**Input:** `year=2026, month=null` or `year=null, month=3`
**Expected:** Month/year filter not applied.

---

### TC-SP-008 — `buildFilterSpecification` always includes `filterByUserId`
**Input:** Any `ExpenseFilterRequest`, any `userId`.
**Expected:** Results always scoped to the given `userId`, regardless of other filters.

---

## 12. ExpenseRepository

### TC-ER-001 — `findByUserId` returns all expenses for the user
**Preconditions:** `userId=1` has 3 expenses; `userId=2` has 2.
**Expected:** `findByUserId(1)` returns 3 expenses, none from `userId=2`.

---

### TC-ER-002 — `findAll(spec, pageable)` with `filterByUserId` spec scopes results correctly
**Preconditions:** Multiple users have expenses.
**Expected:** Only the target user's expenses are returned.

---

### TC-ER-003 — `sumAmountByUserIdAndCategoryIdAndDateBetween` — correct sum
**Preconditions:** `userId=1`, `categoryId=2`, expenses `₹200 + ₹300 + ₹500` in `[2026-03-01, 2026-04-01)`.
**Expected:** Returns `1000.0`.

---

### TC-ER-004 — `sumAmountByUserIdAndDateBetween` — sums across all categories
**Preconditions:** `userId=1`, category A `₹1000` + category B `₹2000` in the date window.
**Expected:** Returns `3000.0`.

---

## 13. Edge Cases

### TC-EE-001 — `ExpenseResponse.note` is nullable
**Preconditions:** Expense saved without a note.
**Expected:** `note = null` in the response, no serialization error.

---

### TC-EE-002 — Expense `createdAt` is set automatically on creation
**Expected:** `createdAt` is populated by the entity default (`LocalDateTime.now()`); client does not supply it.

---

### TC-EE-003 — Category is eagerly required on `Expense` entity (`nullable=false`)
**Expected:** Attempting to save an `Expense` without a valid `category` throws a constraint violation.

---

### TC-EE-004 — `deleteExpense` is a hard delete, not soft
**Expected:** `expenseRepository.deleteById(id)` permanently removes the record. No `deletedAt` field on `Expense`.

---

### TC-EE-005 — Budget check runs before category lookup in `createExpense`
**Expected order in `createExpense()`:**
1. `budgetService.checkBudgetOnExpense()` → throws `BadRequestException` if blocked
2. `categoryRepository.findById()` → throws `ResourceNotFoundException` if missing
3. `expenseRepository.save()`

If budget check blocks, category is never queried.

---

## Test Data Reference

```kotlin
// Minimal valid ExpenseRequest
val expenseRequest = ExpenseRequest(
    title      = "Groceries",
    amount     = 500.0,
    categoryId = 1L,
    userId     = 0L,    // overwritten by controller from JWT
    note       = "Weekly shopping"
)

// Filter request — title + category
val filterByTitleAndCategory = ExpenseFilterRequest(
    searchTitle = "food",
    categoryId  = 3L
)

// Filter request — month/year
val filterByMonth = ExpenseFilterRequest(
    year  = 2026,
    month = 3
)

// Filter request — amount range + date range
val filterByAmountAndDate = ExpenseFilterRequest(
    minAmount = 100.0,
    maxAmount = 500.0,
    startDate = LocalDateTime.of(2026, 3, 1, 0, 0),
    endDate   = LocalDateTime.of(2026, 3, 31, 23, 59, 59)
)
```

---

## Coverage Checklist

| Area | Test Cases |
|---|---|
| Create expense (happy path, budget block, validation) | TC-E-001 to TC-E-009 |
| List expenses (pagination, user scoping) | TC-E-010 to TC-E-013 |
| Get by ID (ownership, not found) | TC-E-014 to TC-E-016 |
| Update expense (fields, ownership, validation) | TC-E-017 to TC-E-021 |
| Delete expense (hard delete, ownership) | TC-E-022 to TC-E-024 |
| Search by keyword (case-insensitive, scoping, blank) | TC-E-025 to TC-E-028 |
| Filter by category | TC-E-029 to TC-E-031 |
| Filter by amount range (inclusive, validation) | TC-E-032 to TC-E-035 |
| Filter by date range (inclusive, validation) | TC-E-036 to TC-E-038 |
| Advanced filter (combined fields, pagination, month/year) | TC-E-039 to TC-E-044 |
| ExpenseSpecifications null-safety | TC-SP-001 to TC-SP-008 |
| ExpenseRepository queries | TC-ER-001 to TC-ER-004 |
| Edge cases (note null, hard delete, order of ops) | TC-EE-001 to TC-EE-005 |