package com.devvikram.expensetracker.expensetracker.enums

enum class AuditAction {

    // ── Expense ───────────────────────────────────────────────────────────────
    EXPENSE_CREATED,
    EXPENSE_UPDATED,
    EXPENSE_DELETED,
    EXPENSE_AUTO_CREATED,           // triggered by the recurring expense scheduler

    // ── Budget ────────────────────────────────────────────────────────────────
    BUDGET_CREATED,
    BUDGET_UPDATED,
    BUDGET_DELETED,

    // ── Recurring Expense ─────────────────────────────────────────────────────
    RECURRING_EXPENSE_CREATED,
    RECURRING_EXPENSE_UPDATED,
    RECURRING_EXPENSE_DELETED,
    RECURRING_EXPENSE_PROCESSED,    // scheduler fired and created an auto-expense

    // ── Category ──────────────────────────────────────────────────────────────
    CATEGORY_CREATED,
    CATEGORY_UPDATED,
    CATEGORY_DELETED,

    // ── Auth ──────────────────────────────────────────────────────────────────
    USER_REGISTERED,
    USER_LOGIN,
    ROLE_ASSIGNED
}
