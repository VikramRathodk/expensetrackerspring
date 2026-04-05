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
import java.time.LocalDateTime


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

    // ── Monthly trend ─────────────────────────────────────────────────────────

    /**
     * Returns one row per (year, month) for expenses on or after [from].
     * Row shape: [year: Number, month: Number, totalAmount: Number, expenseCount: Number]
     */
    @Query(
        value = """
            SELECT
                EXTRACT(YEAR  FROM created_at)::INT AS year,
                EXTRACT(MONTH FROM created_at)::INT AS month,
                COALESCE(SUM(amount), 0)            AS total_amount,
                COUNT(*)                            AS expense_count
            FROM expenses
            WHERE user_id = :userId
              AND created_at >= :from
            GROUP BY EXTRACT(YEAR FROM created_at), EXTRACT(MONTH FROM created_at)
            ORDER BY year ASC, month ASC
        """,
        nativeQuery = true
    )
    fun monthlyTrend(
        @Param("userId") userId: Long,
        @Param("from") from: LocalDateTime
    ): List<Array<Any>>

    /**
     * Returns one row per (year, month, categoryId, categoryName) for expenses on or after [from].
     * Row shape: [year, month, categoryId, categoryName, totalAmount]
     */
    @Query(
        value = """
            SELECT
                EXTRACT(YEAR  FROM e.created_at)::INT AS year,
                EXTRACT(MONTH FROM e.created_at)::INT AS month,
                c.id                                  AS category_id,
                c.name                                AS category_name,
                COALESCE(SUM(e.amount), 0)            AS total_amount
            FROM expenses e
            JOIN categories c ON e.category_id = c.id
            WHERE e.user_id = :userId
              AND e.created_at >= :from
            GROUP BY
                EXTRACT(YEAR  FROM e.created_at),
                EXTRACT(MONTH FROM e.created_at),
                c.id, c.name
            ORDER BY year ASC, month ASC, total_amount DESC
        """,
        nativeQuery = true
    )
    fun monthlyTrendByCategory(
        @Param("userId") userId: Long,
        @Param("from") from: LocalDateTime
    ): List<Array<Any>>
}