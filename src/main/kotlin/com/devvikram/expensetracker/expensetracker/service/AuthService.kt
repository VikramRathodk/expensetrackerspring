package com.devvikram.expensetracker.expensetracker.service


import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import com.devvikram.expensetracker.expensetracker.enums.RoleType
import com.devvikram.expensetracker.expensetracker.entity.User
import com.devvikram.expensetracker.expensetracker.dto.request.AssignRoleRequest
import com.devvikram.expensetracker.expensetracker.dto.response.AuthResponse
import com.devvikram.expensetracker.expensetracker.dto.request.LoginRequest
import com.devvikram.expensetracker.expensetracker.dto.request.RegisterRequest
import com.devvikram.expensetracker.expensetracker.dto.response.UserResponse
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import com.devvikram.expensetracker.expensetracker.security.JwtUtil
import org.slf4j.LoggerFactory

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val roleService: RoleService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val auditLogService: AuditLogService,
    private val refreshTokenService: RefreshTokenService
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)


    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }

        // Create user
        val user = User(
            name = request.name,
            email = request.email,
            password = passwordEncoder.encode(request.password).toString()
        )

        // Assign default USER role
        val userRole = roleService.findByName(RoleType.USER)
        user.roles.add(userRole)

        // If roles provided (admin registration), add them
        request.roles?.forEach { roleType ->
            val role = roleService.findByName(roleType)
            user.roles.add(role)
        }

        val savedUser = userRepository.save(user)

        auditLogService.log(
            userId     = savedUser.id,
            action     = AuditAction.USER_REGISTERED,
            entityType = "User",
            entityId   = savedUser.id,
            newValue   = mapOf("email" to savedUser.email, "roles" to savedUser.getRoleNames())
        )

        // Generate tokens
        val accessToken = jwtUtil.generateToken(savedUser)
        val refreshToken = refreshTokenService.createRefreshToken(savedUser.id)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken.token,
            user = savedUser.toUserResponse()
        )
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmailWithRoles(request.email)
            .orElseThrow { IllegalArgumentException("Invalid email or password") }

        if (!user.isActive) {
            throw IllegalArgumentException("Account is deactivated")
        }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("Invalid email or password")
        }

        val accessToken = jwtUtil.generateToken(user)
        val refreshToken = refreshTokenService.createRefreshToken(user.id)

        auditLogService.log(
            userId     = user.id,
            action     = AuditAction.USER_LOGIN,
            entityType = "User",
            entityId   = user.id
        )

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken.token,
            user = user.toUserResponse()
        )
    }


    @Transactional
    fun assignRoles(request: AssignRoleRequest, adminUserId: Long): UserResponse {

        logger.info("Assign Roles Request received by adminId={} for targetUserId={} with roles={}",
            adminUserId, request.userId, request.roles)

        // Check admin existence
        val admin = userRepository.findById(adminUserId)
            .orElseThrow {
                logger.error("Admin not found with id={}", adminUserId)
                NoSuchElementException("Admin not found")
            }

        // Check permission
        if (!admin.isSuperAdmin()) {
            logger.warn("User id={} attempted to assign roles without SUPER_ADMIN permission", adminUserId)
            throw IllegalArgumentException("Only Super Admin can assign roles")
        }

        // Find target user
        val user = userRepository.findById(request.userId)
            .orElseThrow {
                logger.error("Target user not found with id={}", request.userId)
                NoSuchElementException("User not found")
            }

        logger.info("Clearing existing roles for userId={}", user.id)
        user.roles.clear()

        // Assign new roles
        request.roles.forEach { roleType ->
            val role = roleService.findByName(roleType)
            logger.info("Assigning role={} to userId={}", roleType, user.id)
            user.roles.add(role)
        }

        val updatedUser = userRepository.save(user)

        logger.info("Roles successfully updated for userId={}. New roles={}",
            updatedUser.id, updatedUser.getRoleNames())

        auditLogService.log(
            userId     = adminUserId,
            action     = AuditAction.ROLE_ASSIGNED,
            entityType = "User",
            entityId   = updatedUser.id,
            newValue   = mapOf("assignedRoles" to updatedUser.getRoleNames())
        )

        return updatedUser.toUserResponse()
    }

    @Transactional
    fun refreshToken(token: String): AuthResponse {
        val refreshToken = refreshTokenService.validateRefreshToken(token)
        val user = refreshToken.user

        // Rotate: issue new access token + new refresh token
        val newAccessToken = jwtUtil.generateToken(user)
        val newRefreshToken = refreshTokenService.createRefreshToken(user.id)

        auditLogService.log(
            userId     = user.id,
            action     = AuditAction.TOKEN_REFRESHED,
            entityType = "User",
            entityId   = user.id
        )

        return AuthResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken.token,
            user = user.toUserResponse()
        )
    }

    @Transactional
    fun logout(userId: Long) {
        refreshTokenService.revokeByUserId(userId)

        auditLogService.log(
            userId     = userId,
            action     = AuditAction.USER_LOGOUT,
            entityType = "User",
            entityId   = userId
        )
    }

    @Transactional
    fun updateBaseCurrency(userId: Long, newCurrency: String): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("User not found") }
        val updated = userRepository.save(user.copy(baseCurrency = newCurrency.uppercase()))
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.USER_CURRENCY_UPDATED,
            entityType = "User",
            entityId   = userId,
            newValue   = mapOf("baseCurrency" to newCurrency.uppercase())
        )
        return updated.toUserResponse()
    }

    private fun User.toUserResponse() = UserResponse(
        id           = id,
        name         = name,
        email        = email,
        roles        = getRoleNames(),
        isActive     = isActive,
        createdAt    = createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
        baseCurrency = baseCurrency
    )
}