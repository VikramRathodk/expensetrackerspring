package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.entity.ExchangeRate
import com.devvikram.expensetracker.expensetracker.repository.ExchangeRateRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.LocalDateTime

/**
 * Fetches exchange rates from open.er-api.com (free, no API key required).
 *
 * Storage schema (pivot = USD):
 *   base_currency = "USD", target_currency = "INR", rate = 83.5
 *   → 1 USD = 83.5 INR
 *
 * Cross-rate formula for converting amount_A (currency A) to currency B:
 *   amountInB = amount_A × ( rateUSD→B / rateUSD→A )
 */
@Service
class ExchangeRateService(
    private val exchangeRateRepository: ExchangeRateRepository
) {

    private val log = LoggerFactory.getLogger(ExchangeRateService::class.java)
    private val apiBase = "https://open.er-api.com/v6/latest"
    private val pivotCurrency = "USD"
    private val restClient = RestClient.builder().build()

    // ── API response shape ────────────────────────────────────────────────────

    data class ApiResponse(
        val result: String = "",
        val base_code: String = "",
        val rates: Map<String, Double> = emptyMap()
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns all stored rates with USD as base.
     */
    fun getAllRates(): List<ExchangeRate> =
        exchangeRateRepository.findByBaseCurrency(pivotCurrency)

    /**
     * Converts [amount] from [fromCurrency] to [toCurrency] using stored USD-pivot rates.
     * Falls back to 1:1 if rates are not yet loaded.
     */
    fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return amount
        val rateFrom = exchangeRateRepository
            .findByBaseCurrencyAndTargetCurrency(pivotCurrency, fromCurrency)?.rate ?: run {
            log.warn("No rate found for {}, defaulting to 1:1", fromCurrency)
            return amount
        }
        val rateTo = exchangeRateRepository
            .findByBaseCurrencyAndTargetCurrency(pivotCurrency, toCurrency)?.rate ?: run {
            log.warn("No rate found for {}, defaulting to 1:1", toCurrency)
            return amount
        }
        // amount × (rateUSD→toCurrency / rateUSD→fromCurrency)
        return amount * (rateTo / rateFrom)
    }

    /**
     * Manually trigger a full rate sync. Returns the number of rates stored.
     */
    @Transactional
    fun syncRates(): Int {
        log.info("Starting exchange rate sync...")
        return fetchAndStore()
    }

    // ── Scheduler — runs daily at 01:00 ──────────────────────────────────────

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    fun scheduledSync() {
        try {
            val count = fetchAndStore()
            log.info("Scheduled exchange rate sync complete. {} rates updated.", count)
        } catch (ex: Exception) {
            log.error("Scheduled exchange rate sync failed: {}", ex.message)
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun fetchAndStore(): Int {
        val response = try {
            restClient.get()
                .uri("$apiBase/$pivotCurrency")
                .retrieve()
                .body<ApiResponse>()
        } catch (ex: Exception) {
            log.error("Failed to fetch exchange rates from API: {}", ex.message)
            return 0
        }

        if (response == null || response.result != "success") {
            log.error("Exchange rate API returned non-success response")
            return 0
        }

        val now = LocalDateTime.now()
        var count = 0

        for ((target, rate) in response.rates) {
            if (target.length != 3) continue       // skip malformed codes
            val existing = exchangeRateRepository
                .findByBaseCurrencyAndTargetCurrency(pivotCurrency, target)
            if (existing != null) {
                exchangeRateRepository.save(existing.copy(rate = rate, fetchedAt = now))
            } else {
                exchangeRateRepository.save(
                    ExchangeRate(
                        baseCurrency   = pivotCurrency,
                        targetCurrency = target,
                        rate           = rate,
                        fetchedAt      = now
                    )
                )
            }
            count++
        }
        log.info("Stored/updated {} exchange rates (base={})", count, pivotCurrency)
        return count
    }
}
