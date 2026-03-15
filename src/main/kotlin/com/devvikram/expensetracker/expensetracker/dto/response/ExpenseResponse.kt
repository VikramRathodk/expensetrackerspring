package com.devvikram.expensetracker.expensetracker.dto.response

import com.devvikram.expensetracker.expensetracker.entity.Expense
import java.time.LocalDateTime

data class ExpenseResponse(
    val id: Long,
    val title: String,
    val amount: Double,
    val currency: String,
    val amountInBase: Double,
    val categoryId: Long,
    val categoryName: String,
    val note: String?,
    val createdAt: LocalDateTime,
    val tags: List<TagResponse> = emptyList()
) {
    companion object {
        fun fromEntity(it: Expense): ExpenseResponse {
            return ExpenseResponse(
                id           = it.id,
                title        = it.title,
                amount       = it.amount,
                currency     = it.currency,
                amountInBase = it.amountInBase,
                categoryId   = it.category.id,
                categoryName = it.category.name,
                note         = it.note,
                createdAt    = it.createdAt,
                tags         = it.tags.map { tag ->
                    TagResponse(
                        id        = tag.id,
                        name      = tag.name,
                        color     = tag.color,
                        userId    = tag.userId,
                        createdAt = tag.createdAt
                    )
                }
            )
        }
    }
}