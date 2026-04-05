package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.response.ApiResponse
import com.devvikram.expensetracker.expensetracker.dto.response.NotificationResponse
import com.devvikram.expensetracker.expensetracker.security.SecurityUtil
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAuthenticated
import com.devvikram.expensetracker.expensetracker.service.NotificationService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val securityUtil: SecurityUtil
) {

    // GET /api/notifications?page=0&size=20
    @GetMapping
    @IsAuthenticated
    fun getNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<NotificationResponse>>> {
        val userId = securityUtil.getCurrentUserId()
        val notifications = notificationService.getNotifications(userId, page, size)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Notifications fetched successfully", data = notifications)
        )
    }

    // GET /api/notifications/unread
    @GetMapping("/unread")
    @IsAuthenticated
    fun getUnreadNotifications(): ResponseEntity<ApiResponse<List<NotificationResponse>>> {
        val userId = securityUtil.getCurrentUserId()
        val notifications = notificationService.getUnreadNotifications(userId)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Unread notifications fetched successfully", data = notifications)
        )
    }

    // GET /api/notifications/unread/count
    @GetMapping("/unread/count")
    @IsAuthenticated
    fun getUnreadCount(): ResponseEntity<ApiResponse<Long>> {
        val userId = securityUtil.getCurrentUserId()
        val count = notificationService.getUnreadCount(userId)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Unread count fetched successfully", data = count)
        )
    }

    // PUT /api/notifications/{id}/read
    @PutMapping("/{id}/read")
    @IsAuthenticated
    fun markAsRead(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<NotificationResponse>> {
        val userId = securityUtil.getCurrentUserId()
        val notification = notificationService.markAsRead(userId, id)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Notification marked as read", data = notification)
        )
    }

    // PUT /api/notifications/read-all
    @PutMapping("/read-all")
    @IsAuthenticated
    fun markAllAsRead(): ResponseEntity<ApiResponse<Map<String, Int>>> {
        val userId = securityUtil.getCurrentUserId()
        val count = notificationService.markAllAsRead(userId)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "$count notification(s) marked as read", data = mapOf("markedRead" to count))
        )
    }

    // DELETE /api/notifications/{id}
    @DeleteMapping("/{id}")
    @IsAuthenticated
    fun deleteNotification(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        val userId = securityUtil.getCurrentUserId()
        notificationService.deleteNotification(userId, id)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Notification deleted successfully", data = null)
        )
    }
}
