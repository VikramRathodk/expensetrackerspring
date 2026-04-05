package com.devvikram.expensetracker.expensetracker.enums

enum class NotificationType {

    // ── Budget ────────────────────────────────────────────────────────────────
    BUDGET_ALERT,           // spending nearing the threshold (warning)
    BUDGET_EXCEEDED,        // spending exceeded the budget limit (blocked)

    // ── Recurring Expense ─────────────────────────────────────────────────────
    RECURRING_EXPENSE_DUE,  // a recurring expense was auto-processed today

    // ── System ────────────────────────────────────────────────────────────────
    SYSTEM                  // generic system messages
}