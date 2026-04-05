package com.devvikram.expensetracker.expensetracker.dto.response

import com.devvikram.expensetracker.expensetracker.enums.NotificationType
import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long,
    val title: String,
    val message: String,
    val type: NotificationType,
    val isRead: Boolean,
    val entityType: String?,
    val entityId: Long?,
    val createdAt: LocalDateTime
)
