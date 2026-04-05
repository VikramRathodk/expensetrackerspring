package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.dto.request.ExpenseRequest
import com.devvikram.expensetracker.expensetracker.entity.Category
import com.devvikram.expensetracker.expensetracker.entity.Expense
import com.devvikram.expensetracker.expensetracker.exceptions.BadRequestException
import com.devvikram.expensetracker.expensetracker.repository.CategoryRepository
import com.devvikram.expensetracker.expensetracker.repository.ExpenseRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExpenseServiceTest {

    private val expenseRepository: ExpenseRepository = mock()
    private val categoryRepository: CategoryRepository = mock()
    private val budgetService: BudgetService = mock()
    private val auditLogService: AuditLogService = mock()

    private lateinit var expenseService: ExpenseService

    private val category = Category(id = 1L, name = "Food", isGlobal = true)
    private val expense = Expense(id = 10L, title = "Lunch", amount = 200.0, category = category, userId = 1L)

    @BeforeEach
    fun setUp() {
        expenseService = ExpenseService(expenseRepository, categoryRepository, budgetService, auditLogService)
    }

    // ── createExpense ──────────────────────────────────────────────────────────

    @Test
    fun `createExpense - success returns saved expense response`() {
        val request = ExpenseRequest(title = "Lunch", amount = 200.0, categoryId = 1L, userId = 1L)
        val noBlock = BudgetCheckResult(shouldBlock = false, warnings = emptyList())

        whenever(budgetService.checkBudgetOnExpense(1L, 1L, 200.0)).thenReturn(noBlock)
        whenever(categoryRepository.findById(1L)).thenReturn(Optional.of(category))
        whenever(expenseRepository.save(any())).thenReturn(expense)

        val result = expenseService.createExpense(request)

        assertEquals("Lunch", result.title)
        assertEquals(200.0, result.amount)
        verify(auditLogService).log(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `createExpense - budget block throws BadRequestException`() {
        val request = ExpenseRequest(title = "Dinner", amount = 5000.0, categoryId = 1L, userId = 1L)
        val blocked = BudgetCheckResult(shouldBlock = true, warnings = listOf("Exceeds budget"))

        whenever(budgetService.checkBudgetOnExpense(1L, 1L, 5000.0)).thenReturn(blocked)

        assertThrows<BadRequestException> {
            expenseService.createExpense(request)
        }
        verify(expenseRepository, never()).save(any())
    }

    // ── getExpenseById ────────────────────────────────────────────────────────

    @Test
    fun `getExpenseById - returns expense when userId matches`() {
        whenever(expenseRepository.findById(10L)).thenReturn(Optional.of(expense))

        val result = expenseService.getExpenseById(10L, 1L)

        assertEquals("Lunch", result?.title)
    }

    @Test
    fun `getExpenseById - returns null when userId does not match`() {
        whenever(expenseRepository.findById(10L)).thenReturn(Optional.of(expense))

        val result = expenseService.getExpenseById(10L, 99L)

        assertNull(result)
    }

    @Test
    fun `getExpenseById - returns null when expense not found`() {
        whenever(expenseRepository.findById(99L)).thenReturn(Optional.empty())

        val result = expenseService.getExpenseById(99L, 1L)

        assertNull(result)
    }

    // ── updateExpense ─────────────────────────────────────────────────────────

    @Test
    fun `updateExpense - success updates and logs`() {
        val request = ExpenseRequest(title = "Dinner", amount = 300.0, categoryId = 1L, userId = 1L)
        val updatedExpense = expense.copy(title = "Dinner", amount = 300.0)

        whenever(expenseRepository.findById(10L)).thenReturn(Optional.of(expense))
        whenever(categoryRepository.findById(1L)).thenReturn(Optional.of(category))
        whenever(expenseRepository.save(any())).thenReturn(updatedExpense)

        val result = expenseService.updateExpense(10L, 1L, request)

        assertEquals("Dinner", result?.title)
        assertEquals(300.0, result?.amount)
        verify(auditLogService).log(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `updateExpense - returns null when expense not found for user`() {
        val request = ExpenseRequest(title = "X", amount = 1.0, categoryId = 1L, userId = 99L)

        whenever(expenseRepository.findById(10L)).thenReturn(Optional.of(expense))

        val result = expenseService.updateExpense(10L, 99L, request)

        assertNull(result)
        verify(expenseRepository, never()).save(any())
    }

    // ── deleteExpense ─────────────────────────────────────────────────────────

    @Test
    fun `deleteExpense - success deletes and logs`() {
        whenever(expenseRepository.findById(10L)).thenReturn(Optional.of(expense))

        val result = expenseService.deleteExpense(10L, 1L)

        assertTrue(result)
        verify(expenseRepository).deleteById(10L)
        verify(auditLogService).log(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `deleteExpense - returns false when userId does not match`() {
        whenever(expenseRepository.findById(10L)).thenReturn(Optional.of(expense))

        val result = expenseService.deleteExpense(10L, 99L)

        assertTrue(!result)
        verify(expenseRepository, never()).deleteById(any())
    }
}
