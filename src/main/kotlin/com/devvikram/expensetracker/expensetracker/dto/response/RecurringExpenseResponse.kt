package com.devvikram.expensetracker.expensetracker.dto.response

import com.devvikram.expensetracker.expensetracker.enums.RecurringFrequency
import java.time.LocalDate
import java.time.LocalDateTime

data class RecurringExpenseResponse(
    val id: Long,
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val frequency: RecurringFrequency,
    val nextDueDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean,
    val note: String?,
    val createdAt: LocalDateTime
)