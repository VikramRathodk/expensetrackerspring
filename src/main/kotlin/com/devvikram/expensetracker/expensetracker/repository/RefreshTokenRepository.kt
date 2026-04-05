package com.devvikram.expensetracker.expensetracker.repository

import com.devvikram.expensetracker.expensetracker.entity.RefreshToken
import com.devvikram.expensetracker.expensetracker.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    fun findByToken(token: String): Optional<RefreshToken>

    fun deleteAllByUser(user: User)

    fun deleteAllByUserId(userId: Long)
}