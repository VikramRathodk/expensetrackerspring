package com.devvikram.expensetracker.expensetracker.specifications


import com.devvikram.expensetracker.expensetracker.dto.request.CustomReportRequest
import com.devvikram.expensetracker.expensetracker.entity.Expense
import com.devvikram.expensetracker.expensetracker.dto.request.ExpenseFilterRequest
import com.devvikram.expensetracker.expensetracker.entity.Category
import com.devvikram.expensetracker.expensetracker.entity.User
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDateTime
import java.time.YearMonth
import jakarta.persistence.criteria.Predicate


object ExpenseSpecifications {

    fun filterByUserId(userId: Long): Specification<Expense> {
        return Specification { root, query, criteriaBuilder ->
            criteriaBuilder.equal(root.get<Long>("userId"), userId)
        }
    }

    fun filterByTitle(title: String?): Specification<Expense> {
        return Specification { root, query, criteriaBuilder ->
            if (title.isNullOrBlank()) {
                criteriaBuilder.conjunction()
            } else {
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")),
                    "%${title.lowercase()}%"
                )
            }
        }
    }

    fun filterByCategory(categoryId: Long?): Specification<Expense> {
        return Specification { root, query, criteriaBuilder ->
            if (categoryId == null) {
                criteriaBuilder.conjunction()
            } else {
                criteriaBuilder.equal(root.get<Long>("category").get<Long>("id"), categoryId)
            }
        }
    }

    fun filterByMinAmount(minAmount: Double?): Specification<Expense> {
        return Specification { root, query, criteriaBuilder ->
            if (minAmount == null) {
                criteriaBuilder.conjunction()
            } else {
                criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount)
            }
        }
    }

    fun filterByMaxAmount(maxAmount: Double?): Specification<Expense> {
        return Specification { root, query, criteriaBuilder ->
            if (maxAmount == null) {
                criteriaBuilder.conjunction()
            } else {
                criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount)
            }
        }
    }

    fun filterByStartDate(startDate: LocalDateTime?): Specification<Expense> {
        return Specification { root, query, criteriaBuilder ->
            if (startDate == null) {
                criteriaBuilder.conjunction()
            } else {
                criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate)
            }
        }
    }

    fun filterByEndDate(endDate: LocalDateTime?): Specification<Expense> {
        return Specification { root, query, criteriaBuilder ->
            if (endDate == null) {
                criteriaBuilder.conjunction()
            } else {
                criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate)
            }
        }
    }
    fun filterByMonthYear(
        year: Int?,
        month: Int?
    ): Specification<Expense> {
        return Specification { root, query, cb ->
            if (year == null || month == null) {
                cb.conjunction()
            } else {
                val yearMonth = YearMonth.of(year, month)

                val start = yearMonth.atDay(1).atStartOfDay()
                val end = yearMonth.atEndOfMonth().atTime(23, 59, 59)

                cb.between(root.get("createdAt"), start, end)
            }
        }
    }

    fun buildFilterSpecification(userId: Long, request: ExpenseFilterRequest): Specification<Expense> {
        return filterByUserId(userId)
            .and(filterByTitle(request.searchTitle))
            .and(filterByCategory(request.categoryId))
            .and(filterByMinAmount(request.minAmount))
            .and(filterByMaxAmount(request.maxAmount))
            .and(filterByStartDate(request.startDate))
            .and(filterByEndDate(request.endDate))
            .and(filterByMonthYear(request.year, request.month))

    }


    fun build(userId: Long, request: CustomReportRequest): Specification<Expense> {
        return Specification { root, query, cb ->

            val predicates = mutableListOf<Predicate>()

            // Always filter by logged-in user
            predicates.add(cb.equal(root.get<User>("user").get<Long>("id"), userId))

            // Date range filter
            if (request.startDate != null && request.endDate != null) {
                predicates.add(
                    cb.between(
                        root.get("createdAt"),
                        request.startDate,
                        request.endDate
                    )
                )
            }

            // Category filter
            if (!request.categoryIds.isNullOrEmpty()) {
                predicates.add(
                    root.get<Category>("category")
                        .get<Long>("id")
                        .`in`(request.categoryIds)
                )
            }

            // Amount range filter
            if (request.minAmount != null && request.maxAmount != null) {
                predicates.add(
                    cb.between(
                        root.get("amount"),
                        request.minAmount,
                        request.maxAmount
                    )
                )
            }

            cb.and(*predicates.toTypedArray())
        }
    }
}