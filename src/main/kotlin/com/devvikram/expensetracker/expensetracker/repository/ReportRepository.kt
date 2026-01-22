package com.devvikram.expensetracker.expensetracker.repository

import com.devvikram.expensetracker.expensetracker.dto.response.CategoryWiseReportResponse
import com.devvikram.expensetracker.expensetracker.dto.response.DateWiseReportResponse
import com.devvikram.expensetracker.expensetracker.dto.response.SummaryReportResponse
import com.devvikram.expensetracker.expensetracker.entity.Expense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate


@Repository
interface ReportRepository : JpaRepository<Expense, Long> {

    @Query("""
        SELECT new com.devvikram.expensetracker.expensetracker.dto.response.CategoryWiseReportResponse(
            c.id,
            c.name,
            SUM(e.amount)
        )
        FROM Expense e
        JOIN e.category c
        WHERE e.userId = :userId
        GROUP BY c.id, c.name
        ORDER BY SUM(e.amount) DESC
    """)
    fun categoryWiseReport(@Param("userId") userId: Long): List<CategoryWiseReportResponse>

    @Query("""
        SELECT new com.devvikram.expensetracker.expensetracker.dto.response.DateWiseReportResponse(
            CAST(e.createdAt AS LocalDate),
            SUM(e.amount)
        )
        FROM Expense e
        WHERE e.userId = :userId
        GROUP BY CAST(e.createdAt AS LocalDate)
        ORDER BY CAST(e.createdAt AS LocalDate)
    """)
    fun getDateWiseReport(@Param("userId") userId: Long): List<DateWiseReportResponse>

    @Query("""
        SELECT new com.devvikram.expensetracker.expensetracker.dto.response.SummaryReportResponse(
            COALESCE(SUM(e.amount), 0.0),
            COUNT(e)
        )
        FROM Expense e
        WHERE e.userId = :userId
    """)
    fun summaryReport(@Param("userId") userId: Long): SummaryReportResponse
}