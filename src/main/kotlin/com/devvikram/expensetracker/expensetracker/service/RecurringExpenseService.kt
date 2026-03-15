package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.dto.request.CreateRecurringExpenseRequest
import com.devvikram.expensetracker.expensetracker.dto.request.ExpenseRequest
import com.devvikram.expensetracker.expensetracker.dto.request.UpdateRecurringExpenseRequest
import com.devvikram.expensetracker.expensetracker.dto.response.RecurringExpenseResponse
import com.devvikram.expensetracker.expensetracker.entity.RecurringExpense
import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import com.devvikram.expensetracker.expensetracker.enums.NotificationType
import com.devvikram.expensetracker.expensetracker.enums.RecurringFrequency
import com.devvikram.expensetracker.expensetracker.exceptions.ResourceNotFoundException
import com.devvikram.expensetracker.expensetracker.repository.CategoryRepository
import com.devvikram.expensetracker.expensetracker.repository.RecurringExpenseRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class RecurringExpenseService(
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseService: ExpenseService,        // routes through ExpenseService → triggers budget check
    private val auditLogService: AuditLogService,
    private val notificationService: NotificationService
) {

    private val log = LoggerFactory.getLogger(RecurringExpenseService::class.java)

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    fun createRecurringExpense(
        userId: Long,
        request: CreateRecurringExpenseRequest
    ): RecurringExpenseResponse {
        val category = categoryRepository.findById(request.categoryId).orElseThrow {
            ResourceNotFoundException("Category with id ${request.categoryId} not found")
        }

        val recurring = RecurringExpense(
            userId = userId,
            title = request.title,
            amount = request.amount,
            category = category,
            frequency = request.frequency,
            nextDueDate = request.startDate,
            endDate = request.endDate,
            note = request.note
        )
        val saved = recurringExpenseRepository.save(recurring)
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.RECURRING_EXPENSE_CREATED,
            entityType = "RecurringExpense",
            entityId   = saved.id,
            newValue   = toResponse(saved)
        )
        return toResponse(saved)
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    fun getAllRecurringExpenses(userId: Long): List<RecurringExpenseResponse> =
        recurringExpenseRepository
            .findByUserIdAndIsActiveTrueAndDeletedAtIsNull(userId)
            .map { toResponse(it) }

    fun getRecurringExpenseById(userId: Long, id: Long): RecurringExpenseResponse {
        val recurring = recurringExpenseRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
            ?: throw ResourceNotFoundException("Recurring expense with id $id not found")
        return toResponse(recurring)
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    fun updateRecurringExpense(
        userId: Long,
        id: Long,
        request: UpdateRecurringExpenseRequest
    ): RecurringExpenseResponse {
        val recurring = recurringExpenseRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
            ?: throw ResourceNotFoundException("Recurring expense with id $id not found")

        val category = if (request.categoryId != null) {
            categoryRepository.findById(request.categoryId).orElseThrow {
                ResourceNotFoundException("Category with id ${request.categoryId} not found")
            }
        } else recurring.category

        val updated = recurring.copy(
            title = request.title ?: recurring.title,
            amount = request.amount ?: recurring.amount,
            category = category,
            frequency = request.frequency ?: recurring.frequency,
            nextDueDate = request.nextDueDate ?: recurring.nextDueDate,
            endDate = request.endDate ?: recurring.endDate,
            isActive = request.isActive ?: recurring.isActive,
            note = request.note ?: recurring.note
        )
        val saved = recurringExpenseRepository.save(updated)
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.RECURRING_EXPENSE_UPDATED,
            entityType = "RecurringExpense",
            entityId   = id,
            oldValue   = toResponse(recurring),
            newValue   = toResponse(saved)
        )
        return toResponse(saved)
    }

    // ── Soft Delete ───────────────────────────────────────────────────────────

    @Transactional
    fun deleteRecurringExpense(userId: Long, id: Long) {
        val recurring = recurringExpenseRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
            ?: throw ResourceNotFoundException("Recurring expense with id $id not found")
        recurringExpenseRepository.save(recurring.copy(deletedAt = LocalDateTime.now()))
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.RECURRING_EXPENSE_DELETED,
            entityType = "RecurringExpense",
            entityId   = id,
            oldValue   = toResponse(recurring)
        )
    }

    // ── Daily Scheduler ───────────────────────────────────────────────────────

    /**
     * Runs every day at 00:05 (5 minutes after midnight, after budget reset runs at 00:00).
     * Finds all recurring expenses due today, auto-creates an Expense for each,
     * then advances nextDueDate to the next period.
     *
     * Each entry is processed independently — one failure does NOT stop the rest.
     */
    @Scheduled(cron = "0 5 0 * * *")
    fun processRecurringExpenses() {
        val today = LocalDate.now()
        val dueEntries = recurringExpenseRepository.findAllDueToday(today)

        log.info("Recurring expense scheduler: found ${dueEntries.size} entries due on $today")

        for (recurring in dueEntries) {
            try {
                processSingleRecurringExpense(recurring, today)
            } catch (ex: Exception) {
                // Skip and continue — one failure must not block other users' expenses
                log.error(
                    "Failed to process recurring expense id=${recurring.id} " +
                            "for userId=${recurring.userId}: ${ex.message}", ex
                )
            }
        }
    }

    @Transactional
    fun processSingleRecurringExpense(recurring: RecurringExpense, today: LocalDate) {
        // 1. Auto-create the expense via ExpenseService (triggers budget check)
        val expenseRequest = ExpenseRequest(
            title = recurring.title,
            amount = recurring.amount,
            categoryId = recurring.category.id,
            userId = recurring.userId,
            note = "[Auto] ${recurring.note ?: recurring.title}"
        )
        expenseService.createExpense(expenseRequest)

        // 2. Advance nextDueDate to the next period
        val nextDueDate = when (recurring.frequency) {
            RecurringFrequency.DAILY   -> today.plusDays(1)
            RecurringFrequency.WEEKLY  -> today.plusWeeks(1)
            RecurringFrequency.MONTHLY -> today.plusMonths(1)
            RecurringFrequency.YEARLY  -> today.plusYears(1)
        }

        // 3. Deactivate if past end date
        val isStillActive = recurring.endDate == null || nextDueDate <= recurring.endDate

        recurringExpenseRepository.save(
            recurring.copy(
                nextDueDate = nextDueDate,
                isActive = isStillActive
            )
        )

        // Notify the user that the recurring expense was auto-processed
        notificationService.send(
            userId     = recurring.userId,
            title      = "Recurring Expense Processed",
            message    = "₹${recurring.amount} for \"${recurring.title}\" was auto-deducted today. Next due: $nextDueDate.",
            type       = NotificationType.RECURRING_EXPENSE_DUE,
            entityType = "RecurringExpense",
            entityId   = recurring.id
        )

        auditLogService.log(
            userId     = recurring.userId,
            action     = AuditAction.RECURRING_EXPENSE_PROCESSED,
            entityType = "RecurringExpense",
            entityId   = recurring.id,
            newValue   = mapOf(
                "processedOn"  to today.toString(),
                "nextDueDate"  to nextDueDate.toString(),
                "isStillActive" to isStillActive
            )
        )

        log.info(
            "Recurring expense id=${recurring.id} processed. " +
                    "Next due: $nextDueDate. Still active: $isStillActive"
        )
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun toResponse(recurring: RecurringExpense) = RecurringExpenseResponse(
        id = recurring.id,
        title = recurring.title,
        amount = recurring.amount,
        categoryId = recurring.category.id,
        categoryName = recurring.category.name,
        frequency = recurring.frequency,
        nextDueDate = recurring.nextDueDate,
        endDate = recurring.endDate,
        isActive = recurring.isActive,
        note = recurring.note,
        createdAt = recurring.createdAt
    )
}