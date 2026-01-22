package com.devvikram.expensetracker.expensetracker.dto.response

import java.time.LocalDateTime

data class ExpenseResponse(
    val id: Long,
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val note: String?,
    val createdAt: LocalDateTime
)