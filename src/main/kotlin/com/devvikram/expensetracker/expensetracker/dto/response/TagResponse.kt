package com.devvikram.expensetracker.expensetracker.dto.response

import java.time.LocalDateTime

data class TagResponse(
    val id: Long,
    val name: String,
    val color: String,
    val userId: Long,
    val createdAt: LocalDateTime
)
