package com.devvikram.expensetracker.expensetracker.entity

import com.devvikram.expensetracker.expensetracker.enums.NotificationType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notifications")
data class Notification(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false, length = 150)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val message: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val type: NotificationType,

    @Column(name = "is_read", nullable = false)
    val isRead: Boolean = false,

    @Column(name = "entity_type", length = 50)
    val entityType: String? = null,

    @Column(name = "entity_id")
    val entityId: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
