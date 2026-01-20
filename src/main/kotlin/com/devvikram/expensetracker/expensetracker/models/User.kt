package com.devvikram.expensetracker.expensetracker.models

import com.devvikram.expensetracker.expensetracker.enums.RoleType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import java.time.LocalDateTime


@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val password: String, // BCrypt hashed

    @Column(nullable = false)
    val isActive: Boolean = true,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    val roles: MutableSet<Role> = mutableSetOf()


) {
    // Helper methods
    fun hasRole(roleType: RoleType): Boolean = roles.any { it.name == roleType }

    fun isAdmin(): Boolean = hasRole(RoleType.ADMIN) || hasRole(RoleType.SUPER_ADMIN)

    fun isSuperAdmin(): Boolean = hasRole(RoleType.SUPER_ADMIN)

    fun getRoleNames(): List<String> = roles.map { it.name.name }
}