package com.morningcat.domain.content.entity

import java.math.BigDecimal
import java.time.LocalDateTime

data class FinancialQuote(
    val symbol: String,
    val name: String,
    val currentPrice: BigDecimal,
    val previousClose: BigDecimal,
    val change: BigDecimal,
    val changePercent: BigDecimal,
    val timestamp: LocalDateTime,
) {
    init {
        require(symbol.isNotBlank()) { "Symbol cannot be blank" }
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(currentPrice > BigDecimal.ZERO) { "Current price must be positive" }
        require(previousClose > BigDecimal.ZERO) { "Previous close must be positive" }
    }

    val isGain: Boolean
        get() = change > BigDecimal.ZERO

    val isLoss: Boolean
        get() = change < BigDecimal.ZERO
}
