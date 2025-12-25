package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.models.ApiResponse
import com.devvikram.expensetracker.expensetracker.models.Category
import com.devvikram.expensetracker.expensetracker.service.CategoryService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/categories")
class AdminCategoryController(
    private val categoryService: CategoryService
) {

    /* ============== CREATE GLOBAL CATEGORY ============== */
    @PostMapping
    fun createGlobalCategory(
        @Valid @RequestBody category: Category
    ): ApiResponse<Category> {

        val saved = categoryService.addGlobalCategory(category)

        return ApiResponse(
            status = true,
            message = "Global category created successfully",
            data = saved
        )
    }

    /* ============== UPDATE GLOBAL CATEGORY ============== */
    @PutMapping("/{id}")
    fun updateGlobalCategory(
        @PathVariable id: Long,
        @Valid @RequestBody category: Category
    ): ApiResponse<Category> {

        val updated = categoryService.updateGlobalCategory(id, category)

        return ApiResponse(
            status = true,
            message = "Global category updated successfully",
            data = updated
        )
    }

    /* ============== DELETE GLOBAL CATEGORY ============== */
    @DeleteMapping("/{id}")
    fun deleteGlobalCategory(
        @PathVariable id: Long
    ): ApiResponse<Unit> {

        categoryService.deleteGlobalCategory(id)

        return ApiResponse(
            status = true,
            message = "Global category deleted successfully"
        )
    }

}
