package com.devvikram.expensetracker.expensetracker.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class TagRequest(

    @field:NotBlank(message = "Tag name is required")
    val name: String,

    @field:Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "Color must be a valid hex color code (e.g. #6366f1)"
    )
    val color: String = "#6366f1"
)
