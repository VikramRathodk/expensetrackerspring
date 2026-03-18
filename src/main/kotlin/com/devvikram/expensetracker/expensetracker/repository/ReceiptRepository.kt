package com.devvikram.expensetracker.expensetracker.repository

import com.devvikram.expensetracker.expensetracker.entity.Receipt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ReceiptRepository : JpaRepository<Receipt, Long> {
    fun findByExpenseIdAndUserId(expenseId: Long, userId: Long): List<Receipt>
    fun findByIdAndUserId(id: Long, userId: Long): Optional<Receipt>
    fun countByExpenseId(expenseId: Long): Long
}
