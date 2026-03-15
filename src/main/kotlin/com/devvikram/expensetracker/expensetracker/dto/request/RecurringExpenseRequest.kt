package com.devvikram.expensetracker.expensetracker.dto.request

import com.devvikram.expensetracker.expensetracker.enums.RecurringFrequency
import java.time.LocalDate

data class CreateRecurringExpenseRequest(
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val frequency: RecurringFrequency,
    val startDate: LocalDate,               // first due date
    val endDate: LocalDate? = null,         // null = runs forever
    val note: String? = null
)

data class UpdateRecurringExpenseRequest(
    val title: String? = null,
    val amount: Double? = null,
    val categoryId: Long? = null,
    val frequency: RecurringFrequency? = null,
    val nextDueDate: LocalDate? = null,     // manual override of next due date
    val endDate: LocalDate? = null,
    val isActive: Boolean? = null,
    val note: String? = null
)