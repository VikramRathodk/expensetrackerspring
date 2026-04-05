package com.devvikram.expensetracker.expensetracker.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
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

    val createdAt: LocalDateTime = LocalDateTime.now(),

    /* ================= CURRENCY ================= */

    /** ISO 4217 code for the currency this expense was recorded in (e.g. USD, EUR). */
    @Column(nullable = false, length = 3)
    val currency: String = "INR",

    /** Amount converted to the user's base currency at the time of creation. */
    @Column(name = "amount_in_base", nullable = false)
    val amountInBase: Double = 0.0,

    /* ================= TAGS ================= */

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "expense_tags",
        joinColumns = [JoinColumn(name = "expense_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")]
    )
    val tags: MutableList<Tag> = mutableListOf()
)