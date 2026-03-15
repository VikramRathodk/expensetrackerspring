package com.devvikram.expensetracker.expensetracker.repository

import com.devvikram.expensetracker.expensetracker.entity.RecurringExpense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface RecurringExpenseRepository : JpaRepository<RecurringExpense, Long> {

    // All active, non-deleted recurring expenses for a user
    fun findByUserIdAndIsActiveTrueAndDeletedAtIsNull(userId: Long): List<RecurringExpense>

    // Ownership-safe single lookup
    fun findByIdAndUserIdAndDeletedAtIsNull(id: Long, userId: Long): RecurringExpense?

    // Used by the daily scheduler: find all due today
    // Includes entries where endDate is null (runs forever) OR endDate >= today
    @Query("""
        SELECT r FROM RecurringExpense r
        WHERE r.isActive = true
          AND r.deletedAt IS NULL
          AND r.nextDueDate <= :today
          AND (r.endDate IS NULL OR r.endDate >= :today)
    """)
    fun findAllDueToday(@Param("today") today: LocalDate): List<RecurringExpense>
}