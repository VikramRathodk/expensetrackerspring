package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.dto.request.CreateBudgetRequest
import com.devvikram.expensetracker.expensetracker.entity.Budget
import com.devvikram.expensetracker.expensetracker.enums.BudgetPeriod
import com.devvikram.expensetracker.expensetracker.exceptions.ResourceNotFoundException
import com.devvikram.expensetracker.expensetracker.repository.BudgetRepository
import com.devvikram.expensetracker.expensetracker.repository.CategoryRepository
import com.devvikram.expensetracker.expensetracker.repository.ExpenseRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BudgetServiceTest {

    private val budgetRepository: BudgetRepository = mock()
    private val expenseRepository: ExpenseRepository = mock()
    private val categoryRepository: CategoryRepository = mock()
    private val auditLogService: AuditLogService = mock()
    private val notificationService: NotificationService = mock()

    private lateinit var budgetService: BudgetService

    private val today = LocalDate.now()
    private val budget = Budget(
        id = 1L,
        userId = 1L,
        category = null,
        amount = 5000.0,
        period = BudgetPeriod.MONTHLY,
        startDate = today,
        alertThreshold = 0.80
    )

    @BeforeEach
    fun setUp() {
        budgetService = BudgetService(
            budgetRepository, expenseRepository, categoryRepository,
            auditLogService, notificationService
        )
    }

    // ── createBudget ──────────────────────────────────────────────────────────

    @Test
    fun `createBudget - overall budget (no category) saves and logs`() {
        val request = CreateBudgetRequest(
            categoryId = null,
            amount = 5000.0,
            period = BudgetPeriod.MONTHLY,
            startDate = today
        )
        whenever(budgetRepository.save(any())).thenReturn(budget)
        whenever(expenseRepository.sumAmountByUserIdAndDateBetween(any(), any(), any())).thenReturn(0.0)

        val result = budgetService.createBudget(1L, request)

        assertNotNull(result)
        assertEquals(5000.0, result.amount)
        verify(auditLogService).log(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    // ── getAllBudgets ─────────────────────────────────────────────────────────

    @Test
    fun `getAllBudgets - returns mapped responses`() {
        whenever(budgetRepository.findByUserIdAndIsActiveTrueAndDeletedAtIsNull(1L))
            .thenReturn(listOf(budget))
        whenever(expenseRepository.sumAmountByUserIdAndDateBetween(any(), any(), any())).thenReturn(1000.0)

        val result = budgetService.getAllBudgets(1L)

        assertEquals(1, result.size)
        assertEquals(5000.0, result[0].amount)
        assertEquals(1000.0, result[0].spent)
    }

    // ── deleteBudget ──────────────────────────────────────────────────────────

    @Test
    fun `deleteBudget - soft-deletes budget and logs`() {
        whenever(budgetRepository.findByIdAndUserIdAndDeletedAtIsNull(1L, 1L)).thenReturn(budget)
        whenever(budgetRepository.save(any())).thenReturn(budget.copy(deletedAt = java.time.LocalDateTime.now()))
        whenever(expenseRepository.sumAmountByUserIdAndDateBetween(any(), any(), any())).thenReturn(0.0)

        budgetService.deleteBudget(1L, 1L)

        verify(budgetRepository).save(argThat { deletedAt != null })
        verify(auditLogService).log(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `deleteBudget - throws ResourceNotFoundException when budget not found`() {
        whenever(budgetRepository.findByIdAndUserIdAndDeletedAtIsNull(99L, 1L)).thenReturn(null)

        assertThrows<ResourceNotFoundException> {
            budgetService.deleteBudget(1L, 99L)
        }
    }

    // ── checkBudgetOnExpense ──────────────────────────────────────────────────

    @Test
    fun `checkBudgetOnExpense - returns no-block when no budgets exist`() {
        whenever(budgetRepository.findByUserIdAndCategoryIdAndIsActiveTrueAndDeletedAtIsNull(any(), any()))
            .thenReturn(emptyList())
        whenever(budgetRepository.findByUserIdAndCategoryIsNullAndIsActiveTrueAndDeletedAtIsNull(any()))
            .thenReturn(emptyList())

        val result = budgetService.checkBudgetOnExpense(1L, 1L, 100.0)

        assertEquals(false, result.shouldBlock)
        assertEquals(0, result.warnings.size)
    }

    @Test
    fun `checkBudgetOnExpense - blocks when expense would exceed budget`() {
        val tightBudget = budget.copy(amount = 500.0)
        whenever(budgetRepository.findByUserIdAndCategoryIdAndIsActiveTrueAndDeletedAtIsNull(any(), any()))
            .thenReturn(emptyList())
        whenever(budgetRepository.findByUserIdAndCategoryIsNullAndIsActiveTrueAndDeletedAtIsNull(any()))
            .thenReturn(listOf(tightBudget))
        whenever(expenseRepository.sumAmountByUserIdAndDateBetween(any(), any(), any())).thenReturn(450.0)

        val result = budgetService.checkBudgetOnExpense(1L, 1L, 100.0)

        assertEquals(true, result.shouldBlock)
        assertEquals(1, result.warnings.size)
    }
}
