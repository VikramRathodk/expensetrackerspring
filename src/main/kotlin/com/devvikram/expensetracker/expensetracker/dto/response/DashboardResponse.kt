package com.devvikram.expensetracker.expensetracker.dto.response

import java.time.LocalDate

data class DashboardResponse(
    val thisMonthSummary: MonthSummary,
    val budgetOverview: BudgetOverview,
    val recentExpenses: List<ExpenseResponse>,
    val upcomingRecurring: List<RecurringExpenseResponse>,
    val categoryBreakdown: List<CategoryBreakdown>,
    val unreadNotificationsCount: Long
)

data class MonthSummary(
    val month: String,           // e.g. "March 2026"
    val totalSpent: Double,
    val expenseCount: Int
)

data class BudgetOverview(
    val totalBudgetLimit: Double,
    val totalSpent: Double,
    val totalRemaining: Double,
    val overallUtilizationPercent: Double,
    val budgets: List<BudgetStatusResponse>
)

data class CategoryBreakdown(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Double
)
