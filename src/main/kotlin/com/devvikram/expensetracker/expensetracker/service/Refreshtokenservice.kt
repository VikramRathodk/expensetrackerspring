package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.entity.RefreshToken
import com.devvikram.expensetracker.expensetracker.repository.RefreshTokenRepository
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository
) {
    @Value("\${jwt.refresh-expiration:604800000}")
    private val refreshExpiration: Long = 604800000L

    @Transactional
    fun createRefreshToken(userId: Long): RefreshToken {
        val user = userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("User not found") }

        // Revoke all existing tokens for this user (single active session)
        refreshTokenRepository.deleteAllByUser(user)

        return refreshTokenRepository.save(
            RefreshToken(
                token = UUID.randomUUID().toString(),
                user = user,
                expiresAt = Instant.now().plusMillis(refreshExpiration)
            )
        )
    }

    @Transactional(readOnly = true)
    fun validateRefreshToken(token: String): RefreshToken {
        val refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found") }

        if (refreshToken.revoked)
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked")

        if (refreshToken.expiresAt.isBefore(Instant.now()))
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired")

        return refreshToken
    }

    @Transactional
    fun revokeByUserId(userId: Long) {
        refreshTokenRepository.deleteAllByUserId(userId)
    }
}
