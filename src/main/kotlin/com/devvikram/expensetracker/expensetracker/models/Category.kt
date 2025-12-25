package com.devvikram.expensetracker.expensetracker.models


import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Entity
@Table(
    name = "categories",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["name", "user_id"])
    ]
)
data class Category(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @field:NotBlank(message = "Category name is required")
    @Column(nullable = false)
    val name: String,

    val description: String? = null,

    @Column(nullable = false)
    val isGlobal: Boolean = false,

    @Column(name = "user_id")
    val userId: Long? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
