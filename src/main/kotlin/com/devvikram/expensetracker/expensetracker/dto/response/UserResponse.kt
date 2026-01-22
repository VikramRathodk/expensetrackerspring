package com.devvikram.expensetracker.expensetracker.dto.response

// User Response
data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val roles: List<String>,
    val isActive: Boolean,
    val createdAt: String
)