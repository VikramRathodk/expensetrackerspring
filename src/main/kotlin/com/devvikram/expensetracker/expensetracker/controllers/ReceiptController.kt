package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.response.ApiResponse
import com.devvikram.expensetracker.expensetracker.dto.response.ReceiptResponse
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAuthenticated
import com.devvikram.expensetracker.expensetracker.service.ReceiptService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/expenses/{expenseId}/receipts")
@IsAuthenticated
class ReceiptController(
    private val receiptService: ReceiptService,
    private val userRepository: UserRepository
) {

    private fun getUserId(userDetails: UserDetails): Long =
        userRepository.findByEmail(userDetails.username)
            .orElseThrow { RuntimeException("User not found") }
            .id

    /** Upload one or more receipts (jpg / png / pdf, max 5 MB each) and store them in the database. */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadReceipts(
        @PathVariable expenseId: Long,
        @RequestPart("files") files: List<MultipartFile>,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<ReceiptResponse>>> {
        val data = receiptService.uploadReceipt(expenseId, getUserId(userDetails), files)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(true, "${data.size} receipt(s) uploaded", data))
    }

    /** List all receipts for an expense. */
    @GetMapping
    fun getReceipts(
        @PathVariable expenseId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<ReceiptResponse>>> {
        val data = receiptService.getReceipts(expenseId, getUserId(userDetails))
        return ResponseEntity.ok(ApiResponse(true, "Receipts fetched", data))
    }

    /** Download the raw file bytes for a single receipt. */
    @GetMapping("/{receiptId}/download")
    fun downloadReceipt(
        @PathVariable expenseId: Long,
        @PathVariable receiptId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ByteArray> {
        val receipt = receiptService.getReceiptForDownload(expenseId, receiptId, getUserId(userDetails))
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(receipt.contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${receipt.fileName}\"")
            .body(receipt.fileData)
    }

    /** Permanently delete a receipt from the database. */
    @DeleteMapping("/{receiptId}")
    fun deleteReceipt(
        @PathVariable expenseId: Long,
        @PathVariable receiptId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Nothing>> {
        receiptService.deleteReceipt(expenseId, receiptId, getUserId(userDetails))
        return ResponseEntity.ok(ApiResponse(true, "Receipt deleted"))
    }
}
