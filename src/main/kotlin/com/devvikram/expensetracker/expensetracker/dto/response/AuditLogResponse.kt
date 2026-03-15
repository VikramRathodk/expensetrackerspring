package com.devvikram.expensetracker.expensetracker.dto.response

import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import java.time.LocalDateTime

data class AuditLogResponse(
    val id: Long,
    val userId: Long,
    val action: AuditAction,
    val entityType: String,
    val entityId: Long?,
    val oldValue: String?,
    val newValue: String?,
    val ipAddress: String?,
    val createdAt: LocalDateTime
)
