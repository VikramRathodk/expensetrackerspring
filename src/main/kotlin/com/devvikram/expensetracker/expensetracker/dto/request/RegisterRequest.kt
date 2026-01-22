package com.devvikram.expensetracker.expensetracker.dto.request

import com.devvikram.expensetracker.expensetracker.enums.RoleType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size


// Register Request
data class RegisterRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    val name: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    val password: String,

    // Optional: Assign roles during registration (admin only)
    val roles: Set<RoleType>? = null
)







