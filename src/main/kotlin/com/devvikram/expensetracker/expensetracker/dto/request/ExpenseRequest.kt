package com.devvikram.expensetracker.expensetracker.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class ExpenseRequest(
    @field:NotBlank(message = "Title is required")
    val title: String,

    @field:Positive(message = "Amount must be greater than 0")
    val amount: Double,

    @field:Positive(message = "Category ID must be valid")
    val categoryId: Long,

    @field:Positive(message = "User ID must be valid")
    val userId: Long,

    val note: String? = null,

    val tagIds: List<Long> = emptyList(),

    /** ISO 4217 currency code for this expense (default: INR). */
    val currency: String = "INR"
)