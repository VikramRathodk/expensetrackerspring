package com.devvikram.expensetracker.expensetracker.enums

enum class ErrorCode(val description: String) {

    // ── Resource ──────────────────────────────────────────────────────────────
    RESOURCE_NOT_FOUND("Requested resource not found"),
    USER_NOT_FOUND("User not found"),
    TAG_NOT_FOUND("Tag not found"),

    // ── Validation ────────────────────────────────────────────────────────────
    VALIDATION_FAILED("Request validation failed"),
    BAD_REQUEST("Bad request"),
    BUDGET_EXCEEDED("Expense exceeds the configured budget"),

    // ── Conflict ──────────────────────────────────────────────────────────────
    CONFLICT("Resource already exists"),
    EMAIL_ALREADY_EXISTS("Email address is already registered"),
    TAG_ALREADY_EXISTS("A tag with this name already exists for this user"),

    // ── Auth ──────────────────────────────────────────────────────────────────
    INVALID_CREDENTIALS("Invalid email or password"),
    ACCOUNT_DEACTIVATED("This account has been deactivated"),
    TOKEN_EXPIRED("JWT token has expired"),
    TOKEN_INVALID("JWT token is malformed or invalid"),
    TOKEN_SIGNATURE_INVALID("JWT token signature validation failed"),
    AUTHENTICATION_FAILED("Authentication failed"),
    UNAUTHORIZED("Insufficient permissions"),

    // ── Receipts ──────────────────────────────────────────────────────────────
    RECEIPT_NOT_FOUND("Receipt not found"),
    RECEIPT_INVALID_TYPE("Invalid file type. Allowed: jpg, png, pdf"),
    RECEIPT_SIZE_EXCEEDED("File size exceeds the 5 MB limit"),
    RECEIPT_LIMIT_EXCEEDED("Maximum number of receipts per expense reached"),

    // ── Server ────────────────────────────────────────────────────────────────
    INTERNAL_ERROR("An unexpected internal error occurred")
}
