package com.devvikram.expensetracker.expensetracker.exceptions

import java.time.LocalDateTime

data class ErrorResponse(
    val status: Boolean = false,
    val code: String,
    val message: String,
    val timestamp: String = LocalDateTime.now().toString(),
    val path: String? = null,
    val details: Any? = null        // used for validation field errors
)
