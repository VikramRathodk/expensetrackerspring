package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.dto.request.TagRequest
import com.devvikram.expensetracker.expensetracker.dto.response.TagResponse
import com.devvikram.expensetracker.expensetracker.entity.Tag
import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import com.devvikram.expensetracker.expensetracker.exceptions.ConflictException
import com.devvikram.expensetracker.expensetracker.exceptions.ResourceNotFoundException
import com.devvikram.expensetracker.expensetracker.repository.TagRepository
import org.springframework.stereotype.Service

@Service
class TagService(
    private val tagRepository: TagRepository,
    private val auditLogService: AuditLogService
) {

    fun createTag(userId: Long, request: TagRequest): TagResponse {
        if (tagRepository.existsByNameAndUserId(request.name, userId)) {
            throw ConflictException("Tag '${request.name}' already exists")
        }
        val tag = tagRepository.save(
            Tag(name = request.name, color = request.color, userId = userId)
        )
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.TAG_CREATED,
            entityType = "Tag",
            entityId   = tag.id,
            newValue   = tag.toResponse()
        )
        return tag.toResponse()
    }

    fun getTagsByUser(userId: Long): List<TagResponse> =
        tagRepository.findByUserId(userId).map { it.toResponse() }

    fun getTagById(id: Long, userId: Long): TagResponse =
        (tagRepository.findByIdAndUserId(id, userId)
            ?: throw ResourceNotFoundException("Tag not found")).toResponse()

    fun updateTag(id: Long, userId: Long, request: TagRequest): TagResponse {
        val existing = tagRepository.findByIdAndUserId(id, userId)
            ?: throw ResourceNotFoundException("Tag not found")

        if (existing.name != request.name && tagRepository.existsByNameAndUserId(request.name, userId)) {
            throw ConflictException("Tag '${request.name}' already exists")
        }

        val updated = tagRepository.save(existing.copy(name = request.name, color = request.color))
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.TAG_UPDATED,
            entityType = "Tag",
            entityId   = id,
            oldValue   = existing.toResponse(),
            newValue   = updated.toResponse()
        )
        return updated.toResponse()
    }

    fun deleteTag(id: Long, userId: Long) {
        val tag = tagRepository.findByIdAndUserId(id, userId)
            ?: throw ResourceNotFoundException("Tag not found")
        tagRepository.deleteById(id)
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.TAG_DELETED,
            entityType = "Tag",
            entityId   = id,
            oldValue   = tag.toResponse()
        )
    }

    private fun Tag.toResponse() = TagResponse(
        id        = id,
        name      = name,
        color     = color,
        userId    = userId,
        createdAt = createdAt
    )
}
