package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.request.TagRequest
import com.devvikram.expensetracker.expensetracker.dto.response.ApiResponse
import com.devvikram.expensetracker.expensetracker.dto.response.TagResponse
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAuthenticated
import com.devvikram.expensetracker.expensetracker.service.TagService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tags")
@IsAuthenticated
class TagController(
    private val tagService: TagService,
    private val userRepository: UserRepository
) {

    private fun getUserId(userDetails: UserDetails): Long =
        userRepository.findByEmail(userDetails.username)
            .orElseThrow { RuntimeException("User not found") }
            .id

    @PostMapping
    fun createTag(
        @Valid @RequestBody request: TagRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<TagResponse>> {
        val data = tagService.createTag(getUserId(userDetails), request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(true, "Tag created", data))
    }

    @GetMapping
    fun getAllTags(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<TagResponse>>> {
        val data = tagService.getTagsByUser(getUserId(userDetails))
        return ResponseEntity.ok(ApiResponse(true, "Tags fetched", data))
    }

    @GetMapping("/{id}")
    fun getTag(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<TagResponse>> {
        val data = tagService.getTagById(id, getUserId(userDetails))
        return ResponseEntity.ok(ApiResponse(true, "Tag fetched", data))
    }

    @PutMapping("/{id}")
    fun updateTag(
        @PathVariable id: Long,
        @Valid @RequestBody request: TagRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<TagResponse>> {
        val data = tagService.updateTag(id, getUserId(userDetails), request)
        return ResponseEntity.ok(ApiResponse(true, "Tag updated", data))
    }

    @DeleteMapping("/{id}")
    fun deleteTag(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Nothing>> {
        tagService.deleteTag(id, getUserId(userDetails))
        return ResponseEntity.ok(ApiResponse(true, "Tag deleted"))
    }
}
