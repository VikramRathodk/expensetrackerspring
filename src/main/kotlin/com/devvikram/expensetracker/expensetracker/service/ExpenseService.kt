package com.devvikram.expensetracker.expensetracker.service


import com.devvikram.expensetracker.expensetracker.exceptions.ResourceNotFoundException
import com.devvikram.expensetracker.expensetracker.entity.Expense
import com.devvikram.expensetracker.expensetracker.dto.request.ExpenseFilterRequest
import com.devvikram.expensetracker.expensetracker.dto.response.ExpenseResponse
import com.devvikram.expensetracker.expensetracker.dto.request.ExpenseRequest
import com.devvikram.expensetracker.expensetracker.repository.CategoryRepository
import com.devvikram.expensetracker.expensetracker.repository.ExpenseRepository
import com.devvikram.expensetracker.expensetracker.specifications.ExpenseSpecifications
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ExpenseService(private val expenseRepository: ExpenseRepository,private val categoryRepository: CategoryRepository) {

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

        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { ResourceNotFoundException("Category not found") }

        val expense = Expense(
            title = request.title,
            amount = request.amount,
            userId = request.userId,
            note = request.note,
            category = category
        )

        return expenseRepository.save(expense).toResponse()
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

        val updated = existing.copy(
            title = request.title,
            amount = request.amount,
            category = category,
            note = request.note
        )
        return expenseRepository.save(updated).toResponse()
    }

    fun deleteExpense(id: Long, userId: Long): Boolean {
        val expense = expenseRepository.findById(id)
            .filter { it.userId == userId }
            .orElse(null) ?: return false
        expenseRepository.deleteById(id)
        return true
    }

    private fun Expense.toResponse() = ExpenseResponse(
        id = id,
        title = title,
        amount = amount,
        categoryId = category.id,
        categoryName = category.name,
        note = note,
        createdAt = createdAt
    )

}