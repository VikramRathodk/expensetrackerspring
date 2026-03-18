package com.devvikram.expensetracker.expensetracker.dto.response

import java.time.LocalDateTime

data class ReceiptResponse(
    val id: Long,
    val expenseId: Long,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val uploadedAt: LocalDateTime,
    /** API path to download the raw file bytes. */
    val downloadUrl: String
)
