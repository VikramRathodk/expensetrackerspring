package com.devvikram.expensetracker.expensetracker.entity

import com.devvikram.expensetracker.expensetracker.enums.RecurringFrequency
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "recurring_expenses")
data class RecurringExpense(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val amount: Double,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: Category,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val frequency: RecurringFrequency,

    @Column(name = "next_due_date", nullable = false)
    val nextDueDate: LocalDate,

    @Column(name = "end_date")
    val endDate: LocalDate? = null,         // null = runs forever

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column
    val note: String? = null,

    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)