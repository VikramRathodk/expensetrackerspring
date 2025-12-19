package com.devvikram.expensetracker.expensetracker.exceptions

import com.devvikram.expensetracker.expensetracker.models.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    // 🔹 404 - Resource Not Found
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(
        ex: ResourceNotFoundException
    ): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity(
            ApiResponse(
                status = false,
                message = ex.message ?: "Resource not found"
            ),
            HttpStatus.NOT_FOUND
        )

    // 🔹 400 - Validation Errors (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
        ex: MethodArgumentNotValidException
    ): ResponseEntity<ApiResponse<Map<String, String>>> {

        val errors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }

        return ResponseEntity(
            ApiResponse(
                status = false,
                message = "Validation failed",
                data = errors
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    // 🔹 400 - Bad Request (custom)
    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(
        ex: BadRequestException
    ): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity(
            ApiResponse(
                status = false,
                message = ex.message ?: "Bad request"
            ),
            HttpStatus.BAD_REQUEST
        )

    // 🔹 409 - Conflict
    @ExceptionHandler(ConflictException::class)
    fun handleConflict(
        ex: ConflictException
    ): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity(
            ApiResponse(
                status = false,
                message = ex.message ?: "Conflict occurred"
            ),
            HttpStatus.CONFLICT
        )

    // 🔹 500 - Internal Server Error (fallback)
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception
    ): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity(
            ApiResponse(
                status = false,
                message = "Internal server error"
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
}
