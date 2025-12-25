package com.devvikram.expensetracker.expensetracker.controllers


import com.devvikram.expensetracker.expensetracker.models.ApiResponse
import com.devvikram.expensetracker.expensetracker.models.ExpenseFilterRequest
import com.devvikram.expensetracker.expensetracker.models.ExpenseResponse
import com.devvikram.expensetracker.expensetracker.models.dtos.ExpenseRequest
import com.devvikram.expensetracker.expensetracker.service.ExpenseService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime


@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = ["*"])
class ExpenseController(private val expenseService: ExpenseService) {

    @GetMapping
    fun getAllExpenses(
        @RequestParam userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<ExpenseResponse>>> {
        return try {
            val expenses = expenseService.getAllExpensesPaginated(userId, page, size)
            ResponseEntity.ok(ApiResponse(
                status = true,
                message = "Expenses fetched successfully",
                data = expenses
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(status = false, message = e.message ?: "Error fetching expenses"))
        }
    }

    @GetMapping("/search")
    fun searchExpenses(
        @RequestParam userId: Long,
        @RequestParam keyword: String
    ): ResponseEntity<ApiResponse<List<ExpenseResponse>>> {
        return try {
            val expenses = expenseService.searchExpenses(userId, keyword)
            ResponseEntity.ok(ApiResponse(
                status = true,
                message = "Search completed",
                data = expenses
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(status = false, message = e.message ?: "Search failed"))
        }
    }

    @GetMapping("/filter/category")
    fun filterByCategory(
        @RequestParam userId: Long,
        @RequestParam categoryId: Long
    ): ResponseEntity<ApiResponse<List<ExpenseResponse>>> {
        return try {
            val expenses = expenseService.filterByCategory(userId, categoryId)
            ResponseEntity.ok(ApiResponse(
                status = true,
                message = "Filtered by category",
                data = expenses
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(status = false, message = e.message ?: "Filtering failed"))
        }
    }

    @GetMapping("/filter/amount")
    fun filterByAmount(
        @RequestParam userId: Long,
        @RequestParam minAmount: Double,
        @RequestParam maxAmount: Double
    ): ResponseEntity<ApiResponse<List<ExpenseResponse>>> {
        return try {
            val expenses = expenseService.filterByAmountRange(userId, minAmount, maxAmount)
            ResponseEntity.ok(ApiResponse(
                status = true,
                message = "Filtered by amount",
                data = expenses
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(status = false, message = e.message ?: "Filtering failed"))
        }
    }

    @GetMapping("/filter/date-range")
    fun filterByDateRange(
        @RequestParam userId: Long,
        @RequestParam startDate: LocalDateTime,
        @RequestParam endDate: LocalDateTime
    ): ResponseEntity<ApiResponse<List<ExpenseResponse>>> {
        return try {
            val expenses = expenseService.filterByDateRange(userId, startDate, endDate)
            ResponseEntity.ok(ApiResponse(
                status = true,
                message = "Filtered by date range",
                data = expenses
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(status = false, message = e.message ?: "Filtering failed"))
        }
    }

    @PostMapping("/filter")
    fun filterExpenses(
        @RequestParam userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestBody request: ExpenseFilterRequest
    ): ResponseEntity<ApiResponse<Page<ExpenseResponse>>> {
        return try {
            val expenses = expenseService.filterExpenses(userId, request, page, size, sortBy)
            ResponseEntity.ok(ApiResponse(
                status = true,
                message = "Advanced filter applied",
                data = expenses
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(status = false, message = e.message ?: "Filtering failed"))
        }
    }

    @PostMapping
    fun createExpense(
        @Valid @RequestBody request: ExpenseRequest
    ): ResponseEntity<ApiResponse<ExpenseResponse>> {
        return try {
            val created = expenseService.createExpense(request)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse(
                    status = true,
                    message = "Expense created successfully",
                    data = created
                ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(status = false, message = e.message ?: "Failed to create expense"))
        }
    }

    @GetMapping("/{id}")
    fun getExpense(
        @PathVariable id: Long,
        @RequestParam userId: Long
    ): ResponseEntity<ApiResponse<ExpenseResponse>> {
        return try {
            val expense = expenseService.getExpenseById(id, userId)
            if (expense != null) {
                ResponseEntity.ok(ApiResponse(
                    status = true,
                    message = "Expense fetched",
                    data = expense
                ))
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse(status = false, message = "Expense not found"))
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(status = false, message = e.message ?: "Error fetching expense"))
        }
    }

    @PutMapping("/{id}")
    fun updateExpense(
        @PathVariable id: Long,
        @RequestParam userId: Long,
        @Valid @RequestBody request: ExpenseRequest
    ): ResponseEntity<ApiResponse<ExpenseResponse>> {
        return try {
            val updated = expenseService.updateExpense(id, userId, request)
            if (updated != null) {
                ResponseEntity.ok(ApiResponse(
                    status = true,
                    message = "Expense updated successfully",
                    data = updated
                ))
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse(status = false, message = "Expense not found"))
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(status = false, message = e.message ?: "Failed to update expense"))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteExpense(
        @PathVariable id: Long,
        @RequestParam userId: Long
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            val deleted = expenseService.deleteExpense(id, userId)
            if (deleted) {
                ResponseEntity.ok(ApiResponse(
                    status = true,
                    message = "Expense deleted successfully"
                ))
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse(status = false, message = "Expense not found"))
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(status = false, message = e.message ?: "Failed to delete expense"))
        }
    }
}