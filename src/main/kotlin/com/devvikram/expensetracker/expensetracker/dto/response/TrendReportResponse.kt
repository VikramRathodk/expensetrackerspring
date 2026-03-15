package com.devvikram.expensetracker.expensetracker.dto.response

data class MonthlyTrendResponse(
    val year: Int,
    val month: Int,
    val monthLabel: String,          // e.g. "March 2026"
    val totalAmount: Double,
    val expenseCount: Long,
    val categoryBreakdown: List<CategoryTrendItem> = emptyList()
)

data class CategoryTrendItem(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Double           // share of that month's total
)

data class BudgetPerformanceResponse(
    val budgetId: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val period: String,              // BudgetPeriod name
    val budgetLimit: Double,
    val spent: Double,
    val remaining: Double,
    val percentUsed: Double,
    val isOverBudget: Boolean,
    val isNearLimit: Boolean,
    val status: String               // ON_TRACK | NEAR_LIMIT | EXCEEDED
)
