package com.devvikram.expensetracker.expensetracker.dto.response

import java.time.LocalDate

data class InsightReportResponse(

    // ── Month comparison ──────────────────────────────────────────────────────
    val totalThisMonth: Double,
    val totalLastMonth: Double,

    /** Positive = spending went up, negative = went down. */
    val monthOverMonthChange: Double,

    /** "INCREASING" | "DECREASING" | "STABLE" */
    val spendingVelocity: String,

    // ── 30-day averages ───────────────────────────────────────────────────────
    val averageDailySpendLast30Days: Double,

    // ── Highlights ────────────────────────────────────────────────────────────
    val highestSpendDay: LocalDate?,
    val highestSpendDayAmount: Double,
    val biggestExpense: ExpenseResponse?,

    // ── Category behaviour ────────────────────────────────────────────────────
    val mostUsedCategory: String?,
    val mostUsedCategoryCount: Int
)
