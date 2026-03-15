package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.dto.request.LoginRequest
import com.devvikram.expensetracker.expensetracker.dto.request.RegisterRequest
import com.devvikram.expensetracker.expensetracker.entity.Role
import com.devvikram.expensetracker.expensetracker.entity.User
import com.devvikram.expensetracker.expensetracker.enums.RoleType
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import com.devvikram.expensetracker.expensetracker.security.JwtUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthServiceTest {

    private val userRepository: UserRepository = mock()
    private val roleService: RoleService = mock()
    private val passwordEncoder = BCryptPasswordEncoder()
    private val jwtUtil: JwtUtil = mock()
    private val auditLogService: AuditLogService = mock()

    private lateinit var authService: AuthService

    private val userRole = Role(id = 1L, name = RoleType.USER)
    private val savedUser = User(
        id = 1L,
        name = "Alice",
        email = "alice@example.com",
        password = passwordEncoder.encode("password123").toString(),
        roles = mutableSetOf(userRole)
    )

    @BeforeEach
    fun setUp() {
        authService = AuthService(userRepository, roleService, passwordEncoder, jwtUtil, auditLogService)
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    fun `register - new user is saved and token is returned`() {
        val request = RegisterRequest(name = "Alice", email = "alice@example.com", password = "password123")

        whenever(userRepository.existsByEmail("alice@example.com")).thenReturn(false)
        whenever(roleService.findByName(RoleType.USER)).thenReturn(userRole)
        whenever(userRepository.save(any())).thenReturn(savedUser)
        whenever(jwtUtil.generateToken(savedUser)).thenReturn("test-jwt-token")

        val result = authService.register(request)

        assertNotNull(result.token)
        assertEquals("alice@example.com", result.user.email)
        verify(auditLogService).log(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `register - throws when email already registered`() {
        val request = RegisterRequest(name = "Alice", email = "alice@example.com", password = "password123")
        whenever(userRepository.existsByEmail("alice@example.com")).thenReturn(true)

        assertThrows<IllegalArgumentException> {
            authService.register(request)
        }
        verify(userRepository, never()).save(any())
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    fun `login - valid credentials return token`() {
        val request = LoginRequest(email = "alice@example.com", password = "password123")

        whenever(userRepository.findByEmailWithRoles("alice@example.com")).thenReturn(Optional.of(savedUser))
        whenever(jwtUtil.generateToken(savedUser)).thenReturn("test-jwt-token")

        val result = authService.login(request)

        assertEquals("test-jwt-token", result.token)
        verify(auditLogService).log(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `login - throws when password is wrong`() {
        val request = LoginRequest(email = "alice@example.com", password = "wrong-password")
        whenever(userRepository.findByEmailWithRoles("alice@example.com")).thenReturn(Optional.of(savedUser))

        assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
    }

    @Test
    fun `login - throws when user email not found`() {
        val request = LoginRequest(email = "nobody@example.com", password = "password123")
        whenever(userRepository.findByEmailWithRoles("nobody@example.com")).thenReturn(Optional.empty())

        assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
    }

    @Test
    fun `login - throws when account is deactivated`() {
        val inactiveUser = savedUser.copy(isActive = false)
        val request = LoginRequest(email = "alice@example.com", password = "password123")
        whenever(userRepository.findByEmailWithRoles("alice@example.com")).thenReturn(Optional.of(inactiveUser))

        assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
    }
}
