package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.request.CustomReportRequest
import com.devvikram.expensetracker.expensetracker.dto.response.*
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import com.devvikram.expensetracker.expensetracker.security.anotation.IsSuperAdmin
import com.devvikram.expensetracker.expensetracker.service.ExportService
import com.devvikram.expensetracker.expensetracker.service.ReportService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/reports")
@IsSuperAdmin
class ReportController(
    private val reportService: ReportService,
    private val exportService: ExportService,
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

    // GET /api/reports/trends?months=6
    @GetMapping("/trends")
    fun monthlyTrend(
        @RequestParam(defaultValue = "6") months: Int,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<MonthlyTrendResponse>>> {
        val userId = getUserId(userDetails)
        val data = reportService.getMonthlyTrend(userId, months)
        return ResponseEntity.ok(ApiResponse(true, "Monthly trend report fetched", data))
    }

    // GET /api/reports/budget-performance
    @GetMapping("/budget-performance")
    fun budgetPerformance(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<BudgetPerformanceResponse>>> {
        val userId = getUserId(userDetails)
        val data = reportService.getBudgetPerformance(userId)
        return ResponseEntity.ok(ApiResponse(true, "Budget performance report fetched", data))
    }

    // GET /api/v1/reports/insights
    @GetMapping("/insights")
    fun insights(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<InsightReportResponse>> {
        val userId = getUserId(userDetails)
        val data = reportService.getInsights(userId)
        return ResponseEntity.ok(ApiResponse(true, "Spending insights fetched", data))
    }

    // GET /api/v1/reports/top-expenses?limit=10&categoryId=1
    @GetMapping("/top-expenses")
    fun topExpenses(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(required = false) categoryId: Long?,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<ExpenseResponse>>> {
        val userId = getUserId(userDetails)
        val data = reportService.getTopExpenses(userId, limit, categoryId)
        return ResponseEntity.ok(ApiResponse(true, "Top expenses fetched", data))
    }

    // GET /api/reports/export?format=csv|pdf
    @GetMapping("/export")
    fun export(
        @RequestParam(defaultValue = "csv") format: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ByteArray> {
        val userId = getUserId(userDetails)
        return when (format.lowercase()) {
            "pdf" -> {
                val bytes = exportService.exportPdf(userId)
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"expenses.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes)
            }
            else -> {
                val bytes = exportService.exportCsv(userId)
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"expenses.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(bytes)
            }
        }
    }
}
