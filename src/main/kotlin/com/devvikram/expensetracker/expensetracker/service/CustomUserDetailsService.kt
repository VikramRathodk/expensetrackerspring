package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmailWithRoles(email)
            .orElseThrow { UsernameNotFoundException("User not found with email: $email") }

        if (!user.isActive) {
            throw UsernameNotFoundException("User account is deactivated")
        }

        val authorities = user.roles.map { role ->
            SimpleGrantedAuthority("ROLE_${role.name}")
        }

        return User(
            user.email,
            user.password,
            user.isActive,
            true, // accountNonExpired
            true, // credentialsNonExpired
            true, // accountNonLocked
            authorities
        )
    }

    /**
     * Load user by ID (useful for token validation)
     */
    fun loadUserById(userId: Long): UserDetails {
        val user = userRepository.findById(userId)
            .orElseThrow { UsernameNotFoundException("User not found with id: $userId") }

        val authorities = user.roles.map { role ->
            SimpleGrantedAuthority("ROLE_${role.name}")
        }

        return User(
            user.email,
            user.password,
            user.isActive,
            true,
            true,
            true,
            authorities
        )
    }
}