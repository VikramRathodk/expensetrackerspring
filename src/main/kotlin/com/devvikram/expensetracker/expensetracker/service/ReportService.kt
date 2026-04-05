package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.dto.request.CustomReportRequest
import com.devvikram.expensetracker.expensetracker.dto.response.*
import com.devvikram.expensetracker.expensetracker.repository.BudgetRepository
import com.devvikram.expensetracker.expensetracker.repository.ExpenseRepository
import com.devvikram.expensetracker.expensetracker.repository.ReportRepository
import com.devvikram.expensetracker.expensetracker.specifications.ExpenseSpecifications
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val budgetService: BudgetService
) {

    fun getSummary(userId: Long): SummaryReportResponse =
        reportRepository.summaryReport(userId)

    fun getCategoryWise(userId: Long): List<CategoryWiseReportResponse> =
        reportRepository.categoryWiseReport(userId)

    fun getDateWise(userId: Long): List<DateWiseReportResponse> =
        reportRepository.getDateWiseReport(userId)

    @Transactional(readOnly = true)
    fun getCustomReport(userId: Long, request: CustomReportRequest): List<ExpenseResponse> =
        expenseRepository.findAll(
            ExpenseSpecifications.build(userId, request)
        ).map { ExpenseResponse.fromEntity(it) }

    // ── Monthly Trend ─────────────────────────────────────────────────────────

    /**
     * Returns month-over-month spending totals for the last [months] calendar months
     * (default 6, capped at 24). Each entry includes a category breakdown.
     */
    fun getMonthlyTrend(userId: Long, months: Int): List<MonthlyTrendResponse> {
        val clampedMonths = months.coerceIn(1, 24)
        val from = LocalDate.now()
            .minusMonths(clampedMonths.toLong())
            .withDayOfMonth(1)
            .atStartOfDay()

        // Raw totals per month
        val totals = reportRepository.monthlyTrend(userId, from)
            .associate { row ->
                val year  = (row[0] as Number).toInt()
                val month = (row[1] as Number).toInt()
                Pair(year, month) to Pair(
                    (row[2] as Number).toDouble(),   // totalAmount
                    (row[3] as Number).toLong()      // expenseCount
                )
            }

        // Category breakdown per month
        val categoryRows = reportRepository.monthlyTrendByCategory(userId, from)
        val categoryByMonth = mutableMapOf<Pair<Int, Int>, MutableList<Array<Any>>>()
        for (row in categoryRows) {
            val key = Pair((row[0] as Number).toInt(), (row[1] as Number).toInt())
            categoryByMonth.getOrPut(key) { mutableListOf() }.add(row)
        }

        // Build a complete list covering every month in the window (including months with 0 spend)
        val result = mutableListOf<MonthlyTrendResponse>()
        var cursor = from.toLocalDate()
        val endMonth = LocalDate.now().withDayOfMonth(1)

        while (!cursor.isAfter(endMonth)) {
            val key   = Pair(cursor.year, cursor.monthValue)
            val (total, count) = totals[key] ?: Pair(0.0, 0L)
            val catRows = categoryByMonth[key] ?: emptyList()

            val breakdown = catRows.map { row ->
                val catAmount = (row[4] as Number).toDouble()
                CategoryTrendItem(
                    categoryId   = (row[2] as Number).toLong(),
                    categoryName = row[3].toString(),
                    totalAmount  = catAmount,
                    percentage   = if (total > 0) (catAmount / total) * 100 else 0.0
                )
            }

            result.add(
                MonthlyTrendResponse(
                    year             = cursor.year,
                    month            = cursor.monthValue,
                    monthLabel       = "${Month.of(cursor.monthValue).getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${cursor.year}",
                    totalAmount      = total,
                    expenseCount     = count,
                    categoryBreakdown = breakdown
                )
            )
            cursor = cursor.plusMonths(1)
        }
        return result
    }

    // ── Spending Insights ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    fun getInsights(userId: Long): InsightReportResponse {
        val now        = LocalDate.now()
        val thisMonthStart = now.withDayOfMonth(1).atStartOfDay()
        val lastMonthStart = now.minusMonths(1).withDayOfMonth(1).atStartOfDay()
        val lastMonthEnd   = thisMonthStart.minusNanos(1)
        val thirtyDaysAgo  = now.minusDays(30).atStartOfDay()

        val allExpenses = expenseRepository.findAll(
            ExpenseSpecifications.filterByUserId(userId)
        )

        val thisMonth = allExpenses.filter { !it.createdAt.isBefore(thisMonthStart) }
        val lastMonth = allExpenses.filter {
            !it.createdAt.isBefore(lastMonthStart) && !it.createdAt.isAfter(lastMonthEnd)
        }
        val last30    = allExpenses.filter { !it.createdAt.isBefore(thirtyDaysAgo) }

        val totalThisMonth = thisMonth.sumOf { it.amount }
        val totalLastMonth = lastMonth.sumOf { it.amount }
        val momChange      = totalThisMonth - totalLastMonth
        val velocity       = when {
            abs(momChange) < 0.01 -> "STABLE"
            momChange > 0         -> "INCREASING"
            else                  -> "DECREASING"
        }

        val avgDaily = if (last30.isEmpty()) 0.0 else last30.sumOf { it.amount } / 30.0

        // highest spend day in the last 30 days
        val byDay = last30.groupBy { it.createdAt.toLocalDate() }
        val highestDay = byDay.maxByOrNull { (_, exps) -> exps.sumOf { it.amount } }
        val highestDayAmount = highestDay?.value?.sumOf { it.amount } ?: 0.0

        // biggest single expense all-time for user
        val biggest = allExpenses.maxByOrNull { it.amount }

        // most used category (by count)
        val catFreq = allExpenses.groupBy { it.category.name }
        val mostUsedEntry = catFreq.maxByOrNull { (_, exps) -> exps.size }

        return InsightReportResponse(
            totalThisMonth              = totalThisMonth,
            totalLastMonth              = totalLastMonth,
            monthOverMonthChange        = momChange,
            spendingVelocity            = velocity,
            averageDailySpendLast30Days = avgDaily,
            highestSpendDay             = highestDay?.key,
            highestSpendDayAmount       = highestDayAmount,
            biggestExpense              = biggest?.let { ExpenseResponse.fromEntity(it) },
            mostUsedCategory            = mostUsedEntry?.key,
            mostUsedCategoryCount       = mostUsedEntry?.value?.size ?: 0
        )
    }

    // ── Top Expenses ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    fun getTopExpenses(userId: Long, limit: Int, categoryId: Long?): List<ExpenseResponse> {
        var spec = ExpenseSpecifications.filterByUserId(userId)
        if (categoryId != null) spec = spec.and(ExpenseSpecifications.filterByCategory(categoryId))
        return expenseRepository.findAll(spec)
            .sortedByDescending { it.amount }
            .take(limit.coerceIn(1, 100))
            .map { ExpenseResponse.fromEntity(it) }
    }

    // ── Budget Performance ────────────────────────────────────────────────────

    /**
     * Returns real-time performance for every active budget:
     * limit vs spent, with ON_TRACK / NEAR_LIMIT / EXCEEDED status.
     */
    fun getBudgetPerformance(userId: Long): List<BudgetPerformanceResponse> {
        return budgetRepository
            .findByUserIdAndIsActiveTrueAndDeletedAtIsNull(userId)
            .map { budget ->
                val status = budgetService.getBudgetStatus(userId, budget.id)
                val performanceStatus = when {
                    status.isOverBudget -> "EXCEEDED"
                    status.isNearLimit  -> "NEAR_LIMIT"
                    else                -> "ON_TRACK"
                }
                BudgetPerformanceResponse(
                    budgetId     = budget.id,
                    categoryId   = budget.category?.id,
                    categoryName = budget.category?.name,
                    period       = budget.period.name,
                    budgetLimit  = status.limit,
                    spent        = status.spent,
                    remaining    = status.remaining,
                    percentUsed  = status.percentUsed,
                    isOverBudget = status.isOverBudget,
                    isNearLimit  = status.isNearLimit,
                    status       = performanceStatus
                )
            }
    }
}
