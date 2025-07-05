package com.morningcat.infrastructure.provider.financial

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.FinancialQuote
import com.morningcat.domain.content.ports.FinancialDataProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDateTime

class AlphaVantageAdapter(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "https://www.alphavantage.co/query"
) : FinancialDataProvider {
    
    override suspend fun fetchQuotes(instruments: Set<String>): Either<ProviderError, List<FinancialQuote>> {
        return try {
            coroutineScope {
                val quoteDeferred = instruments.map { symbol ->
                    async { fetchSingleQuote(symbol) }
                }
                
                val results = quoteDeferred.awaitAll()
                val quotes = results.mapNotNull { result ->
                    when (result) {
                        is Either.Right -> result.value
                        is Either.Left -> null // Skip failed quotes
                    }
                }
                
                if (quotes.isEmpty() && instruments.isNotEmpty()) {
                    // All quotes failed
                    results.firstOrNull()?.let {
                        if (it is Either.Left) return@coroutineScope it.value.left()
                    }
                    ProviderError.ServiceUnavailable("Alpha Vantage").left()
                } else {
                    quotes.right()
                }
            }
        } catch (e: Exception) {
            ProviderError.NetworkError(e.message ?: "Unknown error").left()
        }
    }
    
    private suspend fun fetchSingleQuote(symbol: String): Either<ProviderError, FinancialQuote> {
        // Fetch quote data
        val quoteResponse = httpClient.get(baseUrl) {
            parameter("function", "GLOBAL_QUOTE")
            parameter("symbol", symbol)
            parameter("apikey", apiKey)
        }
        
        when (quoteResponse.status) {
            HttpStatusCode.OK -> {
                val responseBody = quoteResponse.bodyAsText()
                
                // Check for rate limit
                if (responseBody.contains("Thank you for using Alpha Vantage")) {
                    return ProviderError.RateLimitExceeded(null).left()
                }
                
                // Check for error message
                if (responseBody.contains("Error Message")) {
                    return ProviderError.InvalidResponse("Symbol not found: $symbol").left()
                }
                
                val quote = try {
                    quoteResponse.body<GlobalQuoteResponse>()
                } catch (e: Exception) {
                    return ProviderError.InvalidResponse("Failed to parse financial data: ${e.message}").left()
                }
                
                // Fetch company name from overview
                val companyName = try {
                    val nameResult = fetchCompanyName(symbol)
                    when (nameResult) {
                        is Either.Right -> nameResult.value
                        is Either.Left -> symbol // Fallback to symbol if name fetch fails
                    }
                } catch (e: Exception) {
                    symbol // Fallback to symbol if name fetch fails
                }
                
                return parseQuote(quote, companyName)
            }
            HttpStatusCode.InternalServerError,
            HttpStatusCode.BadGateway,
            HttpStatusCode.ServiceUnavailable,
            HttpStatusCode.GatewayTimeout -> {
                return ProviderError.ServiceUnavailable("Alpha Vantage").left()
            }
            else -> {
                return ProviderError.ServiceUnavailable("Alpha Vantage").left()
            }
        }
    }
    
    private suspend fun fetchCompanyName(symbol: String): Either<ProviderError, String> {
        return try {
            val response = httpClient.get(baseUrl) {
                parameter("function", "OVERVIEW")
                parameter("symbol", symbol)
                parameter("apikey", apiKey)
            }
            
            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                
                // Check for rate limit in overview request
                if (responseBody.contains("Thank you for using Alpha Vantage")) {
                    return symbol.right() // Return symbol as fallback on rate limit
                }
                
                try {
                    val overview = Json.decodeFromString<CompanyOverviewResponse>(responseBody)
                    overview.name.right()
                } catch (e: Exception) {
                    symbol.right() // Return symbol as fallback on parse error
                }
            } else {
                symbol.right() // Return symbol as fallback on error
            }
        } catch (e: Exception) {
            symbol.right() // Return symbol as fallback on any error
        }
    }
    
    private fun parseQuote(response: GlobalQuoteResponse, companyName: String): Either<ProviderError, FinancialQuote> {
        val quote = response.globalQuote
        
        return try {
            val currentPrice = BigDecimal(quote.price)
            val previousClose = BigDecimal(quote.previousClose)
            val change = BigDecimal(quote.change)
            val changePercent = quote.changePercent
                .replace("%", "")
                .toBigDecimal()
            
            // Validate prices before creating the quote
            if (currentPrice <= BigDecimal.ZERO || previousClose <= BigDecimal.ZERO) {
                return ProviderError.InvalidResponse("Invalid price data for ${quote.symbol}: prices must be positive").left()
            }
            
            FinancialQuote(
                symbol = quote.symbol,
                name = companyName,
                currentPrice = currentPrice,
                previousClose = previousClose,
                change = change,
                changePercent = changePercent,
                timestamp = LocalDateTime.now()
            ).right()
        } catch (e: Exception) {
            ProviderError.InvalidResponse("Invalid quote data for ${quote.symbol}: ${e.message}").left()
        }
    }
    
    @Serializable
    private data class GlobalQuoteResponse(
        @SerialName("Global Quote")
        val globalQuote: GlobalQuote
    )
    
    @Serializable
    private data class GlobalQuote(
        @SerialName("01. symbol")
        val symbol: String,
        @SerialName("02. open")
        val open: String? = null,
        @SerialName("03. high")
        val high: String? = null,
        @SerialName("04. low")
        val low: String? = null,
        @SerialName("05. price")
        val price: String,
        @SerialName("06. volume")
        val volume: String? = null,
        @SerialName("07. latest trading day")
        val latestTradingDay: String? = null,
        @SerialName("08. previous close")
        val previousClose: String,
        @SerialName("09. change")
        val change: String,
        @SerialName("10. change percent")
        val changePercent: String
    )
    
    @Serializable
    private data class CompanyOverviewResponse(
        @SerialName("Symbol")
        val symbol: String,
        @SerialName("Name")
        val name: String,
        @SerialName("Description")
        val description: String? = null,
        @SerialName("Exchange")
        val exchange: String? = null
    )
}