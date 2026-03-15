package com.devvikram.expensetracker.expensetracker.service


import com.devvikram.expensetracker.expensetracker.exceptions.ResourceNotFoundException
import com.devvikram.expensetracker.expensetracker.exceptions.BadRequestException
import com.devvikram.expensetracker.expensetracker.entity.Expense
import com.devvikram.expensetracker.expensetracker.dto.request.ExpenseFilterRequest
import com.devvikram.expensetracker.expensetracker.dto.response.ExpenseResponse
import com.devvikram.expensetracker.expensetracker.dto.request.ExpenseRequest
import com.devvikram.expensetracker.expensetracker.dto.response.TagResponse
import com.devvikram.expensetracker.expensetracker.entity.Tag
import com.devvikram.expensetracker.expensetracker.repository.CategoryRepository
import com.devvikram.expensetracker.expensetracker.repository.ExpenseRepository
import com.devvikram.expensetracker.expensetracker.repository.TagRepository
import com.devvikram.expensetracker.expensetracker.repository.UserRepository
import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import com.devvikram.expensetracker.expensetracker.specifications.ExpenseSpecifications
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val userRepository: UserRepository,
    private val exchangeRateService: ExchangeRateService,
    private val budgetService: BudgetService,
    private val auditLogService: AuditLogService
) {

    fun getAllExpensesPaginated(
        userId: Long,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt"
    ): Page<ExpenseResponse> {
        val pageRequest = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, sortBy)
        )
        return expenseRepository
            .findAll(ExpenseSpecifications.filterByUserId(userId), pageRequest)
            .map { it.toResponse() }
    }

    fun searchExpenses(userId: Long, keyword: String): List<ExpenseResponse> {
        val spec = ExpenseSpecifications.filterByUserId(userId)
            .and(ExpenseSpecifications.filterByTitle(keyword))
        return expenseRepository.findAll(spec)
            .map { it.toResponse() }
            .sortedByDescending { it.createdAt }
    }

    fun filterByCategory(userId: Long, categoryId: Long): List<ExpenseResponse> {
        val spec = ExpenseSpecifications.filterByUserId(userId)
            .and(ExpenseSpecifications.filterByCategory(categoryId))
        return expenseRepository.findAll(spec)
            .map { it.toResponse() }
            .sortedByDescending { it.createdAt }
    }

    fun filterByAmountRange(
        userId: Long,
        minAmount: Double,
        maxAmount: Double
    ): List<ExpenseResponse> {
        require(minAmount >= 0 && maxAmount >= minAmount) { "Invalid amount range" }
        val spec = ExpenseSpecifications.filterByUserId(userId)
            .and(ExpenseSpecifications.filterByMinAmount(minAmount))
            .and(ExpenseSpecifications.filterByMaxAmount(maxAmount))
        return expenseRepository.findAll(spec)
            .map { it.toResponse() }
            .sortedByDescending { it.createdAt }
    }

    fun filterByDateRange(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<ExpenseResponse> {
        require(startDate <= endDate) { "Start date must be before end date" }
        val spec = ExpenseSpecifications.filterByUserId(userId)
            .and(ExpenseSpecifications.filterByStartDate(startDate))
            .and(ExpenseSpecifications.filterByEndDate(endDate))
        return expenseRepository.findAll(spec)
            .map { it.toResponse() }
            .sortedByDescending { it.createdAt }
    }


    fun filterExpenses(
        userId: Long,
        request: ExpenseFilterRequest,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt"
    ): Page<ExpenseResponse> {
        val spec = ExpenseSpecifications.buildFilterSpecification(userId, request)
        val pageRequest = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, sortBy)
        )
        return expenseRepository.findAll(spec, pageRequest)
            .map { it.toResponse() }
    }





    fun createExpense(request: ExpenseRequest): ExpenseResponse {
        val check = budgetService.checkBudgetOnExpense(request.userId, request.categoryId, request.amount)
        if (check.shouldBlock) throw BadRequestException(check.warnings.joinToString(". "))

        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { ResourceNotFoundException("Category not found") }

        val tags = resolveTagsForUser(request.tagIds, request.userId)
        val currency = request.currency.uppercase()
        val baseCurrency = userRepository.findById(request.userId)
            .map { it.baseCurrency }.orElse("INR")
        val amountInBase = exchangeRateService.convert(request.amount, currency, baseCurrency)

        val saved = expenseRepository.save(
            Expense(
                title        = request.title,
                amount       = request.amount,
                currency     = currency,
                amountInBase = amountInBase,
                userId       = request.userId,
                note         = request.note,
                category     = category,
                tags         = tags
            )
        )
        auditLogService.log(
            userId     = saved.userId,
            action     = AuditAction.EXPENSE_CREATED,
            entityType = "Expense",
            entityId   = saved.id,
            newValue   = saved.toResponse()
        )
        return saved.toResponse()
    }


    fun getExpenseById(id: Long, userId: Long): ExpenseResponse? {
        return expenseRepository.findById(id)
            .filter { it.userId == userId }
            .map { it.toResponse() }
            .orElse(null)
    }

    fun updateExpense(id: Long, userId: Long, request: ExpenseRequest): ExpenseResponse? {
        val existing = expenseRepository.findById(id)
            .filter { it.userId == userId }
            .orElse(null) ?: return null
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { ResourceNotFoundException("Category not found") }

        val tags = resolveTagsForUser(request.tagIds, userId)
        val currency = request.currency.uppercase()
        val baseCurrency = userRepository.findById(userId)
            .map { it.baseCurrency }.orElse("INR")
        val amountInBase = exchangeRateService.convert(request.amount, currency, baseCurrency)

        val updated = existing.copy(
            title        = request.title,
            amount       = request.amount,
            currency     = currency,
            amountInBase = amountInBase,
            category     = category,
            note         = request.note,
            tags         = tags
        )
        val saved = expenseRepository.save(updated)
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.EXPENSE_UPDATED,
            entityType = "Expense",
            entityId   = id,
            oldValue   = existing.toResponse(),
            newValue   = saved.toResponse()
        )
        return saved.toResponse()
    }

    fun deleteExpense(id: Long, userId: Long): Boolean {
        val expense = expenseRepository.findById(id)
            .filter { it.userId == userId }
            .orElse(null) ?: return false
        expenseRepository.deleteById(id)
        auditLogService.log(
            userId     = userId,
            action     = AuditAction.EXPENSE_DELETED,
            entityType = "Expense",
            entityId   = id,
            oldValue   = expense.toResponse()
        )
        return true
    }

    private fun resolveTagsForUser(tagIds: List<Long>, userId: Long): MutableList<Tag> =
        tagIds.mapNotNull { tagId ->
            tagRepository.findById(tagId).orElse(null)
                ?.takeIf { it.userId == userId }
        }.toMutableList()

    private fun Expense.toResponse() = ExpenseResponse(
        id           = id,
        title        = title,
        amount       = amount,
        currency     = currency,
        amountInBase = amountInBase,
        categoryId   = category.id,
        categoryName = category.name,
        note         = note,
        createdAt    = createdAt,
        tags         = tags.map { tag ->
            TagResponse(
                id        = tag.id,
                name      = tag.name,
                color     = tag.color,
                userId    = tag.userId,
                createdAt = tag.createdAt
            )
        }
    )

}