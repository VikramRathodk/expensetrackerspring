package com.devvikram.expensetracker.expensetracker.models.dtos

import com.devvikram.expensetracker.expensetracker.enums.RoleType

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.NotEmpty


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

// Login Request
data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

// Auth Response
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

// User Response
data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val roles: List<String>,
    val isActive: Boolean,
    val createdAt: String
)

data class AssignRoleRequest(
    @field:NotNull(message = "User ID is required")
    val userId: Long,

    @field:NotEmpty(message = "Roles are required")
    val roles: Set<RoleType>
)
