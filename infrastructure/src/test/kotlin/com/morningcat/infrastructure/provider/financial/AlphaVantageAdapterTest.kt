package com.morningcat.infrastructure.provider.financial

import arrow.core.left
import com.morningcat.domain.common.error.ProviderError
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.math.BigDecimal

class AlphaVantageAdapterTest :
    StringSpec({

        val json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

        fun createMockClient(handler: MockRequestHandler): HttpClient =
            HttpClient(MockEngine) {
                install(ContentNegotiation) {
                    json(json)
                }
                engine {
                    addHandler(handler)
                }
            }

        "should successfully fetch and parse stock quote" {
            // Given
            val apiKey = "test-api-key"
            val instruments = setOf("AAPL")

            val client =
                createMockClient { request ->
                    when {
                        request.url.toString().contains("function=GLOBAL_QUOTE") -> {
                            respond(
                                content =
                                    """
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
                                    """.trimIndent(),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.toString().contains("function=OVERVIEW") -> {
                            respond(
                                content = """{"Symbol": "AAPL", "Name": "Apple Inc."}""",
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> error("Unhandled ${request.url}")
                    }
                }

            val adapter = AlphaVantageAdapter(client, apiKey)

            // When
            val result = adapter.fetchQuotes(instruments)

            // Then
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

        "should handle network error" {
            // Given
            val apiKey = "test-api-key"
            val instruments = setOf("AAPL")

            val client =
                createMockClient { _ ->
                    throw Exception("Network error")
                }

            val adapter = AlphaVantageAdapter(client, apiKey)

            // When
            val result = adapter.fetchQuotes(instruments)

            // Then
            result shouldBe ProviderError.NetworkError("Network error").left()
        }

        "should handle rate limit response" {
            // Given
            val apiKey = "test-api-key"
            val instruments = setOf("AAPL")

            val client =
                createMockClient { request ->
                    respond(
                        content =
                            """
                            {
                                "Note": "Thank you for using Alpha Vantage! Our standard API call frequency is 5 calls per minute."
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val adapter = AlphaVantageAdapter(client, apiKey)

            // When
            val result = adapter.fetchQuotes(instruments)

            // Then
            result shouldBe ProviderError.RateLimitExceeded(null).left()
        }

        "should use symbol as name when overview fails" {
            // Given
            val apiKey = "test-api-key"
            val instruments = setOf("AAPL")

            val client =
                createMockClient { request ->
                    when {
                        request.url.toString().contains("function=GLOBAL_QUOTE") -> {
                            respond(
                                content =
                                    """
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
                                    """.trimIndent(),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.toString().contains("function=OVERVIEW") -> {
                            respond(
                                content = """{"Error Message": "Invalid API call"}""",
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> error("Unhandled ${request.url}")
                    }
                }

            val adapter = AlphaVantageAdapter(client, apiKey)

            // When
            val result = adapter.fetchQuotes(instruments)

            // Then
            result.isRight() shouldBe true
            val quotes = result.getOrNull()!!
            quotes.size shouldBe 1
            quotes[0].symbol shouldBe "AAPL"
            quotes[0].name shouldBe "AAPL" // Fallback to symbol
        }
    })
