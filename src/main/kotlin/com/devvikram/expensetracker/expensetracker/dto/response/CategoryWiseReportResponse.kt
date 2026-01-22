package com.devvikram.expensetracker.expensetracker.dto.response

data class CategoryWiseReportResponse(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: Double
)