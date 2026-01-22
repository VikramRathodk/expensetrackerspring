package com.devvikram.expensetracker.expensetracker.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

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