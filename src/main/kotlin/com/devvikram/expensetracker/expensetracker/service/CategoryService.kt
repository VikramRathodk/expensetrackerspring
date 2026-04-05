package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.dto.request.CategoryRequest
import com.devvikram.expensetracker.expensetracker.entity.Category
import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import com.devvikram.expensetracker.expensetracker.exceptions.BadRequestException
import com.devvikram.expensetracker.expensetracker.exceptions.ConflictException
import com.devvikram.expensetracker.expensetracker.exceptions.ResourceNotFoundException
import com.devvikram.expensetracker.expensetracker.repository.BudgetRepository
import com.devvikram.expensetracker.expensetracker.repository.CategoryRepository
import com.devvikram.expensetracker.expensetracker.repository.ExpenseRepository
import com.devvikram.expensetracker.expensetracker.repository.RecurringExpenseRepository
import org.springframework.stereotype.Service

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val auditLogService: AuditLogService
) {

    /* ======================================================
     *                  GLOBAL CATEGORIES (ADMIN)
     * ======================================================
     * Rules:
     * - Visible to all users
     * - Only ADMIN/SUPER_ADMIN can create/update/delete
     * - userId stores which admin created the category
     */

    fun listGlobalCategories(): List<Category> =
        categoryRepository.findAll().filter { it.isGlobal }

    fun addGlobalCategory(request: CategoryRequest, adminId: Long): Category {
        if (categoryRepository.existsByNameIgnoreCaseAndIsGlobalTrue(request.name)) {
            throw ConflictException("Global category '${request.name}' already exists")
        }

        val saved = categoryRepository.save(
            Category(name = request.name, description = request.description, isGlobal = true, userId = adminId)
        )

        auditLogService.log(
            userId     = adminId,
            action     = AuditAction.CATEGORY_CREATED,
            entityType = "Category",
            entityId   = saved.id,
            newValue   = mapOf("name" to saved.name, "isGlobal" to true)
        )

        return saved
    }

    fun updateGlobalCategory(categoryId: Long, request: CategoryRequest, adminId: Long): Category {
        val existing = categoryRepository.findById(categoryId)
            .orElseThrow { ResourceNotFoundException("Category not found") }

        if (!existing.isGlobal) throw ConflictException("Only global categories can be updated here")

        if (existing.name.lowercase() != request.name.lowercase() &&
            categoryRepository.existsByNameIgnoreCaseAndIsGlobalTrue(request.name)
        ) {
            throw ConflictException("Global category '${request.name}' already exists")
        }

        val updated = categoryRepository.save(
            existing.copy(name = request.name, description = request.description)
        )

        auditLogService.log(
            userId     = adminId,
            action     = AuditAction.CATEGORY_UPDATED,
            entityType = "Category",
            entityId   = updated.id,
            oldValue   = mapOf("name" to existing.name),
            newValue   = mapOf("name" to updated.name)
        )

        return updated
    }

    fun deleteGlobalCategory(categoryId: Long, adminId: Long) {
        val existing = categoryRepository.findById(categoryId)
            .orElseThrow { ResourceNotFoundException("Category not found") }

        if (!existing.isGlobal) throw ConflictException("Only global categories can be deleted here")

        checkCategoryNotInUse(categoryId)

        categoryRepository.delete(existing)

        auditLogService.log(
            userId     = adminId,
            action     = AuditAction.CATEGORY_DELETED,
            entityType = "Category",
            entityId   = categoryId,
            oldValue   = mapOf("name" to existing.name, "isGlobal" to true)
        )
    }

    /* ======================================================
     *                  USER CATEGORIES
     * ======================================================
     * Rules:
     * - Belong to a single user
     * - isGlobal = false
     * - User can manage ONLY their own categories
     */

    fun addUserCategory(request: CategoryRequest, userId: Long): Category {
        if (categoryRepository.existsByNameIgnoreCaseAndUserId(request.name, userId)) {
            throw ConflictException("Category '${request.name}' already exists for this user")
        }

        val saved = categoryRepository.save(
            Category(name = request.name, description = request.description, isGlobal = false, userId = userId)
        )

        auditLogService.log(
            userId     = userId,
            action     = AuditAction.CATEGORY_CREATED,
            entityType = "Category",
            entityId   = saved.id,
            newValue   = mapOf("name" to saved.name, "isGlobal" to false)
        )

        return saved
    }

    fun updateUserCategory(categoryId: Long, request: CategoryRequest, userId: Long): Category {
        val existing = categoryRepository.findById(categoryId)
            .orElseThrow { ResourceNotFoundException("Category not found") }

        if (existing.isGlobal) throw ConflictException("Global categories cannot be modified by users")
        if (existing.userId != userId) throw ConflictException("You are not allowed to update this category")

        if (existing.name.lowercase() != request.name.lowercase() &&
            categoryRepository.existsByNameIgnoreCaseAndUserId(request.name, userId)
        ) {
            throw ConflictException("Category '${request.name}' already exists for this user")
        }

        val updated = categoryRepository.save(
            existing.copy(name = request.name, description = request.description)
        )

        auditLogService.log(
            userId     = userId,
            action     = AuditAction.CATEGORY_UPDATED,
            entityType = "Category",
            entityId   = updated.id,
            oldValue   = mapOf("name" to existing.name),
            newValue   = mapOf("name" to updated.name)
        )

        return updated
    }

    fun getCategoriesForUser(userId: Long): List<Category> =
        categoryRepository.findByUserIdOrIsGlobalTrue(userId)

    fun deleteUserCategory(categoryId: Long, userId: Long) {
        val existing = categoryRepository.findById(categoryId)
            .orElseThrow { ResourceNotFoundException("Category not found") }

        if (existing.isGlobal) throw ConflictException("Global categories cannot be deleted by users")
        if (existing.userId != userId) throw ConflictException("You are not allowed to delete this category")

        checkCategoryNotInUse(categoryId)

        categoryRepository.delete(existing)

        auditLogService.log(
            userId     = userId,
            action     = AuditAction.CATEGORY_DELETED,
            entityType = "Category",
            entityId   = categoryId,
            oldValue   = mapOf("name" to existing.name, "isGlobal" to false)
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun checkCategoryNotInUse(categoryId: Long) {
        if (expenseRepository.existsByCategoryId(categoryId))
            throw BadRequestException("Cannot delete: category is referenced by existing expenses")
        if (budgetRepository.existsByCategoryIdAndDeletedAtIsNull(categoryId))
            throw BadRequestException("Cannot delete: category is referenced by active budgets")
        if (recurringExpenseRepository.existsByCategoryIdAndDeletedAtIsNull(categoryId))
            throw BadRequestException("Cannot delete: category is referenced by active recurring expenses")
    }
}
