package com.devvikram.expensetracker.expensetracker.dto.response


import com.devvikram.expensetracker.expensetracker.enums.BudgetPeriod
import java.time.LocalDate
import java.time.LocalDateTime

data class BudgetResponse(
    val id: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val amount: Double,
    val period: BudgetPeriod,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val alertThreshold: Double,
    val isActive: Boolean,
    val spent: Double,
    val remaining: Double,
    val percentUsed: Double,
    val createdAt: LocalDateTime
)

data class BudgetStatusResponse(
    val id: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val limit: Double,
    val spent: Double,
    val remaining: Double,
    val percentUsed: Double,
    val isOverBudget: Boolean,
    val isNearLimit: Boolean        // true when percentUsed >= alertThreshold * 100
)