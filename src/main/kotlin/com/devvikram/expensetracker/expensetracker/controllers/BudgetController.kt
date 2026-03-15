package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.request.CreateBudgetRequest
import com.devvikram.expensetracker.expensetracker.dto.request.UpdateBudgetRequest
import com.devvikram.expensetracker.expensetracker.dto.response.ApiResponse
import com.devvikram.expensetracker.expensetracker.dto.response.BudgetResponse
import com.devvikram.expensetracker.expensetracker.dto.response.BudgetStatusResponse
import com.devvikram.expensetracker.expensetracker.security.SecurityUtil
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAuthenticated
import com.devvikram.expensetracker.expensetracker.service.BudgetService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/budgets")
class BudgetController(
    private val budgetService: BudgetService,
    private val securityUtil: SecurityUtil
) {

    // POST /api/budgets
    @PostMapping
    @IsAuthenticated
    fun createBudget(
        @RequestBody request: CreateBudgetRequest
    ): ResponseEntity<ApiResponse<BudgetResponse>> {
        val userId = securityUtil.getCurrentUserId()
        val budget = budgetService.createBudget(userId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(status = true, message = "Budget created successfully", data = budget))
    }

    // GET /api/budgets
    @GetMapping
    @IsAuthenticated
    fun getAllBudgets(): ResponseEntity<ApiResponse<List<BudgetResponse>>> {
        val userId = securityUtil.getCurrentUserId()
        val budgets = budgetService.getAllBudgets(userId)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Budgets fetched successfully", data = budgets)
        )
    }

    // GET /api/budgets/{id}/status
    @GetMapping("/{id}/status")
    @IsAuthenticated
    fun getBudgetStatus(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<BudgetStatusResponse>> {
        val userId = securityUtil.getCurrentUserId()
        val status = budgetService.getBudgetStatus(userId, id)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Budget status fetched successfully", data = status)
        )
    }

    // PUT /api/budgets/{id}
    @PutMapping("/{id}")
    @IsAuthenticated
    fun updateBudget(
        @PathVariable id: Long,
        @RequestBody request: UpdateBudgetRequest
    ): ResponseEntity<ApiResponse<BudgetResponse>> {
        val userId = securityUtil.getCurrentUserId()
        val updated = budgetService.updateBudget(userId, id, request)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Budget updated successfully", data = updated)
        )
    }

    // DELETE /api/budgets/{id}
    @DeleteMapping("/{id}")
    @IsAuthenticated
    fun deleteBudget(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        val userId = securityUtil.getCurrentUserId()
        budgetService.deleteBudget(userId, id)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Budget deleted successfully", data = null)
        )
    }
}