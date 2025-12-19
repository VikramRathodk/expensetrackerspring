package com.devvikram.expensetracker.expensetracker.service


import com.devvikram.expensetracker.expensetracker.exceptions.BadRequestException
import com.devvikram.expensetracker.expensetracker.exceptions.ResourceNotFoundException
import com.devvikram.expensetracker.expensetracker.models.Expense
import com.devvikram.expensetracker.expensetracker.repository.ExpenseRepository
import org.springframework.stereotype.Service

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository
) {

    fun addExpense(expense: Expense): Expense =
        expenseRepository.save(expense)

    fun getAllExpenses(): List<Expense> =
        expenseRepository.findAll()

    fun deleteExpense(id: Long) {
        if (!expenseRepository.existsById(id)) {
            throw ResourceNotFoundException("Expense with id $id not found")
        }
        expenseRepository.deleteById(id)
    }

    fun addExpenses(expenses: List<Expense>): List<Expense> {
        if (expenses.isEmpty()) {
            throw BadRequestException("Expense list cannot be empty")
        }
        return expenseRepository.saveAll(expenses)
    }

    fun updateExpense(id: Long, expense: Expense): Expense {

        val existingExpense = expenseRepository.findById(id)
            .orElseThrow {
                ResourceNotFoundException("Expense with id $id not found")
            }

        val updatedExpense = existingExpense.copy(
            title = expense.title,
            amount = expense.amount,
            category = expense.category,
            note = expense.note
        )

        return expenseRepository.save(updatedExpense)
    }


}
