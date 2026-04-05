package com.devvikram.expensetracker.expensetracker.repository


import com.devvikram.expensetracker.expensetracker.entity.Expense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ExpenseRepository : JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    fun findByUserId(userId: Long): List<Expense>

    fun existsByCategoryId(categoryId: Long): Boolean

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0.0)
        FROM Expense e
        WHERE e.userId = :userId
          AND e.category.id = :categoryId
          AND CAST(e.createdAt AS LocalDate) >= :from
          AND CAST(e.createdAt AS LocalDate) < :to
    """)
    fun sumAmountByUserIdAndCategoryIdAndDateBetween(
        @Param("userId") userId: Long,
        @Param("categoryId") categoryId: Long,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate
    ): Double?

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0.0)
        FROM Expense e
        WHERE e.userId = :userId
          AND CAST(e.createdAt AS LocalDate) >= :from
          AND CAST(e.createdAt AS LocalDate) < :to
    """)
    fun sumAmountByUserIdAndDateBetween(
        @Param("userId") userId: Long,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate
    ): Double?
}