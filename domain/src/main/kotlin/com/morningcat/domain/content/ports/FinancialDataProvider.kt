package com.morningcat.domain.content.ports

import arrow.core.Either
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.FinancialQuote

interface FinancialDataProvider {
    suspend fun fetchQuotes(instruments: Set<String>): Either<ProviderError, List<FinancialQuote>>
}