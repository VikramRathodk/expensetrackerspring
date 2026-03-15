package com.devvikram.expensetracker.expensetracker.entity

import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "audit_logs")
data class AuditLog(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val action: AuditAction,

    @Column(name = "entity_type", nullable = false, length = 50)
    val entityType: String,

    @Column(name = "entity_id")
    val entityId: Long? = null,

    @Column(name = "old_value", columnDefinition = "TEXT")
    val oldValue: String? = null,

    @Column(name = "new_value", columnDefinition = "TEXT")
    val newValue: String? = null,

    @Column(name = "ip_address", length = 45)     // 45 supports IPv6
    val ipAddress: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
