package com.devvikram.expensetracker.expensetracker.config


import com.devvikram.expensetracker.expensetracker.enums.RoleType
import com.devvikram.expensetracker.expensetracker.entity.User
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import com.devvikram.expensetracker.expensetracker.service.RoleService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class DataInitializer {

    @Bean
    fun initRoles(roleService: RoleService) = CommandLineRunner {
        roleService.initializeDefaultRoles()
        println("Default roles initialized successfully")
    }
    @Bean
    fun initAdmin(
        userRepository: UserRepository,
        roleService: RoleService,
        passwordEncoder: PasswordEncoder
    ) = CommandLineRunner {

        val email = "rathodvikramk382@gmail.com"

        if (!userRepository.existsByEmail(email)) {

            val admin = User(
                name = "Super Admin",
                email = email,
                password = passwordEncoder.encode("test123").toString()
            )

            admin.roles.add(roleService.findByName(RoleType.SUPER_ADMIN))
            userRepository.save(admin)

            println(" Super Admin user created")

        } else {

            val user = userRepository.findByEmailWithRoles(email).get()

            if (user.roles.none { it.name == RoleType.SUPER_ADMIN }) {
                user.roles.add(roleService.findByName(RoleType.SUPER_ADMIN))
                userRepository.save(user)
                println(" Super Admin role added to existing user")
            }
        }
    }

}