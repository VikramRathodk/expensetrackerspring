package com.devvikram.expensetracker.expensetracker.service


import com.devvikram.expensetracker.expensetracker.exceptions.ConflictException
import com.devvikram.expensetracker.expensetracker.exceptions.ResourceNotFoundException
import com.devvikram.expensetracker.expensetracker.models.Category
import com.devvikram.expensetracker.expensetracker.repository.CategoryRepository
import org.springframework.stereotype.Service

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    /* ======================================================
     *                  GLOBAL CATEGORIES (ADMIN)
     * ======================================================
     * Rules:
     * - Visible to all users
     * - Only ADMIN/System can create/update/delete
     * - userId is always 0 (system owned)
     */

    /* ---------- CREATE GLOBAL CATEGORY ---------- */
    fun addGlobalCategory(category: Category): Category {

        // Prevent duplicate global category names
        if (categoryRepository.existsByNameIgnoreCaseAndIsGlobalTrue(category.name)) {
            throw ConflictException("Global category '${category.name}' already exists")
        }

        // Save as system-owned global category
        return categoryRepository.save(
            category.copy(
                isGlobal = true,
                userId = 0L // 0 indicates system/admin ownership
            )
        )
    }

    /* ---------- UPDATE GLOBAL CATEGORY ---------- */
    fun updateGlobalCategory(
        categoryId: Long,
        updatedCategory: Category
    ): Category {

        val existing = categoryRepository.findById(categoryId)
            .orElseThrow { ResourceNotFoundException("Category not found") }

        // Ensure only global categories are updated here
        if (!existing.isGlobal) {
            throw ConflictException("Only global categories can be updated here")
        }

        // Prevent duplicate global category names
        if (
            existing.name.lowercase() != updatedCategory.name.lowercase() &&
            categoryRepository.existsByNameIgnoreCaseAndIsGlobalTrue(updatedCategory.name)
        ) {
            throw ConflictException("Global category '${updatedCategory.name}' already exists")
        }

        return categoryRepository.save(
            existing.copy(
                name = updatedCategory.name,
                description = updatedCategory.description
            )
        )
    }

    /* ---------- DELETE GLOBAL CATEGORY ---------- */
    fun deleteGlobalCategory(categoryId: Long) {

        val existing = categoryRepository.findById(categoryId)
            .orElseThrow { ResourceNotFoundException("Category not found") }

        // Safety check: only global categories allowed
        if (!existing.isGlobal) {
            throw ConflictException("Only global categories can be deleted")
        }

        categoryRepository.delete(existing)
    }

    /* ======================================================
     *                  USER CATEGORIES
     * ======================================================
     * Rules:
     * - Belong to a single user
     * - isGlobal = false
     * - User can manage ONLY their own categories
     */

    /* ---------- CREATE USER CATEGORY ---------- */
    fun addUserCategory(category: Category, userId: Long): Category {

        // Prevent duplicate category names for same user
        if (categoryRepository.existsByNameIgnoreCaseAndUserId(category.name, userId)) {
            throw ConflictException("Category '${category.name}' already exists for this user")
        }

        return categoryRepository.save(
            category.copy(
                userId = userId,
                isGlobal = false
            )
        )
    }

    /* ---------- UPDATE USER CATEGORY ---------- */
    fun updateUserCategory(
        categoryId: Long,
        updatedCategory: Category,
        userId: Long
    ): Category {

        val existing = categoryRepository.findById(categoryId)
            .orElseThrow { ResourceNotFoundException("Category not found") }

        // Block updates to global categories
        if (existing.isGlobal) {
            throw ConflictException("Global categories cannot be modified")
        }

        // Ensure user owns the category
        if (existing.userId != userId) {
            throw ConflictException("You are not allowed to update this category")
        }

        // Prevent duplicate names for same user
        if (
            existing.name.lowercase() != updatedCategory.name.lowercase() &&
            categoryRepository.existsByNameIgnoreCaseAndUserId(
                updatedCategory.name,
                userId
            )
        ) {
            throw ConflictException("Category '${updatedCategory.name}' already exists for this user")
        }

        return categoryRepository.save(
            existing.copy(
                name = updatedCategory.name,
                description = updatedCategory.description
            )
        )
    }

    /* ---------- READ USER + GLOBAL CATEGORIES ---------- */
    fun getCategoriesForUser(userId: Long): List<Category> =
    // User can see:
    // - All global categories
        // - Their own categories
        categoryRepository.findByUserIdOrIsGlobalTrue(userId)

    /* ---------- DELETE USER CATEGORY ---------- */
    fun deleteUserCategory(categoryId: Long, userId: Long) {

        val existing = categoryRepository.findById(categoryId)
            .orElseThrow { ResourceNotFoundException("Category not found") }

        // Global categories cannot be deleted by users
        if (existing.isGlobal) {
            throw ConflictException("Global categories cannot be deleted")
        }

        // Ensure user owns the category
        if (existing.userId != userId) {
            throw ConflictException("You are not allowed to delete this category")
        }

        categoryRepository.delete(existing)
    }
}


