package com.devvikram.expensetracker.expensetracker.integration

import com.devvikram.expensetracker.expensetracker.dto.request.ExpenseRequest
import com.devvikram.expensetracker.expensetracker.entity.Category
import com.devvikram.expensetracker.expensetracker.entity.Expense
import com.devvikram.expensetracker.expensetracker.repository.CategoryRepository
import com.devvikram.expensetracker.expensetracker.repository.ExpenseRepository
import com.devvikram.expensetracker.expensetracker.service.AuditLogService
import com.devvikram.expensetracker.expensetracker.service.BudgetCheckResult
import com.devvikram.expensetracker.expensetracker.service.BudgetService
import com.devvikram.expensetracker.expensetracker.service.ExpenseService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import java.util.Optional

/**
 * Verifies that expense ownership is enforced: a user cannot read, update,
 * or delete another user's expenses even when both users exist.
 */
class ExpenseOwnershipIntegrationTest {

    private val expenseRepository: ExpenseRepository = mock()
    private val categoryRepository: CategoryRepository = mock()
    private val budgetService: BudgetService = mock()
    private val auditLogService: AuditLogService = mock()

    private lateinit var expenseService: ExpenseService

    private val category = Category(id = 1L, name = "Food", isGlobal = true)

    // Expense owned by user 1
    private val user1Expense = Expense(id = 42L, title = "User1 Lunch", amount = 100.0, category = category, userId = 1L)

    @BeforeEach
    fun setUp() {
        expenseService = ExpenseService(expenseRepository, categoryRepository, budgetService, auditLogService)
        whenever(expenseRepository.findById(42L)).thenReturn(Optional.of(user1Expense))
    }

    // ── Read isolation ────────────────────────────────────────────────────────

    @Test
    fun `user1 can read their own expense`() {
        val result = expenseService.getExpenseById(42L, userId = 1L)
        assertNotNull(result)
    }

    @Test
    fun `user2 cannot read user1's expense`() {
        val result = expenseService.getExpenseById(42L, userId = 2L)
        assertNull(result)
    }

    // ── Update isolation ──────────────────────────────────────────────────────

    @Test
    fun `user2 cannot update user1's expense`() {
        val request = ExpenseRequest(title = "Hacked", amount = 999.0, categoryId = 1L, userId = 2L)

        val result = expenseService.updateExpense(42L, userId = 2L, request = request)

        assertNull(result)
        verify(expenseRepository, never()).save(any())
    }

    @Test
    fun `user1 can update their own expense`() {
        val request = ExpenseRequest(title = "Updated Lunch", amount = 150.0, categoryId = 1L, userId = 1L)
        val updatedExpense = user1Expense.copy(title = "Updated Lunch", amount = 150.0)

        whenever(categoryRepository.findById(1L)).thenReturn(Optional.of(category))
        whenever(expenseRepository.save(any())).thenReturn(updatedExpense)

        val result = expenseService.updateExpense(42L, userId = 1L, request = request)

        assertNotNull(result)
        verify(expenseRepository).save(any())
    }

    // ── Delete isolation ──────────────────────────────────────────────────────

    @Test
    fun `user2 cannot delete user1's expense`() {
        val deleted = expenseService.deleteExpense(42L, userId = 2L)

        assert(!deleted)
        verify(expenseRepository, never()).deleteById(any())
    }

    @Test
    fun `user1 can delete their own expense`() {
        val deleted = expenseService.deleteExpense(42L, userId = 1L)

        assert(deleted)
        verify(expenseRepository).deleteById(42L)
    }
}
