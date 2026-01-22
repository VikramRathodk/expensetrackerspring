package com.devvikram.expensetracker.expensetracker.dto.response


import java.time.LocalDate

data class DateWiseReportResponse(
    val date: LocalDate,
    val totalAmount: Double
)