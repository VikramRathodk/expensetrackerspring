# Notification & Dashboard Feature Guide

## Feature Overview

This document covers the in-app notification system and the user dashboard introduced in `dev-feature/notification-dashboard`. Both features are designed to give users real-time awareness of their financial activity without requiring external services such as email or push notifications.

**Notifications** are generated automatically by the application in three situations:
- A budget is nearing its configured alert threshold
- A budget has been exceeded by a new expense
- A recurring expense has been auto-processed by the daily scheduler

**Dashboard** provides a single aggregated view of the user's financial state for the current month, including spending summary, budget utilisation, recent expenses, upcoming recurring expenses, category breakdown, and unread notification count.

---

## Notification Types

| Type | Trigger | Severity |
|------|---------|----------|
| `BUDGET_ALERT` | Spending after adding an expense reaches or exceeds `alertThreshold` of a budget limit | Warning |
| `BUDGET_EXCEEDED` | Spending after adding an expense meets or exceeds the full budget `amount` | Blocking |
| `RECURRING_EXPENSE_DUE` | Daily scheduler (`RecurringExpenseService`) auto-processes a due recurring entry | Informational |
| `SYSTEM` | Reserved for generic system-level messages (manual / future use) | Varies |

### How Notifications Are Triggered

**Budget notifications** — `BudgetService.checkBudgetOnExpense()` is called from `ExpenseService.createExpense()` before the expense is saved. For every active budget that applies to the user+category combination (both category-scoped and overall budgets are checked), the service:
1. Computes current spend for the period.
2. Adds the new expense amount.
3. If `newSpent >= budget.amount` → sends `BUDGET_EXCEEDED` and sets `shouldBlock = true` (the expense is rejected).
4. If `newSpent / budget.amount >= alertThreshold` → sends `BUDGET_ALERT`.

**Recurring expense notifications** — `RecurringExpenseService.processSingleRecurringExpense()` fires at `00:05` every day. After saving the auto-created expense and advancing `nextDueDate`, it calls `notificationService.send()` with type `RECURRING_EXPENSE_DUE`.

All `notificationService.send()` calls are wrapped in a try-catch so that a notification failure never interrupts the primary business operation.

---

## API Endpoints

All endpoints require a valid JWT Bearer token (`Authorization: Bearer <token>`).
Base URL: `http://localhost:8081`

---

### Dashboard

#### GET /api/dashboard

Returns an aggregated dashboard for the authenticated user.

**Response 200:**
```json
{
  "status": true,
  "message": "Dashboard fetched successfully",
  "data": {
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
      "budgets": [
        {
          "id": 1,
          "categoryId": 3,
          "categoryName": "Food",
          "limit": 8000.00,
          "spent": 5200.00,
          "remaining": 2800.00,
          "percentUsed": 65.0,
          "isOverBudget": false,
          "isNearLimit": true
        }
      ]
    },
    "recentExpenses": [
      {
        "id": 42,
        "title": "Grocery Store",
        "amount": 850.00,
        "categoryId": 3,
        "categoryName": "Food",
        "note": null,
        "createdAt": "2026-03-14T18:30:00"
      }
    ],
    "upcomingRecurring": [
      {
        "id": 5,
        "title": "Netflix",
        "amount": 649.00,
        "categoryId": 7,
        "categoryName": "Entertainment",
        "frequency": "MONTHLY",
        "nextDueDate": "2026-03-16",
        "endDate": null,
        "isActive": true,
        "note": null,
        "createdAt": "2026-01-16T10:00:00"
      }
    ],
    "categoryBreakdown": [
      {
        "categoryId": 3,
        "categoryName": "Food",
        "totalAmount": 5200.00,
        "percentage": 41.6
      }
    ],
    "unreadNotificationsCount": 3
  }
}
```

**Dashboard fields explained:**

| Field | Description |
|-------|-------------|
| `thisMonthSummary.month` | Current month and year formatted as "MMMM yyyy" |
| `thisMonthSummary.totalSpent` | Sum of all expenses created in the current calendar month |
| `thisMonthSummary.expenseCount` | Number of expenses in the current month |
| `budgetOverview.totalBudgetLimit` | Sum of `amount` across all active, non-deleted budgets |
| `budgetOverview.overallUtilizationPercent` | `(totalSpent / totalBudgetLimit) * 100` |
| `recentExpenses` | Last 5 expenses ordered by `createdAt DESC` |
| `upcomingRecurring` | Active recurring expenses with `nextDueDate <= today + 7 days`, sorted ascending |
| `categoryBreakdown` | This-month expenses grouped by category, sorted by `totalAmount DESC` |
| `unreadNotificationsCount` | Count of notifications where `isRead = false` |

