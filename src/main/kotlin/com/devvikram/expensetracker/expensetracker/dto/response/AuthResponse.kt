package com.devvikram.expensetracker.expensetracker.dto.response

// Auth Response
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse
)