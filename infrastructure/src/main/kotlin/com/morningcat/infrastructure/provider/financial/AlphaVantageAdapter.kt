package com.morningcat.infrastructure.provider.financial

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.raise.either
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
    
    override suspend fun fetchQuotes(instruments: Set<String>): Either<ProviderError, List<FinancialQuote>> = either {
        coroutineScope {
            val quoteDeferred = instruments.map { symbol ->
                async { fetchSingleQuote(symbol) }
            }
            
            val results = try {
                quoteDeferred.awaitAll()
            } catch (e: Exception) {
                raise(ProviderError.NetworkError(e.message ?: "Unknown error"))
            }
            
            val quotes = results.mapNotNull { result ->
                when (result) {
                    is Either.Right -> result.value
                    is Either.Left -> null // Skip failed quotes
                }
            }
            
            if (quotes.isEmpty() && instruments.isNotEmpty()) {
                // All quotes failed
                results.firstOrNull()?.let {
                    if (it is Either.Left) raise(it.value)
                }
                raise(ProviderError.ServiceUnavailable("Alpha Vantage"))
            }
            
            quotes
        }
    }
    
    private suspend fun fetchSingleQuote(symbol: String): Either<ProviderError, FinancialQuote> = either {
        // Fetch quote data
        val quoteResponse = try {
            httpClient.get(baseUrl) {
                parameter("function", "GLOBAL_QUOTE")
                parameter("symbol", symbol)
                parameter("apikey", apiKey)
            }
        } catch (e: Exception) {
            raise(ProviderError.NetworkError(e.message ?: "Unknown error"))
        }
        
        when (quoteResponse.status) {
            HttpStatusCode.OK -> {
                val responseBody = quoteResponse.bodyAsText()
                
                // Check for rate limit
                if (responseBody.contains("Thank you for using Alpha Vantage")) {
                    raise(ProviderError.RateLimitExceeded(null))
                }
                
                // Check for error message
                if (responseBody.contains("Error Message")) {
                    raise(ProviderError.InvalidResponse("Symbol not found: $symbol"))
                }
                
                val quote = try {
                    quoteResponse.body<GlobalQuoteResponse>()
                } catch (e: Exception) {
                    raise(ProviderError.InvalidResponse("Failed to parse financial data: ${e.message}"))
                }
                
                // Fetch company name from overview
                val companyName = fetchCompanyName(symbol).fold(
                    { symbol }, // Fallback to symbol if name fetch fails
                    { it }
                )
                
                parseQuote(quote, companyName).bind()
            }
            HttpStatusCode.InternalServerError,
            HttpStatusCode.BadGateway,
            HttpStatusCode.ServiceUnavailable,
            HttpStatusCode.GatewayTimeout -> {
                raise(ProviderError.ServiceUnavailable("Alpha Vantage"))
            }
            else -> {
                raise(ProviderError.ServiceUnavailable("Alpha Vantage"))
            }
        }
    }
    
    private suspend fun fetchCompanyName(symbol: String): Either<ProviderError, String> = either {
        val response = try {
            httpClient.get(baseUrl) {
                parameter("function", "OVERVIEW")
                parameter("symbol", symbol)
                parameter("apikey", apiKey)
            }
        } catch (e: Exception) {
            return@either symbol // Return symbol as fallback on any error
        }
        
        if (response.status == HttpStatusCode.OK) {
            val responseBody = response.bodyAsText()
            
            // Check for rate limit in overview request
            if (responseBody.contains("Thank you for using Alpha Vantage")) {
                return@either symbol // Return symbol as fallback on rate limit
            }
            
            try {
                val overview = Json.decodeFromString<CompanyOverviewResponse>(responseBody)
                overview.name
            } catch (e: Exception) {
                symbol // Return symbol as fallback on parse error
            }
        } else {
            symbol // Return symbol as fallback on error
        }
    }
    
    private fun parseQuote(response: GlobalQuoteResponse, companyName: String): Either<ProviderError, FinancialQuote> = either {
        val quote = response.globalQuote
        
        val currentPrice = try {
            BigDecimal(quote.price)
        } catch (e: Exception) {
            raise(ProviderError.InvalidResponse("Invalid quote data for ${quote.symbol}: ${e.message}"))
        }
        
        val previousClose = try {
            BigDecimal(quote.previousClose)
        } catch (e: Exception) {
            raise(ProviderError.InvalidResponse("Invalid quote data for ${quote.symbol}: ${e.message}"))
        }
        
        val change = try {
            BigDecimal(quote.change)
        } catch (e: Exception) {
            raise(ProviderError.InvalidResponse("Invalid quote data for ${quote.symbol}: ${e.message}"))
        }
        
        val changePercent = try {
            quote.changePercent.replace("%", "").toBigDecimal()
        } catch (e: Exception) {
            raise(ProviderError.InvalidResponse("Invalid quote data for ${quote.symbol}: ${e.message}"))
        }
        
        // Validate prices before creating the quote
        if (currentPrice <= BigDecimal.ZERO || previousClose <= BigDecimal.ZERO) {
            raise(ProviderError.InvalidResponse("Invalid price data for ${quote.symbol}: prices must be positive"))
        }
        
        FinancialQuote(
            symbol = quote.symbol,
            name = companyName,
            currentPrice = currentPrice,
            previousClose = previousClose,
            change = change,
            changePercent = changePercent,
            timestamp = LocalDateTime.now()
        )
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