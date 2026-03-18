package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.response.ApiResponse
import com.devvikram.expensetracker.expensetracker.dto.request.ExpenseFilterRequest
import com.devvikram.expensetracker.expensetracker.dto.response.ExpenseResponse
import com.devvikram.expensetracker.expensetracker.dto.request.ExpenseRequest
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAuthenticated
import com.devvikram.expensetracker.expensetracker.service.ExpenseService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/expenses")
@CrossOrigin(origins = ["*"])
@IsAuthenticated
class ExpenseController(
    private val expenseService: ExpenseService,
    private val userRepository: UserRepository
) {

    @GetMapping
    fun getAllExpenses(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Page<ExpenseResponse>>> {
        return try {
            val userId = getUserIdFromAuth(userDetails)
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
        @RequestParam keyword: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<ExpenseResponse>>> {
        return try {
            val userId = getUserIdFromAuth(userDetails)
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
        @RequestParam categoryId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<ExpenseResponse>>> {
        return try {
            val userId = getUserIdFromAuth(userDetails)
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
        @RequestParam minAmount: Double,
        @RequestParam maxAmount: Double,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<ExpenseResponse>>> {
        return try {
            val userId = getUserIdFromAuth(userDetails)
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
        @RequestParam startDate: LocalDateTime,
        @RequestParam endDate: LocalDateTime,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<ExpenseResponse>>> {
        return try {
            val userId = getUserIdFromAuth(userDetails)
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
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestBody request: ExpenseFilterRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Page<ExpenseResponse>>> {
        return try {
            val userId = getUserIdFromAuth(userDetails)
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
        @Valid @RequestBody request: ExpenseRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<ExpenseResponse>> {
        return try {
            val userId = getUserIdFromAuth(userDetails)
            // Ensure the request userId matches the authenticated user
            val validatedRequest = request.copy(userId = userId)
            val created = expenseService.createExpense(validatedRequest)
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

    /** Create an expense and optionally attach multiple receipts in one request (multipart). */
    @PostMapping("/with-receipt", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createExpenseWithReceipt(
        @RequestPart("expense") request: ExpenseRequest,
        @RequestPart(value = "files", required = false) files: List<MultipartFile>?,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<ExpenseResponse>> {
        return try {
            val userId = getUserIdFromAuth(userDetails)
            val validatedRequest = request.copy(userId = userId)
            val created = expenseService.createExpenseWithReceipt(validatedRequest, files ?: emptyList())
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse(status = true, message = "Expense created successfully", data = created))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(status = false, message = e.message ?: "Failed to create expense"))
        }
    }

    @GetMapping("/{id}")
    fun getExpense(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<ExpenseResponse>> {
        return try {
            val userId = getUserIdFromAuth(userDetails)
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
        @Valid @RequestBody request: ExpenseRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<ExpenseResponse>> {
        return try {
            val userId = getUserIdFromAuth(userDetails)
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
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            val userId = getUserIdFromAuth(userDetails)
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

    private fun getUserIdFromAuth(userDetails: UserDetails): Long {
        return userRepository.findByEmail(userDetails.username)
            .orElseThrow { RuntimeException("User not found") }
            .id
    }
}