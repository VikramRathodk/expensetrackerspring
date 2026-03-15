package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.response.ApiResponse
import com.devvikram.expensetracker.expensetracker.dto.response.AuditLogResponse
import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import com.devvikram.expensetracker.expensetracker.security.SecurityUtil
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAdmin
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAuthenticated
import com.devvikram.expensetracker.expensetracker.security.anotation.IsSuperAdmin
import com.devvikram.expensetracker.expensetracker.service.AuditLogService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/audit-logs")
class AuditLogController(
    private val auditLogService: AuditLogService,
    private val securityUtil: SecurityUtil
) {

    // GET /api/audit-logs?page=0&size=20
    // Returns all logs — ADMIN only
    @GetMapping
    @IsAdmin
    fun getAllLogs(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<AuditLogResponse>>> {
        val logs = auditLogService.getAllLogs(page, size)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Audit logs fetched successfully", data = logs)
        )
    }

    // GET /api/audit-logs/me?page=0&size=20
    // Returns the current user's own audit trail — any authenticated user
    @GetMapping("/me")
    @IsAuthenticated
    fun getMyLogs(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<AuditLogResponse>>> {
        val userId = securityUtil.getCurrentUserId()
        val logs = auditLogService.getLogsByUser(userId, page, size)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Your audit logs fetched successfully", data = logs)
        )
    }

    // GET /api/audit-logs/user/{userId}?page=0&size=20
    // Returns audit trail for any user — SUPER_ADMIN only
    @GetMapping("/user/{userId}")
    @IsSuperAdmin
    fun getLogsByUser(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<AuditLogResponse>>> {
        val logs = auditLogService.getLogsByUser(userId, page, size)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Audit logs for user $userId fetched successfully", data = logs)
        )
    }

    // GET /api/audit-logs/entity/{entityType}/{entityId}
    // Returns full change history for a specific entity — ADMIN only
    @GetMapping("/entity/{entityType}/{entityId}")
    @IsAdmin
    fun getLogsByEntity(
        @PathVariable entityType: String,
        @PathVariable entityId: Long
    ): ResponseEntity<ApiResponse<List<AuditLogResponse>>> {
        val logs = auditLogService.getLogsByEntity(entityType, entityId)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Entity audit history fetched successfully", data = logs)
        )
    }

    // GET /api/audit-logs/action/{action}?page=0&size=20
    // Returns logs filtered by action type — ADMIN only
    @GetMapping("/action/{action}")
    @IsAdmin
    fun getLogsByAction(
        @PathVariable action: AuditAction,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<AuditLogResponse>>> {
        val logs = auditLogService.getLogsByAction(action, page, size)
        return ResponseEntity.ok(
            ApiResponse(status = true, message = "Audit logs for action $action fetched successfully", data = logs)
        )
    }
}
