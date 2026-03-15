package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.dto.response.AuditLogResponse
import com.devvikram.expensetracker.expensetracker.entity.AuditLog
import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import com.devvikram.expensetracker.expensetracker.repository.AuditLogRepository
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(AuditLogService::class.java)

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Records an audit event.
     * Safe to call from both HTTP handlers and @Scheduled jobs:
     * - In HTTP context: IP is extracted from the current request.
     * - In scheduler context: IP is stored as null (no request available).
     *
     * @param userId     The user who performed (or owns) the action.
     * @param action     The type of change that occurred.
     * @param entityType Simple class name of the affected entity (e.g. "Expense").
     * @param entityId   PK of the affected row; null for actions with no specific row.
     * @param oldValue   Snapshot of the entity BEFORE the change; null for creates.
     * @param newValue   Snapshot of the entity AFTER the change; null for deletes.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun log(
        userId: Long,
        action: AuditAction,
        entityType: String,
        entityId: Long? = null,
        oldValue: Any? = null,
        newValue: Any? = null
    ) {
        try {
            val ipAddress = resolveIpAddress()

            auditLogRepository.save(
                AuditLog(
                    userId     = userId,
                    action     = action,
                    entityType = entityType,
                    entityId   = entityId,
                    oldValue   = oldValue?.let { objectMapper.writeValueAsString(it) },
                    newValue   = newValue?.let { objectMapper.writeValueAsString(it) },
                    ipAddress  = ipAddress
                )
            )
        } catch (ex: Exception) {
            // Audit failure must never crash the main operation
            log.error("Failed to write audit log: action=$action entity=$entityType id=$entityId", ex)
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    fun getAllLogs(page: Int, size: Int): Page<AuditLogResponse> =
        auditLogRepository
            .findAllByOrderByCreatedAtDesc(pageRequest(page, size))
            .map { it.toResponse() }

    fun getLogsByUser(userId: Long, page: Int, size: Int): Page<AuditLogResponse> =
        auditLogRepository
            .findByUserIdOrderByCreatedAtDesc(userId, pageRequest(page, size))
            .map { it.toResponse() }

    fun getLogsByEntity(entityType: String, entityId: Long): List<AuditLogResponse> =
        auditLogRepository
            .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
            .map { it.toResponse() }

    fun getLogsByAction(action: AuditAction, page: Int, size: Int): Page<AuditLogResponse> =
        auditLogRepository
            .findByActionOrderByCreatedAtDesc(action, pageRequest(page, size))
            .map { it.toResponse() }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun resolveIpAddress(): String? =
        try {
            val attrs = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
            extractIp(attrs.request)
        } catch (_: Exception) {
            null   // scheduler / non-HTTP context
        }

    /** Handles X-Forwarded-For so proxied requests show the real client IP. */
    private fun extractIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        return if (!forwarded.isNullOrBlank()) forwarded.split(",").first().trim()
        else request.remoteAddr
    }

    private fun pageRequest(page: Int, size: Int) =
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

    private fun AuditLog.toResponse() = AuditLogResponse(
        id         = id,
        userId     = userId,
        action     = action,
        entityType = entityType,
        entityId   = entityId,
        oldValue   = oldValue,
        newValue   = newValue,
        ipAddress  = ipAddress,
        createdAt  = createdAt
    )
}
