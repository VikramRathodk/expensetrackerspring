package com.devvikram.expensetracker.expensetracker.controllers

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
@RequestMapping("/api/admin/categories")
@IsAdmin
class AdminCategoryController(
    private val categoryService: CategoryService,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /* ============== CREATE GLOBAL CATEGORY ============== */
    @PostMapping
    fun createGlobalCategory(
        @Valid @RequestBody category: Category,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Category>> {

        logger.info(
            "Create global category API called by admin username={} categoryName={}",
            userDetails.username, category.name
        )

        val admin = userRepository.findByEmail(userDetails.username)
            .orElseThrow {
                logger.error("Admin not found in DB for username={}", userDetails.username)
                RuntimeException("Admin not found")
            }

        logger.info("Admin verified. adminId={}", admin.id)

        // Pass admin's userId to track who created the category
        val saved = categoryService.addGlobalCategory(category, admin.id)

        logger.info(
            "Global category created successfully. categoryId={} by adminId={}",
            saved.id, admin.id
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(
                status = true,
                message = "Global category created successfully",
                data = saved
            )
        )
    }

    /* ============== UPDATE GLOBAL CATEGORY ============== */
    @PutMapping("/{id}")
    fun updateGlobalCategory(
        @PathVariable id: Long,
        @Valid @RequestBody category: Category,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Category>> {

        logger.info(
            "Update global category API called by admin username={} categoryId={}",
            userDetails.username, id
        )

        val admin = userRepository.findByEmail(userDetails.username)
            .orElseThrow {
                logger.error("Admin not found in DB for username={}", userDetails.username)
                RuntimeException("Admin not found")
            }

        logger.info("Admin verified. adminId={}", admin.id)

        val updated = categoryService.updateGlobalCategory(id, category)

        logger.info(
            "Global category updated successfully. categoryId={} by adminId={}",
            updated.id, admin.id
        )

        return ResponseEntity.ok(
            ApiResponse(
                status = true,
                message = "Global category updated successfully",
                data = updated
            )
        )
    }

    /* ============== DELETE GLOBAL CATEGORY ============== */
    @DeleteMapping("/{id}")
    fun deleteGlobalCategory(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Unit>> {

        logger.info(
            "Delete global category API called by admin username={} categoryId={}",
            userDetails.username, id
        )

        val admin = userRepository.findByEmail(userDetails.username)
            .orElseThrow {
                logger.error("Admin not found in DB for username={}", userDetails.username)
                RuntimeException("Admin not found")
            }

        logger.info("Admin verified. adminId={}", admin.id)

        categoryService.deleteGlobalCategory(id)

        logger.info(
            "Global category deleted successfully. categoryId={} by adminId={}",
            id, admin.id
        )

        return ResponseEntity.ok(
            ApiResponse(
                status = true,
                message = "Global category deleted successfully"
            )
        )
    }
}