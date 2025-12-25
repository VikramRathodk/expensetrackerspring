package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.models.ApiResponse
import com.devvikram.expensetracker.expensetracker.models.Category
import com.devvikram.expensetracker.expensetracker.service.CategoryService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    /* ================= CREATE ================= */

    @PostMapping
    fun createCategory(
        @RequestHeader("X-USER-ID") userId: Long,
        @Valid @RequestBody category: Category
    ): ApiResponse<Category> {

        val saved = categoryService.addUserCategory(category, userId)

        return ApiResponse(
            status = true,
            message = "Category created successfully",
            data = saved
        )
    }

    /* ================= UPDATE ================= */

    @PutMapping("/{id}")
    fun updateCategory(
        @PathVariable id: Long,
        @RequestHeader("X-USER-ID") userId: Long,
        @Valid @RequestBody category: Category
    ): ApiResponse<Category> {

        val updated = categoryService.updateUserCategory(id, category, userId)

        return ApiResponse(
            status = true,
            message = "Category updated successfully",
            data = updated
        )
    }

    /* ================= READ ================= */

    @GetMapping
    fun getCategories(
        @RequestHeader("X-USER-ID") userId: Long
    ): ApiResponse<List<Category>> {

        val categories = categoryService.getCategoriesForUser(userId)

        return ApiResponse(
            status = true,
            message = "Categories fetched successfully",
            data = categories
        )
    }

    /* ================= DELETE ================= */

    @DeleteMapping("/{id}")
    fun deleteCategory(
        @PathVariable id: Long,
        @RequestHeader("X-USER-ID") userId: Long
    ): ApiResponse<Unit> {

        categoryService.deleteUserCategory(id, userId)

        return ApiResponse(
            status = true,
            message = "Category deleted successfully"
        )
    }
}
