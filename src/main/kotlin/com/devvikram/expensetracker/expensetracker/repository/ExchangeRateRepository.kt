package com.devvikram.expensetracker.expensetracker.repository

import com.devvikram.expensetracker.expensetracker.entity.ExchangeRate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExchangeRateRepository : JpaRepository<ExchangeRate, Long> {
    fun findByBaseCurrency(baseCurrency: String): List<ExchangeRate>
    fun findByBaseCurrencyAndTargetCurrency(base: String, target: String): ExchangeRate?
}
