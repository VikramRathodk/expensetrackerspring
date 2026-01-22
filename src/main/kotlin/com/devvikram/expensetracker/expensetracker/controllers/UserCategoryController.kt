package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.response.ApiResponse
import com.devvikram.expensetracker.expensetracker.entity.Category
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAuthenticated
import com.devvikram.expensetracker.expensetracker.security.anotation.IsSuperAdmin
import com.devvikram.expensetracker.expensetracker.service.CategoryService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
@IsAuthenticated
@IsSuperAdmin
class UserCategoryController(
    private val categoryService: CategoryService,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /* ================= CREATE ================= */
    @PostMapping
    fun createCategory(
        @Valid @RequestBody category: Category,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Category>> {

        logger.info(
            "Create user category API called by username={} categoryName={}",
            userDetails.username, category.name
        )

        val user = userRepository.findByEmail(userDetails.username)
            .orElseThrow {
                logger.error("User not found in DB for username={}", userDetails.username)
                RuntimeException("User not found")
            }

        logger.info("User verified. userId={}", user.id)

        val saved = categoryService.addUserCategory(category, user.id)

        logger.info(
            "User category created successfully. categoryId={} by userId={}",
            saved.id, user.id
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(
                status = true,
                message = "Category created successfully",
                data = saved
            )
        )
    }

    /* ================= UPDATE ================= */
    @PutMapping("/{id}")
    fun updateCategory(
        @PathVariable id: Long,
        @Valid @RequestBody category: Category,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Category>> {

        logger.info(
            "Update user category API called by username={} categoryId={}",
            userDetails.username, id
        )

        val user = userRepository.findByEmail(userDetails.username)
            .orElseThrow {
                logger.error("User not found in DB for username={}", userDetails.username)
                RuntimeException("User not found")
            }

        logger.info("User verified. userId={}", user.id)

        val updated = categoryService.updateUserCategory(id, category, user.id)

        logger.info(
            "User category updated successfully. categoryId={} by userId={}",
            updated.id, user.id
        )

        return ResponseEntity.ok(
            ApiResponse(
                status = true,
                message = "Category updated successfully",
                data = updated
            )
        )
    }

    /* ================= READ ================= */
    @GetMapping
    fun getCategories(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<Category>>> {

        logger.info(
            "Get categories API called by username={}",
            userDetails.username
        )

        val user = userRepository.findByEmail(userDetails.username)
            .orElseThrow {
                logger.error("User not found in DB for username={}", userDetails.username)
                RuntimeException("User not found")
            }

        logger.info("User verified. userId={}", user.id)

        val categories = categoryService.getCategoriesForUser(user.id)

        logger.info(
            "Categories fetched successfully. userId={} count={}",
            user.id, categories.size
        )

        return ResponseEntity.ok(
            ApiResponse(
                status = true,
                message = "Categories fetched successfully",
                data = categories
            )
        )
    }

    /* ================= DELETE ================= */
    @DeleteMapping("/{id}")
    fun deleteCategory(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Unit>> {

        logger.info(
            "Delete user category API called by username={} categoryId={}",
            userDetails.username, id
        )

        val user = userRepository.findByEmail(userDetails.username)
            .orElseThrow {
                logger.error("User not found in DB for username={}", userDetails.username)
                RuntimeException("User not found")
            }

        logger.info("User verified. userId={}", user.id)

        categoryService.deleteUserCategory(id, user.id)

        logger.info(
            "User category deleted successfully. categoryId={} by userId={}",
            id, user.id
        )

        return ResponseEntity.ok(
            ApiResponse(
                status = true,
                message = "Category deleted successfully"
            )
        )
    }
}