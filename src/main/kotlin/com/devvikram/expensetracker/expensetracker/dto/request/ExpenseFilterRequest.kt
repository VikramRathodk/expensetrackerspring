package com.devvikram.expensetracker.expensetracker.dto.request

import java.time.LocalDateTime

data class ExpenseFilterRequest(
    val categoryId: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val searchTitle: String? = null,
    val year: Int? = null,
    val month: Int? = null,
    val tagIds: List<Long>? = null
)