package com.devvikram.expensetracker.expensetracker.repository

import com.devvikram.expensetracker.expensetracker.enums.RoleType
import com.devvikram.expensetracker.expensetracker.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean

    @Query("""
   SELECT u FROM User u 
   LEFT JOIN FETCH u.roles 
   WHERE u.email = :email
""")
    fun findByEmailWithRoles(@Param("email") email: String): Optional<User>


    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleType")
    fun findByRoleType(roleType: RoleType): List<User>
}