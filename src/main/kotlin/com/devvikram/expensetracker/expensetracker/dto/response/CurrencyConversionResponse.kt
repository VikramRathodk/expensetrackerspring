package com.devvikram.expensetracker.expensetracker.dto.response

data class CurrencyConversionResponse(
    val fromCurrency: String,
    val toCurrency: String,
    val originalAmount: Double,
    val convertedAmount: Double
)
