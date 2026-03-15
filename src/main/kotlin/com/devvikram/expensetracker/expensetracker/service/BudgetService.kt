package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.dto.request.CreateBudgetRequest
import com.devvikram.expensetracker.expensetracker.dto.request.UpdateBudgetRequest
import com.devvikram.expensetracker.expensetracker.dto.response.BudgetResponse
import com.devvikram.expensetracker.expensetracker.dto.response.BudgetStatusResponse
import com.devvikram.expensetracker.expensetracker.entity.Budget
import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import com.devvikram.expensetracker.expensetracker.enums.BudgetPeriod
import com.devvikram.expensetracker.expensetracker.enums.NotificationType
import com.devvikram.expensetracker.expensetracker.exceptions.ResourceNotFoundException
import com.devvikram.expensetracker.expensetracker.repository.BudgetRepository
import com.devvikram.expensetracker.expensetracker.repository.CategoryRepository
import com.devvikram.expensetracker.expensetracker.repository.ExpenseRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class BudgetService(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val auditLogService: AuditLogService,
    private val notificationService: NotificationService
) {

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    fun createBudget(userId: Long, request: CreateBudgetRequest): BudgetResponse {
        val category = request.categoryId?.let {
            categoryRepository.findById(it).orElseThrow {
                ResourceNotFoundException("Category with id $it not found")
            }
        }

        val budget = Budget(
            userId = userId,
            category = category,
            amount = request.amount,
            period = request.period,
            startDate = request.startDate,
            endDate = request.endDate,
            alertThreshold = request.alertThreshold
        )
        val saved = budgetRepository.save(budget)
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.BUDGET_CREATED,
            entityType = "Budget",
            entityId   = saved.id,
            newValue   = toResponse(saved, userId)
        )
        return toResponse(saved, userId)
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    fun getAllBudgets(userId: Long): List<BudgetResponse> =
        budgetRepository
            .findByUserIdAndIsActiveTrueAndDeletedAtIsNull(userId)
            .map { toResponse(it, userId) }

    fun getBudgetStatus(userId: Long, budgetId: Long): BudgetStatusResponse {
        val budget = budgetRepository.findByIdAndUserIdAndDeletedAtIsNull(budgetId, userId)
            ?: throw ResourceNotFoundException("Budget with id $budgetId not found")

        val spent = getSpentForPeriod(userId, budget)
        val remaining = (budget.amount - spent).coerceAtLeast(0.0)
        val percentUsed = if (budget.amount > 0) (spent / budget.amount) * 100 else 0.0

        return BudgetStatusResponse(
            id = budget.id,
            categoryId = budget.category?.id,
            categoryName = budget.category?.name,
            limit = budget.amount,
            spent = spent,
            remaining = remaining,
            percentUsed = percentUsed,
            isOverBudget = spent >= budget.amount,
            isNearLimit = percentUsed >= budget.alertThreshold * 100
        )
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    fun updateBudget(userId: Long, budgetId: Long, request: UpdateBudgetRequest): BudgetResponse {
        val budget = budgetRepository.findByIdAndUserIdAndDeletedAtIsNull(budgetId, userId)
            ?: throw ResourceNotFoundException("Budget with id $budgetId not found")

        val updated = budget.copy(
            amount = request.amount ?: budget.amount,
            alertThreshold = request.alertThreshold ?: budget.alertThreshold,
            endDate = request.endDate ?: budget.endDate,
            isActive = request.isActive ?: budget.isActive
        )
        val saved = budgetRepository.save(updated)
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.BUDGET_UPDATED,
            entityType = "Budget",
            entityId   = budgetId,
            oldValue   = toResponse(budget, userId),
            newValue   = toResponse(saved, userId)
        )
        return toResponse(saved, userId)
    }

    // ── Delete (soft) ─────────────────────────────────────────────────────────

    @Transactional
    fun deleteBudget(userId: Long, budgetId: Long) {
        val budget = budgetRepository.findByIdAndUserIdAndDeletedAtIsNull(budgetId, userId)
            ?: throw ResourceNotFoundException("Budget with id $budgetId not found")

        budgetRepository.save(budget.copy(deletedAt = LocalDateTime.now()))
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.BUDGET_DELETED,
            entityType = "Budget",
            entityId   = budgetId,
            oldValue   = toResponse(budget, userId)
        )
    }

    // ── Budget check (called from ExpenseService) ─────────────────────────────

    /**
     * Call this inside ExpenseService.createExpense() BEFORE saving the expense.
     *
     * Example usage in ExpenseService:
     *   val check = budgetService.checkBudgetOnExpense(userId, categoryId, amount)
     *   if (check.shouldBlock) throw BadRequestException(check.warnings.joinToString(". "))
     */
    fun checkBudgetOnExpense(userId: Long, categoryId: Long, expenseAmount: Double): BudgetCheckResult {
        val categoryBudgets = budgetRepository
            .findByUserIdAndCategoryIdAndIsActiveTrueAndDeletedAtIsNull(userId, categoryId)

        val overallBudgets = budgetRepository
            .findByUserIdAndCategoryIsNullAndIsActiveTrueAndDeletedAtIsNull(userId)

        val warnings = mutableListOf<String>()
        var shouldBlock = false

        for (budget in categoryBudgets + overallBudgets) {
            val currentSpent = getSpentForPeriod(userId, budget)
            val newSpent = currentSpent + expenseAmount
            val percentUsed = if (budget.amount > 0) newSpent / budget.amount else 0.0
            val label = if (budget.category != null) "category" else "overall"

            when {
                newSpent >= budget.amount -> {
                    shouldBlock = true
                    val msg = "This expense exceeds your $label budget of ₹${budget.amount}."
                    warnings.add(msg)
                    notificationService.send(
                        userId     = userId,
                        title      = "Budget Exceeded",
                        message    = msg,
                        type       = NotificationType.BUDGET_EXCEEDED,
                        entityType = "Budget",
                        entityId   = budget.id
                    )
                }
                percentUsed >= budget.alertThreshold -> {
                    val msg = "You've used ${(percentUsed * 100).toInt()}% of your $label budget of ₹${budget.amount}."
                    warnings.add(msg)
                    notificationService.send(
                        userId     = userId,
                        title      = "Budget Alert",
                        message    = msg,
                        type       = NotificationType.BUDGET_ALERT,
                        entityType = "Budget",
                        entityId   = budget.id
                    )
                }
            }
        }

        return BudgetCheckResult(shouldBlock = shouldBlock, warnings = warnings)
    }

    // ── Scheduled reset ───────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    fun resetPeriodicBudgets() {
        val today = LocalDate.now()

        budgetRepository.findAllActiveBudgets(today).forEach { budget ->
            val nextPeriodStart = when (budget.period) {
                BudgetPeriod.DAILY   -> budget.startDate.plusDays(1)
                BudgetPeriod.WEEKLY  -> budget.startDate.plusWeeks(1)
                BudgetPeriod.MONTHLY -> budget.startDate.plusMonths(1)
                BudgetPeriod.YEARLY  -> budget.startDate.plusYears(1)
            }
            if (!today.isBefore(nextPeriodStart)) {
                budgetRepository.save(budget.copy(startDate = today))
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun getSpentForPeriod(userId: Long, budget: Budget): Double {
        val from = budget.startDate
        val to = when (budget.period) {
            BudgetPeriod.DAILY   -> from.plusDays(1)
            BudgetPeriod.WEEKLY  -> from.plusWeeks(1)
            BudgetPeriod.MONTHLY -> from.plusMonths(1)
            BudgetPeriod.YEARLY  -> from.plusYears(1)
        }
        return if (budget.category != null) {
            expenseRepository.sumAmountByUserIdAndCategoryIdAndDateBetween(
                userId, budget.category!!.id, from, to
            ) ?: 0.0
        } else {
            expenseRepository.sumAmountByUserIdAndDateBetween(userId, from, to) ?: 0.0
        }
    }

    private fun toResponse(budget: Budget, userId: Long): BudgetResponse {
        val spent = getSpentForPeriod(userId, budget)
        return BudgetResponse(
            id = budget.id,
            categoryId = budget.category?.id,
            categoryName = budget.category?.name,
            amount = budget.amount,
            period = budget.period,
            startDate = budget.startDate,
            endDate = budget.endDate,
            alertThreshold = budget.alertThreshold,
            isActive = budget.isActive,
            spent = spent,
            remaining = (budget.amount - spent).coerceAtLeast(0.0),
            percentUsed = if (budget.amount > 0) (spent / budget.amount) * 100 else 0.0,
            createdAt = budget.createdAt
        )
    }
}