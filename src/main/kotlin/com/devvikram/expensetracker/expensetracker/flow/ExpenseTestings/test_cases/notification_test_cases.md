# Notification API — Test Cases

Base URL: `http://localhost:8081`
Auth: All endpoints require `Authorization: Bearer <token>`

---

## Prerequisites

1. Register a user and obtain a JWT token via `POST /api/auth/login`.
2. Create at least one budget (`POST /api/budgets`) to trigger `BUDGET_ALERT` / `BUDGET_EXCEEDED` notifications.
3. Create a recurring expense (`POST /api/recurring-expenses`) to trigger `RECURRING_EXPENSE_DUE` notifications.

---

## TC-N-01 — Automatic Budget Alert Notification

**Trigger:** Create an expense that pushes spending to ≥ `alertThreshold` of a budget but below the limit.

**Steps:**
1. Create a MONTHLY budget: `amount = 10000`, `alertThreshold = 0.80`, `categoryId = <food>`
2. Add expenses totalling `₹7,999` in the food category.
3. `POST /api/expenses` with `amount = 1` (now at 80% of limit).

**Expected:**
- Expense created successfully (HTTP 201).
- `GET /api/notifications/unread` returns one new notification:
  - `type = BUDGET_ALERT`
  - `title = "Budget Alert"`
  - `entityType = "Budget"`
  - `isRead = false`

---

## TC-N-02 — Automatic Budget Exceeded Notification + Block

**Trigger:** Create an expense that would exceed the budget limit.

**Steps:**
1. Using the same budget from TC-N-01 (limit = `₹10,000`).
2. Current spending = `₹8,000`.
3. `POST /api/expenses` with `amount = 2100`.

**Expected:**
- HTTP 400 Bad Request — expense is blocked.
- Response body contains `"This expense exceeds your category budget of ₹10000.0."`.
- `GET /api/notifications/unread` returns one new notification:
  - `type = BUDGET_EXCEEDED`
  - `title = "Budget Exceeded"`
  - `isRead = false`

---

## TC-N-03 — Recurring Expense Notification (Scheduler)

**Trigger:** The daily scheduler (`00:05`) auto-processes a due recurring expense.

**Steps:**
1. Create a recurring expense with `nextDueDate = today`.
2. Trigger `RecurringExpenseService.processRecurringExpenses()` (or wait for scheduled run).

**Expected:**
- `GET /api/notifications` returns one new notification:
  - `type = RECURRING_EXPENSE_DUE`
  - `title = "Recurring Expense Processed"`
  - `message` contains the expense title and next due date
  - `entityType = "RecurringExpense"`

---

## TC-N-04 — GET /api/notifications (Paginated)

| # | Input | Expected |
|---|-------|----------|
| 1 | `page=0&size=20` (default) | HTTP 200, `data.content` is array, `data.totalElements >= 0` |
| 2 | `page=0&size=2` | At most 2 items in `data.content`, `data.totalPages` correct |
| 3 | `page=999&size=20` | HTTP 200, `data.content = []` |

---

## TC-N-05 — GET /api/notifications/unread

| # | Scenario | Expected |
|---|----------|----------|
| 1 | User has unread notifications | HTTP 200, array of `isRead = false` items |
| 2 | All notifications already read | HTTP 200, empty array `[]` |
| 3 | No notifications at all | HTTP 200, empty array `[]` |

---

## TC-N-06 — GET /api/notifications/unread/count

| # | Scenario | Expected |
|---|----------|----------|
| 1 | 3 unread notifications | HTTP 200, `data = 3` |
| 2 | All read | HTTP 200, `data = 0` |

---

## TC-N-07 — PUT /api/notifications/{id}/read

| # | Input | Expected |
|---|-------|----------|
| 1 | Valid `id` belonging to current user | HTTP 200, `data.isRead = true` |
| 2 | `id` of another user's notification | HTTP 404 |
| 3 | Non-existent `id` | HTTP 404 |
| 4 | Already-read notification | HTTP 200, `data.isRead = true` (idempotent) |

**Side-effect:** Audit log entry with `action = NOTIFICATION_READ` is created.

---

## TC-N-08 — PUT /api/notifications/read-all

| # | Scenario | Expected |
|---|----------|----------|
| 1 | 3 unread notifications | HTTP 200, `data.markedRead = 3`, subsequent `/unread/count` returns `0` |
| 2 | Already all read | HTTP 200, `data.markedRead = 0` |
| 3 | Mix of read and unread | Only unread ones are updated |

---

## TC-N-09 — DELETE /api/notifications/{id}

| # | Input | Expected |
|---|-------|----------|
| 1 | Valid `id` belonging to current user | HTTP 200, `data = null`, notification no longer in `GET /api/notifications` |
| 2 | `id` of another user's notification | HTTP 404 |
| 3 | Non-existent `id` | HTTP 404 |

**Side-effect:** Audit log entry with `action = NOTIFICATION_DELETED` is created.

---

## TC-N-10 — Unauthenticated Access

| # | Endpoint | Expected |
|---|----------|----------|
| 1 | Any endpoint without `Authorization` header | HTTP 401 Unauthorized |
| 2 | Any endpoint with expired JWT | HTTP 401 Unauthorized |

---

## TC-N-11 — Notification Failure Does Not Interrupt Primary Operation

**Scenario:** Simulate NotificationRepository failure (e.g., DB constraint violation).

**Expected:**
- The primary operation (expense creation, recurring processing) completes successfully.
- Error is only logged; no exception propagates to the caller.
