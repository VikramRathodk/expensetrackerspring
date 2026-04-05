package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.request.CategoryRequest
import com.devvikram.expensetracker.expensetracker.dto.response.ApiResponse
import com.devvikram.expensetracker.expensetracker.entity.Category
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAdmin
import com.devvikram.expensetracker.expensetracker.service.CategoryService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/categories")
@IsAdmin
class AdminCategoryController(
    private val categoryService: CategoryService,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun getAdminId(userDetails: UserDetails): Long =
        userRepository.findByEmail(userDetails.username)
            .orElseThrow { RuntimeException("Admin not found") }
            .id

    /* ============== LIST GLOBAL CATEGORIES ============== */
    @GetMapping
    fun listGlobalCategories(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<Category>>> {
        logger.info("List global categories called by admin username={}", userDetails.username)
        val categories = categoryService.listGlobalCategories()
        return ResponseEntity.ok(ApiResponse(true, "Global categories fetched", categories))
    }

    /* ============== CREATE GLOBAL CATEGORY ============== */
    @PostMapping
    fun createGlobalCategory(
        @Valid @RequestBody request: CategoryRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Category>> {
        logger.info("Create global category called by admin username={} name={}", userDetails.username, request.name)
        val adminId = getAdminId(userDetails)
        val saved = categoryService.addGlobalCategory(request, adminId)
        logger.info("Global category created. categoryId={} adminId={}", saved.id, adminId)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(true, "Global category created successfully", saved)
        )
    }

    /* ============== UPDATE GLOBAL CATEGORY ============== */
    @PutMapping("/{id}")
    fun updateGlobalCategory(
        @PathVariable id: Long,
        @Valid @RequestBody request: CategoryRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Category>> {
        logger.info("Update global category id={} called by admin username={}", id, userDetails.username)
        val adminId = getAdminId(userDetails)
        val updated = categoryService.updateGlobalCategory(id, request, adminId)
        logger.info("Global category updated. categoryId={} adminId={}", updated.id, adminId)
        return ResponseEntity.ok(ApiResponse(true, "Global category updated successfully", updated))
    }

    /* ============== DELETE GLOBAL CATEGORY ============== */
    @DeleteMapping("/{id}")
    fun deleteGlobalCategory(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Nothing>> {
        logger.info("Delete global category id={} called by admin username={}", id, userDetails.username)
        val adminId = getAdminId(userDetails)
        categoryService.deleteGlobalCategory(id, adminId)
        logger.info("Global category deleted. categoryId={} adminId={}", id, adminId)
        return ResponseEntity.ok(ApiResponse(true, "Global category deleted successfully"))
    }
}
