package com.devvikram.expensetracker.expensetracker.service

data class BudgetCheckResult(
    val shouldBlock: Boolean,
    val warnings: List<String>
)