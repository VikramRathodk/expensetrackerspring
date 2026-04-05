package com.devvikram.expensetracker.expensetracker.exceptions

import com.devvikram.expensetracker.expensetracker.enums.ErrorCode
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.security.SignatureException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // 404 - Resource Not Found
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(
        ex: ResourceNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(
                code    = ErrorCode.RESOURCE_NOT_FOUND.name,
                message = ex.message ?: "Resource not found",
                path    = request.requestURI
            ),
            HttpStatus.NOT_FOUND
        )

    // 400 - Validation Errors (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }
        return ResponseEntity(
            ErrorResponse(
                code    = ErrorCode.VALIDATION_FAILED.name,
                message = "Validation failed",
                path    = request.requestURI,
                details = errors
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    // 400 - Bad Request (custom)
    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(
        ex: BadRequestException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(
                code    = ErrorCode.BAD_REQUEST.name,
                message = ex.message ?: "Bad request",
                path    = request.requestURI
            ),
            HttpStatus.BAD_REQUEST
        )

    // 409 - Conflict
    @ExceptionHandler(ConflictException::class)
    fun handleConflict(
        ex: ConflictException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(
                code    = ErrorCode.CONFLICT.name,
                message = ex.message ?: "Conflict occurred",
                path    = request.requestURI
            ),
            HttpStatus.CONFLICT
        )

    // 404 - User not found
    @ExceptionHandler(UsernameNotFoundException::class)
    fun handleUsernameNotFoundException(
        ex: UsernameNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(
                code    = ErrorCode.USER_NOT_FOUND.name,
                message = ex.message ?: "User not found",
                path    = request.requestURI
            ),
            HttpStatus.NOT_FOUND
        )

    // 401 - Authentication failed
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(
                code    = ErrorCode.AUTHENTICATION_FAILED.name,
                message = "Authentication failed: ${ex.message}",
                path    = request.requestURI
            ),
            HttpStatus.UNAUTHORIZED
        )

    // 401 - JWT expired
    @ExceptionHandler(ExpiredJwtException::class)
    fun handleExpiredJwtException(
        ex: ExpiredJwtException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(
                code    = ErrorCode.TOKEN_EXPIRED.name,
                message = "JWT token has expired",
                path    = request.requestURI
            ),
            HttpStatus.UNAUTHORIZED
        )

    // 400 - Malformed JWT
    @ExceptionHandler(MalformedJwtException::class)
    fun handleMalformedJwtException(
        ex: MalformedJwtException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(
                code    = ErrorCode.TOKEN_INVALID.name,
                message = "Invalid JWT token format",
                path    = request.requestURI
            ),
            HttpStatus.BAD_REQUEST
        )

    // 401 - JWT signature invalid
    @ExceptionHandler(SignatureException::class)
    fun handleSignatureException(
        ex: SignatureException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(
                code    = ErrorCode.TOKEN_SIGNATURE_INVALID.name,
                message = "JWT signature validation failed",
                path    = request.requestURI
            ),
            HttpStatus.UNAUTHORIZED
        )

    // 401 - Invalid argument (e.g. bad credentials)
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(
                code    = ErrorCode.INVALID_CREDENTIALS.name,
                message = ex.message ?: "Invalid request",
                path    = request.requestURI
            ),
            HttpStatus.UNAUTHORIZED
        )

    // 500 - Internal Server Error (fallback)
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception", ex)
        return ResponseEntity(
            ErrorResponse(
                code    = ErrorCode.INTERNAL_ERROR.name,
                message = "Internal server error",
                path    = request.requestURI
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}
