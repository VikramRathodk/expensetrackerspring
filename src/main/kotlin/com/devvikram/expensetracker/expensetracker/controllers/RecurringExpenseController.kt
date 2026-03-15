package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.request.CreateRecurringExpenseRequest
import com.devvikram.expensetracker.expensetracker.dto.request.UpdateRecurringExpenseRequest
import com.devvikram.expensetracker.expensetracker.dto.response.ApiResponse
import com.devvikram.expensetracker.expensetracker.dto.response.RecurringExpenseResponse
import com.devvikram.expensetracker.expensetracker.security.SecurityUtil
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAuthenticated
import com.devvikram.expensetracker.expensetracker.service.RecurringExpenseService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/recurring-expenses")
class RecurringExpenseController(
    private val recurringExpenseService: RecurringExpenseService,
    private val securityUtil: SecurityUtil
) {

    // POST /api/recurring-expenses
    @PostMapping
    @IsAuthenticated
    fun createRecurringExpense(
        @RequestBody request: CreateRecurringExpenseRequest
    ): ResponseEntity<ApiResponse<RecurringExpenseResponse>> {
        val userId = securityUtil.getCurrentUserId()
        val result = recurringExpenseService.createRecurringExpense(userId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(status = true, message = "Recurring expense created successfully", data = result))
    }

    // GET /api/recurring-expenses
    @GetMapping
    @IsAuthenticated
    fun getAllRecurringExpenses(): ResponseEntity<ApiResponse<List<RecurringExpenseResponse>>> {
        val userId = securityUtil.getCurrentUserId()
        val result = recurringExpenseService.getAllRecurringExpenses(userId)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Recurring expenses fetched successfully", data = result)
        )
    }

    // GET /api/recurring-expenses/{id}
    @GetMapping("/{id}")
    @IsAuthenticated
    fun getRecurringExpenseById(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<RecurringExpenseResponse>> {
        val userId = securityUtil.getCurrentUserId()
        val result = recurringExpenseService.getRecurringExpenseById(userId, id)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Recurring expense fetched successfully", data = result)
        )
    }

    // PUT /api/recurring-expenses/{id}
    @PutMapping("/{id}")
    @IsAuthenticated
    fun updateRecurringExpense(
        @PathVariable id: Long,
        @RequestBody request: UpdateRecurringExpenseRequest
    ): ResponseEntity<ApiResponse<RecurringExpenseResponse>> {
        val userId = securityUtil.getCurrentUserId()
        val result = recurringExpenseService.updateRecurringExpense(userId, id, request)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Recurring expense updated successfully", data = result)
        )
    }

    // DELETE /api/recurring-expenses/{id}
    @DeleteMapping("/{id}")
    @IsAuthenticated
    fun deleteRecurringExpense(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        val userId = securityUtil.getCurrentUserId()
        recurringExpenseService.deleteRecurringExpense(userId, id)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Recurring expense deleted successfully", data = null)
        )
    }
}