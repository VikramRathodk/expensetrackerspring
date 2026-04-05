package com.devvikram.expensetracker.expensetracker.dto.response

import java.time.LocalDateTime

data class ExchangeRateResponse(
    val id: Long,
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val fetchedAt: LocalDateTime
)
