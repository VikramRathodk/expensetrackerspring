package com.devvikram.expensetracker.expensetracker.dto.request

import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token must not be blank")
    val refreshToken: String
)
