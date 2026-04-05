package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.dto.response.NotificationResponse
import com.devvikram.expensetracker.expensetracker.entity.Notification
import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import com.devvikram.expensetracker.expensetracker.enums.NotificationType
import com.devvikram.expensetracker.expensetracker.exceptions.ResourceNotFoundException
import com.devvikram.expensetracker.expensetracker.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val auditLogService: AuditLogService
) {
    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    // ── Internal API (called by other services) ───────────────────────────────

    /**
     * Creates and persists a notification. Safe to call from schedulers.
     * Failures are swallowed so they never interrupt the caller.
     */
    @Transactional
    fun send(
        userId: Long,
        title: String,
        message: String,
        type: NotificationType,
        entityType: String? = null,
        entityId: Long? = null
    ) {
        try {
            notificationRepository.save(
                Notification(
                    userId = userId,
                    title = title,
                    message = message,
                    type = type,
                    entityType = entityType,
                    entityId = entityId
                )
            )
        } catch (ex: Exception) {
            log.error("Failed to save notification for userId=$userId type=$type: ${ex.message}", ex)
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    fun getNotifications(userId: Long, page: Int, size: Int): Page<NotificationResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return notificationRepository
            .findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getUnreadNotifications(userId: Long): List<NotificationResponse> =
        notificationRepository
            .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
            .map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getUnreadCount(userId: Long): Long =
        notificationRepository.countByUserIdAndIsReadFalse(userId)

    // ── Mark as read ──────────────────────────────────────────────────────────

    @Transactional
    fun markAsRead(userId: Long, notificationId: Long): NotificationResponse {
        val notification = notificationRepository.findById(notificationId)
            .filter { it.userId == userId }
            .orElseThrow { ResourceNotFoundException("Notification with id $notificationId not found") }

        val updated = notificationRepository.save(notification.copy(isRead = true))
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.NOTIFICATION_READ,
            entityType = "Notification",
            entityId   = notificationId
        )
        return updated.toResponse()
    }

    @Transactional
    fun markAllAsRead(userId: Long): Int {
        val unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
        if (unread.isEmpty()) return 0
        notificationRepository.saveAll(unread.map { it.copy(isRead = true) })
        return unread.size
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    fun deleteNotification(userId: Long, notificationId: Long) {
        val notification = notificationRepository.findById(notificationId)
            .filter { it.userId == userId }
            .orElseThrow { ResourceNotFoundException("Notification with id $notificationId not found") }

        notificationRepository.delete(notification)
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.NOTIFICATION_DELETED,
            entityType = "Notification",
            entityId   = notificationId
        )
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun Notification.toResponse() = NotificationResponse(
        id         = id,
        title      = title,
        message    = message,
        type       = type,
        isRead     = isRead,
        entityType = entityType,
        entityId   = entityId,
        createdAt  = createdAt
    )
}
