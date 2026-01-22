package com.devvikram.expensetracker.expensetracker.dto.response

// Auth Response
data class AuthResponse(
    val token: String,
    val user: UserResponse
)