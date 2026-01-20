package com.devvikram.expensetracker.expensetracker.repository

import com.devvikram.expensetracker.expensetracker.enums.RoleType
import com.devvikram.expensetracker.expensetracker.models.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RoleRepository : JpaRepository<Role, Long> {
    fun findByName(name: RoleType): Optional<Role>
    fun existsByName(name: RoleType): Boolean
}