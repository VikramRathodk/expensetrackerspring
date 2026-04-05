package com.devvikram.expensetracker.expensetracker.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Entity
@Table(
    name = "tags",
    uniqueConstraints = [UniqueConstraint(columnNames = ["name", "user_id"])]
)
data class Tag(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @field:NotBlank(message = "Tag name is required")
    @Column(nullable = false, length = 100)
    val name: String,

    @Column(nullable = false, length = 7)
    val color: String = "#6366f1",

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
