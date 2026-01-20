package com.devvikram.expensetracker.expensetracker.controllers



import com.devvikram.expensetracker.expensetracker.models.ApiResponse
import com.devvikram.expensetracker.expensetracker.models.dtos.AssignRoleRequest
import com.devvikram.expensetracker.expensetracker.models.dtos.AuthResponse
import com.devvikram.expensetracker.expensetracker.models.dtos.LoginRequest
import com.devvikram.expensetracker.expensetracker.models.dtos.RegisterRequest
import com.devvikram.expensetracker.expensetracker.models.dtos.UserResponse
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import com.devvikram.expensetracker.expensetracker.security.anotation.IsSuperAdmin
import com.devvikram.expensetracker.expensetracker.service.AuthService
import com.devvikram.expensetracker.expensetracker.service.RoleService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val roleService: RoleService,
    private val userRepository: UserRepository
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(
                status = true,
                message = "User registered successfully",
                data = response
            )
        )
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(
            ApiResponse(
                status = true,
                message = "Login successful",
                data = response
            )
        )
    }

    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<ApiResponse<Map<String, Any>>> {
        return ResponseEntity.ok(
            ApiResponse(
                status = true,
                message = "User details fetched",
                data = mapOf(
                    "email" to userDetails.username,
                    "authorities" to userDetails.authorities.map { it.authority }
                )
            )
        )
    }

    @PostMapping("/assign-roles")
    @IsSuperAdmin
    fun assignRoles(
        @Valid @RequestBody request: AssignRoleRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<UserResponse>> {

        val logger = LoggerFactory.getLogger(this::class.java)

        logger.info(
            "Assign-Roles API called by username={} for targetUserId={} roles={}",
            userDetails.username, request.userId, request.roles
        )

        val admin = userRepository.findByEmail(userDetails.username)
            .orElseThrow {
                logger.error("Admin not found in DB for username={}", userDetails.username)
                RuntimeException("Admin not found")
            }

        logger.info("Admin verified. adminId={}", admin.id)

        val response = authService.assignRoles(request, admin.id)

        logger.info(
            "Roles successfully assigned. adminId={} → userId={}",
            admin.id, request.userId
        )

        return ResponseEntity.ok(
            ApiResponse(
                status = true,
                message = "Roles assigned successfully",
                data = response
            )
        )
    }



    @GetMapping("/roles")
    fun getAllRoles(): ResponseEntity<ApiResponse<List<String>>> {
        val roles = roleService.getAllRoles().map { it.name.name }
        return ResponseEntity.ok(
            ApiResponse(
                status = true,
                message = "Roles fetched successfully",
                data = roles
            )
        )
    }
}