package com.devvikram.expensetracker.expensetracker.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "exchange_rates",
    uniqueConstraints = [UniqueConstraint(columnNames = ["base_currency", "target_currency"])]
)
data class ExchangeRate(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** Pivot base — always "USD" for our storage scheme. */
    @Column(name = "base_currency", nullable = false, length = 3)
    val baseCurrency: String,

    /** The currency this rate converts to (e.g. "INR", "EUR"). */
    @Column(name = "target_currency", nullable = false, length = 3)
    val targetCurrency: String,

    /** 1 baseCurrency = rate targetCurrency (e.g. 1 USD = 83.5 INR → rate = 83.5). */
    @Column(nullable = false)
    val rate: Double,

    @Column(name = "fetched_at", nullable = false)
    val fetchedAt: LocalDateTime = LocalDateTime.now()
)
