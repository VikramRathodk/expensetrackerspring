package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.dto.response.*
import com.devvikram.expensetracker.expensetracker.entity.Expense
import com.devvikram.expensetracker.expensetracker.repository.BudgetRepository
import com.devvikram.expensetracker.expensetracker.repository.ExpenseRepository
import com.devvikram.expensetracker.expensetracker.repository.RecurringExpenseRepository
import com.devvikram.expensetracker.expensetracker.specifications.ExpenseSpecifications
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class DashboardService(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val budgetService: BudgetService,
    private val notificationService: NotificationService
) {

    fun getDashboard(userId: Long): DashboardResponse {
        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = monthStart.plusMonths(1)

        // ── This-month expenses ───────────────────────────────────────────────
        val monthExpenses = expenseRepository.findAll(
            ExpenseSpecifications.filterByUserId(userId)
                .and(ExpenseSpecifications.filterByStartDate(monthStart.atStartOfDay()))
                .and(ExpenseSpecifications.filterByEndDate(monthEnd.atStartOfDay()))
        )
        val totalSpentThisMonth = monthExpenses.sumOf { it.amount }

        val monthSummary = MonthSummary(
            month        = today.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            totalSpent   = totalSpentThisMonth,
            expenseCount = monthExpenses.size
        )

        // ── Budget overview ───────────────────────────────────────────────────
        val activeBudgets = budgetRepository
            .findByUserIdAndIsActiveTrueAndDeletedAtIsNull(userId)

        val budgetStatuses = activeBudgets.map { budgetService.getBudgetStatus(userId, it.id) }

        val totalLimit   = activeBudgets.sumOf { it.amount }
        val totalSpent   = budgetStatuses.sumOf { it.spent }
        val totalRemain  = (totalLimit - totalSpent).coerceAtLeast(0.0)
        val utilization  = if (totalLimit > 0) (totalSpent / totalLimit) * 100 else 0.0

        val budgetOverview = BudgetOverview(
            totalBudgetLimit          = totalLimit,
            totalSpent                = totalSpent,
            totalRemaining            = totalRemain,
            overallUtilizationPercent = utilization,
            budgets                   = budgetStatuses
        )

        // ── Recent expenses (last 5) ──────────────────────────────────────────
        val recent = expenseRepository.findAll(
            ExpenseSpecifications.filterByUserId(userId),
            PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).content.map { it.toResponse() }

        // ── Upcoming recurring (due in next 7 days) ───────────────────────────
        val nextWeek = today.plusDays(7)
        val upcoming = recurringExpenseRepository
            .findByUserIdAndIsActiveTrueAndDeletedAtIsNull(userId)
            .filter { it.nextDueDate <= nextWeek }
            .sortedBy { it.nextDueDate }
            .map { it.toResponse() }

        // ── Category breakdown (this month) ───────────────────────────────────
        val categoryBreakdown = monthExpenses
            .groupBy { it.category }
            .map { (cat, expenses) ->
                val catTotal = expenses.sumOf { it.amount }
                CategoryBreakdown(
                    categoryId   = cat.id,
                    categoryName = cat.name,
                    totalAmount  = catTotal,
                    percentage   = if (totalSpentThisMonth > 0) (catTotal / totalSpentThisMonth) * 100 else 0.0
                )
            }
            .sortedByDescending { it.totalAmount }

        // ── Unread notifications count ────────────────────────────────────────
        val unreadCount = notificationService.getUnreadCount(userId)

        return DashboardResponse(
            thisMonthSummary       = monthSummary,
            budgetOverview         = budgetOverview,
            recentExpenses         = recent,
            upcomingRecurring      = upcoming,
            categoryBreakdown      = categoryBreakdown,
            unreadNotificationsCount = unreadCount
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun Expense.toResponse() = ExpenseResponse(
        id           = id,
        title        = title,
        amount       = amount,
        currency     = currency,
        amountInBase = amountInBase,
        categoryId   = category.id,
        categoryName = category.name,
        note         = note,
        createdAt    = createdAt
    )

    private fun com.devvikram.expensetracker.expensetracker.entity.RecurringExpense.toResponse() =
        RecurringExpenseResponse(
            id           = id,
            title        = title,
            amount       = amount,
            categoryId   = category.id,
            categoryName = category.name,
            frequency    = frequency,
            nextDueDate  = nextDueDate,
            endDate      = endDate,
            isActive     = isActive,
            note         = note,
            createdAt    = createdAt
        )
}
