package com.devvikram.expensetracker.expensetracker.repository

import com.devvikram.expensetracker.expensetracker.entity.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Notification>

    fun findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId: Long): List<Notification>

    fun countByUserIdAndIsReadFalse(userId: Long): Long
}
