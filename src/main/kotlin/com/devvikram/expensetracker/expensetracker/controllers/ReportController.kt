package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.request.CustomReportRequest
import com.devvikram.expensetracker.expensetracker.dto.response.ApiResponse
import com.devvikram.expensetracker.expensetracker.dto.response.CategoryWiseReportResponse
import com.devvikram.expensetracker.expensetracker.dto.response.DateWiseReportResponse
import com.devvikram.expensetracker.expensetracker.dto.response.ExpenseResponse
import com.devvikram.expensetracker.expensetracker.dto.response.SummaryReportResponse
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAuthenticated
import com.devvikram.expensetracker.expensetracker.security.anotation.IsSuperAdmin
import com.devvikram.expensetracker.expensetracker.service.ReportService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/reports")
@IsAuthenticated
@IsSuperAdmin
class ReportController(
    private val reportService: ReportService,
    private val userRepository: UserRepository
) {

    private fun getUserId(userDetails: UserDetails): Long =
        userRepository.findByEmail(userDetails.username)
            .orElseThrow { RuntimeException("User not found") }
            .id


    @GetMapping("/summary")
    fun summary(@AuthenticationPrincipal userDetails: UserDetails)
            : ResponseEntity<ApiResponse<SummaryReportResponse>> {

        val userId = getUserId(userDetails)
        val data = reportService.getSummary(userId)

        return ResponseEntity.ok(ApiResponse(true, "Summary fetched", data))
    }


    @GetMapping("/category-wise")
    fun categoryWise(@AuthenticationPrincipal userDetails: UserDetails)
            : ResponseEntity<ApiResponse<List<CategoryWiseReportResponse>>> {

        val userId = getUserId(userDetails)
        val data = reportService.getCategoryWise(userId)

        return ResponseEntity.ok(ApiResponse(true, "Category-wise report fetched", data))
    }


    @GetMapping("/date-wise")
    fun dateWise(@AuthenticationPrincipal userDetails: UserDetails)
            : ResponseEntity<ApiResponse<List<DateWiseReportResponse>>> {

        val userId = getUserId(userDetails)
        val data = reportService.getDateWise(userId)

        return ResponseEntity.ok(ApiResponse(true, "Date-wise report fetched", data))
    }


    @PostMapping("/custom")
    fun customReport(
        @RequestBody request: CustomReportRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<ExpenseResponse>>> {

        val userId = getUserId(userDetails)
        val data = reportService.getCustomReport(userId, request)

        return ResponseEntity.ok(ApiResponse(true, "Custom report fetched", data))
    }
}
