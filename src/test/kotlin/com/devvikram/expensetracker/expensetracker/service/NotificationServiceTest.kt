package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.entity.Notification
import com.devvikram.expensetracker.expensetracker.enums.NotificationType
import com.devvikram.expensetracker.expensetracker.exceptions.ResourceNotFoundException
import com.devvikram.expensetracker.expensetracker.repository.NotificationRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationServiceTest {

    private val notificationRepository: NotificationRepository = mock()
    private val auditLogService: AuditLogService = mock()

    private lateinit var notificationService: NotificationService

    private val notification = Notification(
        id = 1L,
        userId = 1L,
        title = "Budget Alert",
        message = "You've used 85% of your budget.",
        type = NotificationType.BUDGET_ALERT,
        isRead = false
    )

    @BeforeEach
    fun setUp() {
        notificationService = NotificationService(notificationRepository, auditLogService)
    }

    // ── send ──────────────────────────────────────────────────────────────────

    @Test
    fun `send - saves notification successfully`() {
        whenever(notificationRepository.save(any())).thenReturn(notification)

        notificationService.send(
            userId = 1L,
            title = "Budget Alert",
            message = "You've used 85% of your budget.",
            type = NotificationType.BUDGET_ALERT
        )

        verify(notificationRepository).save(any())
    }

    @Test
    fun `send - does not throw when repository throws`() {
        whenever(notificationRepository.save(any())).thenThrow(RuntimeException("DB error"))

        // Should not throw — failures are swallowed
        notificationService.send(1L, "Title", "Message", NotificationType.BUDGET_ALERT)
    }

    // ── markAsRead ────────────────────────────────────────────────────────────

    @Test
    fun `markAsRead - marks notification as read and logs`() {
        val readNotification = notification.copy(isRead = true)
        whenever(notificationRepository.findById(1L)).thenReturn(Optional.of(notification))
        whenever(notificationRepository.save(any())).thenReturn(readNotification)

        val result = notificationService.markAsRead(1L, 1L)

        assertTrue(result.isRead)
        verify(auditLogService).log(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `markAsRead - throws ResourceNotFoundException when notification belongs to different user`() {
        whenever(notificationRepository.findById(1L)).thenReturn(Optional.of(notification))

        assertThrows<ResourceNotFoundException> {
            notificationService.markAsRead(userId = 99L, notificationId = 1L)
        }
    }

    // ── deleteNotification ────────────────────────────────────────────────────

    @Test
    fun `deleteNotification - deletes and logs`() {
        whenever(notificationRepository.findById(1L)).thenReturn(Optional.of(notification))

        notificationService.deleteNotification(1L, 1L)

        verify(notificationRepository).delete(notification)
        verify(auditLogService).log(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `deleteNotification - throws when notification not found`() {
        whenever(notificationRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            notificationService.deleteNotification(1L, 99L)
        }
    }

    // ── getUnreadCount ────────────────────────────────────────────────────────

    @Test
    fun `getUnreadCount - delegates to repository`() {
        whenever(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(3L)

        val count = notificationService.getUnreadCount(1L)

        assertEquals(3L, count)
    }
}
