package com.devvikram.expensetracker.expensetracker.service


import com.devvikram.expensetracker.expensetracker.enums.RoleType
import com.devvikram.expensetracker.expensetracker.models.Role
import com.devvikram.expensetracker.expensetracker.repository.RoleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoleService(
    private val roleRepository: RoleRepository
) {

    fun findByName(roleType: RoleType): Role {
        return roleRepository.findByName(roleType)
            .orElseThrow { NoSuchElementException("Role not found: $roleType") }
    }

    fun getAllRoles(): List<Role> = roleRepository.findAll()

    @Transactional
    fun createRoleIfNotExists(roleType: RoleType, description: String? = null): Role {
        return if (roleRepository.existsByName(roleType)) {
            roleRepository.findByName(roleType).get()
        } else {
            roleRepository.save(Role(name = roleType, description = description))
        }
    }

    @Transactional
    fun initializeDefaultRoles() {
        createRoleIfNotExists(RoleType.USER, "Regular user with basic permissions")
        createRoleIfNotExists(RoleType.ADMIN, "Administrator with elevated permissions")
        createRoleIfNotExists(RoleType.SUPER_ADMIN, "Super administrator with full access")
        createRoleIfNotExists(RoleType.MODERATOR, "Moderator with view permissions")
        createRoleIfNotExists(RoleType.ACCOUNTANT, "Accountant with financial report access")
        createRoleIfNotExists(RoleType.VIEWER, "Read-only access")
    }
}