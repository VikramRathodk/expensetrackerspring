package com.devvikram.expensetracker.expensetracker.repository

import com.devvikram.expensetracker.expensetracker.entity.AuditLog
import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long> {

    // All logs — admin overview
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<AuditLog>

    // Logs for a single user
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<AuditLog>

    // Full history for a specific entity (e.g. all changes to Expense id=5)
    fun findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        entityType: String,
        entityId: Long
    ): List<AuditLog>

    // Filter by action type
    fun findByActionOrderByCreatedAtDesc(action: AuditAction, pageable: Pageable): Page<AuditLog>
}
