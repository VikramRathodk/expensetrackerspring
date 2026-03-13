package com.devvikram.expensetracker.expensetracker.security

import com.devvikram.expensetracker.expensetracker.exceptions.ResourceNotFoundException
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component

@Component
class SecurityUtil(
    private val userRepository: UserRepository
) {

    /**
     * Get current authenticated user's ID.
     * Looks up the user by email (the principal username) since the plain
     * Spring Security User object stored as principal does not carry an id field.
     */
    fun getCurrentUserId(): Long {
        val email = getCurrentUserEmail()
            ?: throw ResourceNotFoundException("No authenticated user found")
        return userRepository.findByEmail(email)
            .orElseThrow { ResourceNotFoundException("User not found with email: $email") }
            .id
    }

    companion object {

        /**
         * Get current authenticated user's email
         */
        fun getCurrentUserEmail(): String? {
            val authentication = getAuthentication()
            return when (val principal = authentication?.principal) {
                is UserDetails -> principal.username
                is String -> principal
                else -> null
            }
        }

        /**
         * Get current authentication object
         */
        fun getAuthentication(): Authentication? {
            return SecurityContextHolder.getContext().authentication
        }

        /**
         * Check if user has specific role
         */
        fun hasRole(role: String): Boolean {
            val authentication = getAuthentication()
            return authentication?.authorities?.any {
                it.authority == "ROLE_$role"
            } ?: false
        }

        /**
         * Check if user has any of the specified roles
         */
        fun hasAnyRole(vararg roles: String): Boolean {
            return roles.any { hasRole(it) }
        }

        /**
         * Check if user is admin
         */
        fun isAdmin(): Boolean {
            return hasAnyRole("ADMIN", "SUPER_ADMIN")
        }

        /**
         * Check if user is super admin
         */
        fun isSuperAdmin(): Boolean {
            return hasRole("SUPER_ADMIN")
        }

        /**
         * Check if current user is authenticated
         */
        fun isAuthenticated(): Boolean {
            val authentication = getAuthentication()
            return authentication?.isAuthenticated == true
        }

        /**
         * Get current user's roles
         */
        fun getCurrentUserRoles(): List<String> {
            val authentication = getAuthentication()
            return authentication?.authorities?.map {
                it.authority?.removePrefix("ROLE_") ?: ""
            } ?: emptyList()
        }
    }
}