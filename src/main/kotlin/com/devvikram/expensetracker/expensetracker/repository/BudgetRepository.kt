package com.devvikram.expensetracker.expensetracker.repository

import com.devvikram.expensetracker.expensetracker.entity.Budget
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface BudgetRepository : JpaRepository<Budget, Long> {

    // All active budgets for a user
    fun findByUserIdAndIsActiveTrueAndDeletedAtIsNull(userId: Long): List<Budget>

    // Active budgets scoped to a specific category
    fun findByUserIdAndCategoryIdAndIsActiveTrueAndDeletedAtIsNull(
        userId: Long,
        categoryId: Long
    ): List<Budget>

    // Overall (non-category) active budgets for a user
    fun findByUserIdAndCategoryIsNullAndIsActiveTrueAndDeletedAtIsNull(
        userId: Long
    ): List<Budget>

    fun existsByCategoryIdAndDeletedAtIsNull(categoryId: Long): Boolean

    // Ownership-safe single budget lookup
    fun findByIdAndUserIdAndDeletedAtIsNull(id: Long, userId: Long): Budget?

    // Used by reset scheduler — all active budgets within valid date range
    @Query("""
        SELECT b FROM Budget b
        WHERE b.isActive = true
          AND b.deletedAt IS NULL
          AND b.startDate <= :today
          AND (b.endDate IS NULL OR b.endDate >= :today)
    """)
    fun findAllActiveBudgets(@Param("today") today: LocalDate): List<Budget>
}