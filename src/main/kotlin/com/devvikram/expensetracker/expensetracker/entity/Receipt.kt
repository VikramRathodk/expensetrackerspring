package com.devvikram.expensetracker.expensetracker.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "receipts")
data class Receipt(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "expense_id", nullable = false)
    val expenseId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    /** Original file name supplied by the client (e.g. "receipt_jan.pdf"). */
    @Column(name = "file_name", nullable = false, length = 255)
    val fileName: String,

    /** Raw file bytes stored directly in the database. */
    @Column(name = "file_data", nullable = false, columnDefinition = "BYTEA")
    val fileData: ByteArray,

    /** File size in bytes. */
    @Column(name = "file_size", nullable = false)
    val fileSize: Long,

    /** MIME type (image/jpeg, image/png, application/pdf). */
    @Column(name = "content_type", nullable = false, length = 100)
    val contentType: String,

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    val uploadedAt: LocalDateTime = LocalDateTime.now()
)
