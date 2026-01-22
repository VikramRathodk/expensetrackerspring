package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.dto.request.CustomReportRequest
import com.devvikram.expensetracker.expensetracker.dto.response.CategoryWiseReportResponse
import com.devvikram.expensetracker.expensetracker.dto.response.DateWiseReportResponse
import com.devvikram.expensetracker.expensetracker.dto.response.ExpenseResponse
import com.devvikram.expensetracker.expensetracker.dto.response.SummaryReportResponse
import com.devvikram.expensetracker.expensetracker.repository.ExpenseRepository
import com.devvikram.expensetracker.expensetracker.repository.ReportRepository
import com.devvikram.expensetracker.expensetracker.specifications.ExpenseSpecifications
import org.springframework.stereotype.Service

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val expenseRepository: ExpenseRepository
) {

    fun getSummary(userId: Long): SummaryReportResponse {
        return reportRepository.summaryReport(userId)
    }

    fun getCategoryWise(userId: Long): List<CategoryWiseReportResponse> {
        return reportRepository.categoryWiseReport(userId)
    }

    fun getDateWise(userId: Long): List<DateWiseReportResponse> {
        return reportRepository.getDateWiseReport(userId)
    }

    fun getCustomReport(userId: Long, request: CustomReportRequest): List<ExpenseResponse> {
        return expenseRepository.findAll(
            ExpenseSpecifications.build(userId, request)
        ).map { ExpenseResponse.fromEntity(it) }
    }
}
