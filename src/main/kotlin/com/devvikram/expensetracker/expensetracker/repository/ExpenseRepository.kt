package com.devvikram.expensetracker.expensetracker.repository


import com.devvikram.expensetracker.expensetracker.models.Expense
import org.springframework.data.jpa.repository.JpaRepository

interface ExpenseRepository : JpaRepository<Expense, Long>
