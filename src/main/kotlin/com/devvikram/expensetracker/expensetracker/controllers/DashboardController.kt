package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.response.ApiResponse
import com.devvikram.expensetracker.expensetracker.dto.response.DashboardResponse
import com.devvikram.expensetracker.expensetracker.security.SecurityUtil
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAuthenticated
import com.devvikram.expensetracker.expensetracker.service.DashboardService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController(
    private val dashboardService: DashboardService,
    private val securityUtil: SecurityUtil
) {

    // GET /api/dashboard
    @GetMapping
    @IsAuthenticated
    fun getDashboard(): ResponseEntity<ApiResponse<DashboardResponse>> {
        val userId = securityUtil.getCurrentUserId()
        val dashboard = dashboardService.getDashboard(userId)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Dashboard fetched successfully", data = dashboard)
        )
    }
}
