package com.morningcat.infrastructure.provider.financial

import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.FinancialQuote
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class AlphaVantageAdapterIntegrationTest : StringSpec({
    
    lateinit var mockWebServer: MockWebServer
    lateinit var httpClient: HttpClient
    lateinit var adapter: AlphaVantageAdapter
    
    beforeEach {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            engine {
                endpoint {
                    connectTimeout = 5000
                    requestTimeout = 5000
                }
            }
        }
        
        val baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        adapter = AlphaVantageAdapter(httpClient, "test-api-key", baseUrl)
    }
    
    afterEach {
        httpClient.close()
        mockWebServer.shutdown()
    }
    
    "should fetch single stock quote successfully" {
        // Given
        val instruments = setOf("AAPL")
        val mockResponse = """
            {
                "Global Quote": {
                    "01. symbol": "AAPL",
                    "02. open": "175.50",
                    "03. high": "178.00",
                    "04. low": "174.80",
                    "05. price": "177.25",
                    "06. volume": "55123456",
                    "07. latest trading day": "2023-11-12",
                    "08. previous close": "175.00",
                    "09. change": "2.25",
                    "10. change percent": "1.29%"
                }
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // Mock company overview for name
        val overviewResponse = """
            {
                "Symbol": "AAPL",
                "Name": "Apple Inc.",
                "Description": "Apple Inc. designs, manufactures, and markets smartphones...",
                "Exchange": "NASDAQ"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(overviewResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.fetchQuotes(instruments)
        
        // Then - verify requests
        val quoteRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)!!
        quoteRequest.path shouldContain "function=GLOBAL_QUOTE"
        quoteRequest.path shouldContain "symbol=AAPL"
        quoteRequest.path shouldContain "apikey=test-api-key"
        quoteRequest.method shouldBe "GET"
        
        val overviewRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)!!
        overviewRequest.path shouldContain "function=OVERVIEW"
        overviewRequest.path shouldContain "symbol=AAPL"
        
        // Verify response parsing
        result.isRight() shouldBe true
        val quotes = result.getOrNull()!!
        quotes.size shouldBe 1
        quotes[0].symbol shouldBe "AAPL"
        quotes[0].name shouldBe "Apple Inc."
        quotes[0].currentPrice shouldBe BigDecimal("177.25")
        quotes[0].previousClose shouldBe BigDecimal("175.00")
        quotes[0].change shouldBe BigDecimal("2.25")
        quotes[0].changePercent shouldBe BigDecimal("1.29")
    }
    
    "should fetch multiple stock quotes successfully" {
        // Given
        val instruments = setOf("GOOGL", "MSFT")
        
        // Due to concurrent requests, the order is:
        // 1. GOOGL quote request
        // 2. MSFT quote request (concurrent)
        // 3. GOOGL overview request
        // 4. MSFT overview request
        
        // Mock GOOGL quote
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "Global Quote": {
                            "01. symbol": "GOOGL",
                            "02. open": "134.50",
                            "03. high": "136.00",
                            "04. low": "134.00",
                            "05. price": "135.50",
                            "06. volume": "12345678",
                            "07. latest trading day": "2023-11-12",
                            "08. previous close": "134.00",
                            "09. change": "1.50",
                            "10. change percent": "1.12%"
                        }
                    }
                """.trimIndent())
                .addHeader("Content-Type", "application/json")
        )
        
        // Mock MSFT quote (concurrent request)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "Global Quote": {
                            "01. symbol": "MSFT",
                            "02. open": "373.00",
                            "03. high": "376.00",
                            "04. low": "372.50",
                            "05. price": "375.00",
                            "06. volume": "98765432",
                            "07. latest trading day": "2023-11-12",
                            "08. previous close": "373.50",
                            "09. change": "1.50",
                            "10. change percent": "0.40%"
                        }
                    }
                """.trimIndent())
                .addHeader("Content-Type", "application/json")
        )
        
        // Mock GOOGL overview
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"Symbol": "GOOGL", "Name": "Alphabet Inc."}""")
                .addHeader("Content-Type", "application/json")
        )
        
        // Mock MSFT overview
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"Symbol": "MSFT", "Name": "Microsoft Corporation"}""")
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.fetchQuotes(instruments)
        
        // Then
        result.isRight() shouldBe true
        val quotes = result.getOrNull()!!
        quotes.size shouldBe 2
        quotes.map { it.symbol }.toSet() shouldBe setOf("GOOGL", "MSFT")
    }
    
    "should handle API rate limit error" {
        // Given
        val instruments = setOf("AAPL")
        val errorResponse = """
            {
                "Note": "Thank you for using Alpha Vantage! Our standard API call frequency is 5 calls per minute and 500 calls per day."
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200) // Alpha Vantage returns 200 with error message
                .setBody(errorResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.fetchQuotes(instruments)
        
        // Then
        result shouldBe ProviderError.RateLimitExceeded(null).left()
    }
    
    "should handle invalid symbol" {
        // Given
        val instruments = setOf("INVALID_SYMBOL")
        val errorResponse = """
            {
                "Error Message": "Invalid API call. Please retry or visit the documentation."
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(errorResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.fetchQuotes(instruments)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.InvalidResponse -> {
                error.details shouldContain "Symbol not found"
            }
            else -> error("Expected InvalidResponse error")
        }
    }
    
    "should handle network timeout" {
        // Given
        val instruments = setOf("AAPL")
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .setBodyDelay(10, TimeUnit.SECONDS) // Longer than timeout
        )
        
        // When
        val result = adapter.fetchQuotes(instruments)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.NetworkError -> true
            else -> false
        } shouldBe true
    }
    
    "should handle server errors" {
        // Given
        val instruments = setOf("AAPL")
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("""{"error": "Service temporarily unavailable"}""")
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.fetchQuotes(instruments)
        
        // Then
        result shouldBe ProviderError.ServiceUnavailable("Alpha Vantage").left()
    }
    
    "should handle malformed JSON response" {
        // Given
        val instruments = setOf("AAPL")
        val malformedJson = """{ invalid json """
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(malformedJson)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.fetchQuotes(instruments)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.InvalidResponse -> {
                error.details shouldContain "Failed to parse financial data"
            }
            else -> error("Expected InvalidResponse error")
        }
    }
    
    "should skip quotes with invalid data" {
        // Given
        val instruments = setOf("AAPL", "GOOGL")
        
        // Due to concurrent requests, order is:
        // 1. AAPL quote request
        // 2. GOOGL quote request (concurrent)
        // 3. AAPL overview request
        // 4. GOOGL overview request
        
        // Mock AAPL with invalid price (negative)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "Global Quote": {
                            "01. symbol": "AAPL",
                            "05. price": "-177.25",
                            "08. previous close": "175.00",
                            "09. change": "2.25",
                            "10. change percent": "1.29%"
                        }
                    }
                """.trimIndent())
                .addHeader("Content-Type", "application/json")
        )
        
        // Mock GOOGL with valid data (concurrent request)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "Global Quote": {
                            "01. symbol": "GOOGL",
                            "02. open": "134.50",
                            "03. high": "136.00",
                            "04. low": "134.00",
                            "05. price": "135.50",
                            "06. volume": "12345678",
                            "07. latest trading day": "2023-11-12",
                            "08. previous close": "134.00",
                            "09. change": "1.50",
                            "10. change percent": "1.12%"
                        }
                    }
                """.trimIndent())
                .addHeader("Content-Type", "application/json")
        )
        
        // Mock AAPL overview
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"Symbol": "AAPL", "Name": "Apple Inc."}""")
                .addHeader("Content-Type", "application/json")
        )
        
        // Mock GOOGL overview
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"Symbol": "GOOGL", "Name": "Alphabet Inc."}""")
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.fetchQuotes(instruments)
        
        // Then - only valid quote should be returned
        result.isRight() shouldBe true
        val quotes = result.getOrNull()!!
        quotes.size shouldBe 1
        quotes[0].symbol shouldBe "GOOGL"
    }
})