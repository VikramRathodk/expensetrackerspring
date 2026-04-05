package com.devvikram.expensetracker.expensetracker.dto.request

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateBaseCurrencyRequest(

    @field:Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @field:Pattern(
        regexp = "^[A-Z]{3}$",
        message = "Currency code must be 3 uppercase letters (e.g. INR, USD, EUR)"
    )
    val baseCurrency: String
)
