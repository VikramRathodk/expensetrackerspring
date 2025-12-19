package com.devvikram.expensetracker.expensetracker.models


data class ApiResponse<T>(
    val status: Boolean,
    val message: String,
    val data: T? = null
)
