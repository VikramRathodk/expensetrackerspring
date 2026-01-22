package com.devvikram.expensetracker.expensetracker.dto.request

import java.time.LocalDateTime

data class CustomReportRequest(
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val categoryIds: Set<Long>?,
    val minAmount: Double?,
    val maxAmount: Double?
)
