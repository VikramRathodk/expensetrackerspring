package com.devvikram.expensetracker.expensetracker.dto.response

data class ApiResponse<T>(
    val status: Boolean,
    val message: String,
    val data: T? = null
)