---

### Notifications

#### GET /api/notifications

Returns a paginated list of all notifications for the authenticated user, newest first.

**Query Parameters:**

| Param | Default | Description |
|-------|---------|-------------|
| `page` | `0` | Zero-based page index |
| `size` | `20` | Number of records per page |

**Response 200:**
```json
{
  "status": true,
  "message": "Notifications fetched successfully",
  "data": {
    "content": [
      {
        "id": 12,
        "title": "Budget Alert",
        "message": "You've used 75% of your category budget of ₹8000.0.",
        "type": "BUDGET_ALERT",
        "isRead": false,
        "entityType": "Budget",
        "entityId": 1,
        "createdAt": "2026-03-14T18:30:05"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

---

#### GET /api/notifications/unread

Returns all unread notifications for the authenticated user, newest first (not paginated).

**Response 200:**
```json
{
  "status": true,
  "message": "Unread notifications fetched successfully",
  "data": [
    {
      "id": 12,
      "title": "Budget Alert",
      "message": "You've used 75% of your category budget of ₹8000.0.",
      "type": "BUDGET_ALERT",
      "isRead": false,
      "entityType": "Budget",
      "entityId": 1,
      "createdAt": "2026-03-14T18:30:05"
    }
  ]
}
```

---

#### GET /api/notifications/unread/count

Returns the count of unread notifications for the authenticated user.

**Response 200:**
```json
{
  "status": true,
  "message": "Unread count fetched successfully",
  "data": 3
}
```

---

#### PUT /api/notifications/{id}/read

Marks a single notification as read. Returns 404 if the notification does not belong to the authenticated user.

**Path Variable:** `id` — notification ID

**Response 200:**
```json
{
  "status": true,
  "message": "Notification marked as read",
  "data": {
    "id": 12,
    "title": "Budget Alert",
    "message": "You've used 75% of your category budget of ₹8000.0.",
    "type": "BUDGET_ALERT",
    "isRead": true,
    "entityType": "Budget",
    "entityId": 1,
    "createdAt": "2026-03-14T18:30:05"
  }
}
```

**Audit:** Logged as `NOTIFICATION_READ`.

---

#### PUT /api/notifications/read-all

Marks all unread notifications for the authenticated user as read.

**Response 200:**
```json
{
  "status": true,
  "message": "3 notification(s) marked as read",
  "data": {
    "markedRead": 3
  }
}
```

---

#### DELETE /api/notifications/{id}

Permanently deletes a notification. Returns 404 if the notification does not belong to the authenticated user.

**Path Variable:** `id` — notification ID

**Response 200:**
```json
{
  "status": true,
  "message": "Notification deleted successfully",
  "data": null
}
```

**Audit:** Logged as `NOTIFICATION_DELETED`.

---

## Database Schema

The `notifications` table is created by Flyway migration `V8__create_notifications.sql`.

```sql
CREATE TABLE notifications (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(150) NOT NULL,
    message     TEXT         NOT NULL,
    type        VARCHAR(50)  NOT NULL,     -- maps to NotificationType enum
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    entity_type VARCHAR(50),               -- e.g. "Budget", "RecurringExpense"
    entity_id   BIGINT,                    -- FK (logical) to the related entity
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

Three indexes are created:
- `idx_notifications_user_id` — fast lookup by user
- `idx_notifications_user_unread` — partial index on unread records per user (used by badge counts)
- `idx_notifications_created_at` — descending sort support

---

## Audit Logging

Two new `AuditAction` values were added:

| Action | When |
|--------|------|
| `NOTIFICATION_READ` | `PUT /api/notifications/{id}/read` |
| `NOTIFICATION_DELETED` | `DELETE /api/notifications/{id}` |

Bulk `markAllAsRead` is not individually audited to avoid flooding the audit log.

---

## Service Architecture

```
ExpenseService.createExpense()
    └── BudgetService.checkBudgetOnExpense()
            └── NotificationService.send()   [BUDGET_ALERT / BUDGET_EXCEEDED]

RecurringExpenseService.processSingleRecurringExpense()  [scheduler: 00:05 daily]
    └── NotificationService.send()           [RECURRING_EXPENSE_DUE]

NotificationController  →  NotificationService  →  NotificationRepository
DashboardController     →  DashboardService     →  (ExpenseRepo, BudgetRepo, RecurringRepo, NotificationService)
```
