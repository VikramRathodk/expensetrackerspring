package com.devvikram.expensetracker.expensetracker.models


import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime
import java.util.Optional


@Entity
@Table(name = "expenses")
data class Expense(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @field:NotBlank(message = "Title is required")
    val title: String,

    @Column(nullable = false)
    @field:Positive(message = "Amount must be greater than 0")
    val amount: Double,

    /* ================= CATEGORY RELATION ================= */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: Category,

    /* ================= USER OWNERSHIP ================= */

    @Column(nullable = false)
    val userId: Long,

    val note: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
