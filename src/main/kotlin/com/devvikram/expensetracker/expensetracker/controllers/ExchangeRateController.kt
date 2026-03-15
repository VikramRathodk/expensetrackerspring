package com.devvikram.expensetracker.expensetracker.controllers

import com.devvikram.expensetracker.expensetracker.dto.response.ApiResponse
import com.devvikram.expensetracker.expensetracker.dto.response.CurrencyConversionResponse
import com.devvikram.expensetracker.expensetracker.dto.response.ExchangeRateResponse
import com.devvikram.expensetracker.expensetracker.entity.ExchangeRate
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAdmin
import com.devvikram.expensetracker.expensetracker.security.anotation.IsAuthenticated
import com.devvikram.expensetracker.expensetracker.service.ExchangeRateService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/exchange-rates")
@IsAuthenticated
class ExchangeRateController(
    private val exchangeRateService: ExchangeRateService
) {

    /** List all stored exchange rates (base = USD). */
    @GetMapping
    fun getAllRates(): ResponseEntity<ApiResponse<List<ExchangeRateResponse>>> {
        val data = exchangeRateService.getAllRates().map { it.toResponse() }
        return ResponseEntity.ok(ApiResponse(true, "Exchange rates fetched", data))
    }

    /**
     * Convert an amount between two currencies.
     * Example: GET /api/v1/exchange-rates/convert?from=USD&to=INR&amount=100
     */
    @GetMapping("/convert")
    fun convert(
        @RequestParam from: String,
        @RequestParam to: String,
        @RequestParam amount: Double
    ): ResponseEntity<ApiResponse<CurrencyConversionResponse>> {
        val converted = exchangeRateService.convert(amount, from.uppercase(), to.uppercase())
        return ResponseEntity.ok(
            ApiResponse(
                true,
                "Conversion successful",
                CurrencyConversionResponse(
                    fromCurrency    = from.uppercase(),
                    toCurrency      = to.uppercase(),
                    originalAmount  = amount,
                    convertedAmount = converted
                )
            )
        )
    }

    /**
     * Manually trigger a full rate sync from open.er-api.com.
     * Requires ADMIN role.
     */
    @IsAdmin
    @PostMapping("/sync")
    fun syncRates(): ResponseEntity<ApiResponse<Map<String, Int>>> {
        val count = exchangeRateService.syncRates()
        return ResponseEntity.ok(
            ApiResponse(true, "Exchange rates synced successfully", mapOf("ratesUpdated" to count))
        )
    }

    private fun ExchangeRate.toResponse() = ExchangeRateResponse(
        id             = id,
        baseCurrency   = baseCurrency,
        targetCurrency = targetCurrency,
        rate           = rate,
        fetchedAt      = fetchedAt
    )
}
