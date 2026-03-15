# Dashboard API — Test Cases

Base URL: `http://localhost:8081`
Auth: All endpoints require `Authorization: Bearer <token>`

---

## Prerequisites

1. Register a user and obtain a JWT token via `POST /api/auth/login`.
2. Have at least a few expenses, budgets, and recurring expenses for the user under test.

---

## TC-D-01 — Empty State Dashboard (New User)

**Scenario:** User has no expenses, budgets, or recurring expenses.

**Steps:**
1. Register a new user.
2. `GET /api/dashboard`

**Expected:**
```json
{
  "status": true,
  "data": {
    "thisMonthSummary": { "totalSpent": 0.0, "expenseCount": 0 },
    "budgetOverview": {
      "totalBudgetLimit": 0.0,
      "totalSpent": 0.0,
      "totalRemaining": 0.0,
      "overallUtilizationPercent": 0.0,
      "budgets": []
    },
    "recentExpenses": [],
    "upcomingRecurring": [],
    "categoryBreakdown": [],
    "unreadNotificationsCount": 0
  }
}
```

---

## TC-D-02 — thisMonthSummary Accuracy

**Steps:**
1. Create 3 expenses in the current calendar month, totalling `₹5,000`.
2. Create 2 expenses in a previous month (these must NOT be counted).
3. `GET /api/dashboard`

**Expected:**
- `thisMonthSummary.totalSpent = 5000.0`
- `thisMonthSummary.expenseCount = 3`
- `thisMonthSummary.month` matches "MMMM yyyy" format (e.g. `"March 2026"`)

---

## TC-D-03 — budgetOverview with Single Budget

**Steps:**
1. Create a MONTHLY budget: `amount = 10000`, `alertThreshold = 0.80`.
2. Spend `₹6,500` in current period.
3. `GET /api/dashboard`

**Expected:**
- `budgetOverview.totalBudgetLimit = 10000.0`
- `budgetOverview.totalSpent = 6500.0`
- `budgetOverview.totalRemaining = 3500.0`
- `budgetOverview.overallUtilizationPercent ≈ 65.0`
- `budgetOverview.budgets` contains one entry with `isNearLimit = false`, `isOverBudget = false`

---

## TC-D-04 — budgetOverview Near Limit

**Steps:**
1. Spend `₹8,100` against the `₹10,000` budget (> 80% threshold).
2. `GET /api/dashboard`

**Expected:**
- `budgetOverview.budgets[0].isNearLimit = true`
- `budgetOverview.budgets[0].isOverBudget = false`

---

## TC-D-05 — recentExpenses (Last 5 Only)

**Steps:**
1. Create 7 expenses for the current user.
2. `GET /api/dashboard`

**Expected:**
- `recentExpenses` contains exactly 5 items.
- They are ordered by `createdAt DESC` (newest first).
- Each item has `id`, `title`, `amount`, `categoryId`, `categoryName`, `note`, `createdAt`.

---

## TC-D-06 — upcomingRecurring (Next 7 Days)

| # | nextDueDate | Expected in upcomingRecurring |
|---|-------------|-------------------------------|
| 1 | today | Yes |
| 2 | today + 6 days | Yes |
| 3 | today + 7 days | Yes |
| 4 | today + 8 days | No |
| 5 | yesterday (past due) | No |
| 6 | `isActive = false` | No |

**Steps:**
1. Create recurring expenses matching each case above.
2. `GET /api/dashboard`

**Expected:** Only cases 1, 2, 3 appear in `upcomingRecurring`, sorted ascending by `nextDueDate`.

---

## TC-D-07 — categoryBreakdown Percentages

**Steps:**
1. Spend `₹6,000` in Food category and `₹4,000` in Transport category (total `₹10,000`).
2. `GET /api/dashboard`

**Expected:**
- `categoryBreakdown` has 2 entries.
- Food: `totalAmount = 6000.0`, `percentage = 60.0`
- Transport: `totalAmount = 4000.0`, `percentage = 40.0`
- Sorted by `totalAmount DESC` (Food first).

---

## TC-D-08 — unreadNotificationsCount

**Steps:**
1. Trigger 3 budget alert notifications (add expenses near threshold).
2. `GET /api/dashboard`

**Expected:** `unreadNotificationsCount = 3`

3. Mark all as read via `PUT /api/notifications/read-all`.
4. `GET /api/dashboard`

**Expected:** `unreadNotificationsCount = 0`

---

## TC-D-09 — Multiple Active Budgets

**Steps:**
1. Create 2 budgets: one category-scoped (`₹5,000` for Food), one overall (`₹20,000`).
2. Spend `₹3,000` on Food.
3. `GET /api/dashboard`

**Expected:**
- `budgetOverview.totalBudgetLimit = 25000.0`
- `budgetOverview.budgets` has 2 entries.
- Food budget: `spent = 3000`, `remaining = 2000`
- Overall budget: `spent = 3000`, `remaining = 17000`

---

## TC-D-10 — Unauthenticated Access

| # | Scenario | Expected |
|---|----------|----------|
| 1 | No `Authorization` header | HTTP 401 Unauthorized |
| 2 | Expired JWT | HTTP 401 Unauthorized |
| 3 | Valid JWT for user A does NOT show user B's data | Data isolation verified |

---

## TC-D-11 — Data Isolation Between Users

**Steps:**
1. User A: create 3 expenses totalling `₹5,000`.
2. User B: create 2 expenses totalling `₹2,000`.
3. `GET /api/dashboard` with User A token.

**Expected:**
- `thisMonthSummary.totalSpent = 5000.0` (User A data only)
- `thisMonthSummary.expenseCount = 3`
