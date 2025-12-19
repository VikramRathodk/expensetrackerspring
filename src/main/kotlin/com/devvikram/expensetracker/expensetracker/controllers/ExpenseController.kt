package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.models.ApiResponse
import com.devvikram.expensetracker.expensetracker.models.Expense
import com.devvikram.expensetracker.expensetracker.service.ExpenseService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/expenses")
class ExpenseController(
    private val expenseService: ExpenseService
) {

    @PostMapping
    fun createExpense(@Valid @RequestBody expense: Expense): ApiResponse<Expense> {
        val savedExpense = expenseService.addExpense(expense)
        return ApiResponse(
            status = true,
            message = "Expense added successfully",
            data = savedExpense
        )
    }
    @PostMapping("/bulk")
    fun createExpenses(
        @Valid @RequestBody expenses: List<Expense>
    ): ApiResponse<List<Expense>> {

        val savedExpenses = expenseService.addExpenses(expenses)

        return ApiResponse(
            status = true,
            message = "Expenses added successfully",
        )
    }

    @PutMapping("/{id}")
    fun updateExpense(
        @PathVariable id: Long,
        @Valid @RequestBody expense: Expense
    ): ApiResponse<Expense> {

        val updatedExpense = expenseService.updateExpense(id, expense)

        return ApiResponse(
            status = true,
            message = "Expense updated successfully",
            data = updatedExpense
        )
    }



    @GetMapping
    fun getExpenses(): ApiResponse<List<Expense>> {
        val expenses = expenseService.getAllExpenses()
        return ApiResponse(
            status = true,
            message = "Expenses fetched successfully",
            data = expenses
        )
    }

    @DeleteMapping("/{id}")
    fun deleteExpense(@PathVariable id: Long): ApiResponse<Unit> {
        expenseService.deleteExpense(id)
        return ApiResponse(
            status = true,
            message = "Expense deleted successfully"
        )
    }
}
