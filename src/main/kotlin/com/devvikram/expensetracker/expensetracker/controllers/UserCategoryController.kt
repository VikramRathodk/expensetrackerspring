package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.request.CategoryRequest
import com.devvikram.expensetracker.expensetracker.dto.response.ApiResponse
import com.devvikram.expensetracker.expensetracker.entity.Category
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAuthenticated
import com.devvikram.expensetracker.expensetracker.service.CategoryService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/categories")
@IsAuthenticated
class UserCategoryController(
    private val categoryService: CategoryService,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun getUserId(userDetails: UserDetails): Long =
        userRepository.findByEmail(userDetails.username)
            .orElseThrow { RuntimeException("User not found") }
            .id

    /* ================= GET ================= */
    @GetMapping
    fun getCategories(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<Category>>> {
        logger.info("Get categories called by username={}", userDetails.username)
        val userId = getUserId(userDetails)
        val categories = categoryService.getCategoriesForUser(userId)
        logger.info("Categories fetched. userId={} count={}", userId, categories.size)
        return ResponseEntity.ok(ApiResponse(true, "Categories fetched successfully", categories))
    }

    /* ================= CREATE ================= */
    @PostMapping
    fun createCategory(
        @Valid @RequestBody request: CategoryRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Category>> {
        logger.info("Create user category called by username={} name={}", userDetails.username, request.name)
        val userId = getUserId(userDetails)
        val saved = categoryService.addUserCategory(request, userId)
        logger.info("User category created. categoryId={} userId={}", saved.id, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(true, "Category created successfully", saved)
        )
    }

    /* ================= UPDATE ================= */
    @PutMapping("/{id}")
    fun updateCategory(
        @PathVariable id: Long,
        @Valid @RequestBody request: CategoryRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Category>> {
        logger.info("Update user category id={} called by username={}", id, userDetails.username)
        val userId = getUserId(userDetails)
        val updated = categoryService.updateUserCategory(id, request, userId)
        logger.info("User category updated. categoryId={} userId={}", updated.id, userId)
        return ResponseEntity.ok(ApiResponse(true, "Category updated successfully", updated))
    }

    /* ================= DELETE ================= */
    @DeleteMapping("/{id}")
    fun deleteCategory(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Nothing>> {
        logger.info("Delete user category id={} called by username={}", id, userDetails.username)
        val userId = getUserId(userDetails)
        categoryService.deleteUserCategory(id, userId)
        logger.info("User category deleted. categoryId={} userId={}", id, userId)
        return ResponseEntity.ok(ApiResponse(true, "Category deleted successfully"))
    }
}
