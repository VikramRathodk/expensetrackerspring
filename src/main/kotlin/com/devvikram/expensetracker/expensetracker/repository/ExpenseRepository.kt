package com.devvikram.expensetracker.expensetracker.repository


import com.devvikram.expensetracker.expensetracker.models.Expense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface ExpenseRepository : JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {
    fun findByUserId(userId: Long): List<Expense>
}