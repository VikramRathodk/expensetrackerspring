package com.devvikram.expensetracker.expensetracker.dto.request


import com.devvikram.expensetracker.expensetracker.enums.BudgetPeriod
import java.time.LocalDate

data class CreateBudgetRequest(
    val categoryId: Long? = null,           // null = overall budget
    val amount: Double,
    val period: BudgetPeriod,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val alertThreshold: Double = 0.80
)

data class UpdateBudgetRequest(
    val amount: Double? = null,
    val alertThreshold: Double? = null,
    val endDate: LocalDate? = null,
    val isActive: Boolean? = null
)