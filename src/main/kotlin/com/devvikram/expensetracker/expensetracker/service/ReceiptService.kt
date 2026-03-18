package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.dto.response.ReceiptResponse
import com.devvikram.expensetracker.expensetracker.entity.Receipt
import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import com.devvikram.expensetracker.expensetracker.exceptions.BadRequestException
import com.devvikram.expensetracker.expensetracker.exceptions.ResourceNotFoundException
import com.devvikram.expensetracker.expensetracker.repository.ExpenseRepository
import com.devvikram.expensetracker.expensetracker.repository.ReceiptRepository
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class ReceiptService(
    private val receiptRepository: ReceiptRepository,
    private val expenseRepository: ExpenseRepository,
    private val auditLogService: AuditLogService
) {

    companion object {
        private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "application/pdf")
        private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L   // 5 MB
        private const val MAX_RECEIPTS_PER_EXPENSE = 5
    }

    fun uploadReceipt(expenseId: Long, userId: Long, files: List<MultipartFile>): List<ReceiptResponse> {
        if (files.isEmpty()) return emptyList()

        val expense = expenseRepository.findById(expenseId)
            .orElseThrow { ResourceNotFoundException("Expense not found") }
        if (expense.userId != userId) throw ResourceNotFoundException("Expense not found")

        val existingCount = receiptRepository.countByExpenseId(expenseId)
        if (existingCount + files.size > MAX_RECEIPTS_PER_EXPENSE)
            throw BadRequestException("Cannot upload ${files.size} file(s): only ${MAX_RECEIPTS_PER_EXPENSE - existingCount} slot(s) remaining")

        // Validate all files before saving any
        files.forEach { file ->
            val contentType = file.contentType?.lowercase()
                ?: throw BadRequestException("Content-Type header is required for file: ${file.originalFilename}")
            if (contentType !in ALLOWED_CONTENT_TYPES)
                throw BadRequestException("Invalid file type for '${file.originalFilename}'. Allowed: jpg, png, pdf")
            if (file.size > MAX_FILE_SIZE_BYTES)
                throw BadRequestException("File '${file.originalFilename}' exceeds the 5 MB limit")
        }

        return files.map { file -> saveSingleFile(expenseId, userId, file) }
    }

    private fun saveSingleFile(expenseId: Long, userId: Long, file: MultipartFile): ReceiptResponse {
        val contentType = file.contentType!!.lowercase()

        val receipt = receiptRepository.save(
            Receipt(
                expenseId   = expenseId,
                userId      = userId,
                fileName    = file.originalFilename ?: "receipt",
                fileData    = file.bytes,
                fileSize    = file.size,
                contentType = contentType
            )
        )

        auditLogService.log(
            userId     = userId,
            action     = AuditAction.RECEIPT_UPLOADED,
            entityType = "Receipt",
            entityId   = receipt.id,
            newValue   = mapOf("expenseId" to expenseId, "fileName" to receipt.fileName, "fileSize" to receipt.fileSize)
        )

        return receipt.toResponse()
    }

    fun getReceipts(expenseId: Long, userId: Long): List<ReceiptResponse> {
        val expense = expenseRepository.findById(expenseId)
            .orElseThrow { ResourceNotFoundException("Expense not found") }
        if (expense.userId != userId) throw ResourceNotFoundException("Expense not found")

        return receiptRepository.findByExpenseIdAndUserId(expenseId, userId)
            .map { it.toResponse() }
    }

    fun getReceiptForDownload(expenseId: Long, receiptId: Long, userId: Long): Receipt {
        val receipt = receiptRepository.findByIdAndUserId(receiptId, userId)
            .orElseThrow { ResourceNotFoundException("Receipt not found") }
        if (receipt.expenseId != expenseId) throw ResourceNotFoundException("Receipt not found")
        return receipt
    }

    fun deleteReceipt(expenseId: Long, receiptId: Long, userId: Long) {
        val receipt = receiptRepository.findByIdAndUserId(receiptId, userId)
            .orElseThrow { ResourceNotFoundException("Receipt not found") }
        if (receipt.expenseId != expenseId) throw ResourceNotFoundException("Receipt not found")

        receiptRepository.delete(receipt)

        auditLogService.log(
            userId     = userId,
            action     = AuditAction.RECEIPT_DELETED,
            entityType = "Receipt",
            entityId   = receiptId,
            oldValue   = mapOf("expenseId" to expenseId, "fileName" to receipt.fileName)
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun Receipt.toResponse() = ReceiptResponse(
        id          = id,
        expenseId   = expenseId,
        fileName    = fileName,
        fileSize    = fileSize,
        contentType = contentType,
        uploadedAt  = uploadedAt,
        downloadUrl = "/api/v1/expenses/$expenseId/receipts/$id/download"
    )
}
