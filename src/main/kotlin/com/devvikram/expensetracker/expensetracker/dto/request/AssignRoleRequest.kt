package com.devvikram.expensetracker.expensetracker.dto.request

import com.devvikram.expensetracker.expensetracker.enums.RoleType
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class AssignRoleRequest(
    @field:NotNull(message = "User ID is required")
    val userId: Long,

    @field:NotEmpty(message = "Roles are required")
    val roles: Set<RoleType>
)