package com.devvikram.expensetracker.expensetracker.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

// Login Request
data class LoginRequest(
    @field:NotBlank(message = "Email is required")

    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)