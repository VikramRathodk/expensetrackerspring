package com.devvikram.expensetracker.expensetracker.repository

import com.devvikram.expensetracker.expensetracker.entity.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long> {

    fun existsByNameIgnoreCaseAndIsGlobalTrue(name: String): Boolean

    fun existsByNameIgnoreCaseAndUserId(name: String, userId: Long): Boolean

    fun findByUserIdOrIsGlobalTrue(userId: Long): List<Category>

}


