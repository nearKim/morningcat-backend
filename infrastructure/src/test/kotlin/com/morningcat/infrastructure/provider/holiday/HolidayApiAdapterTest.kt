package com.morningcat.infrastructure.provider.holiday

import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.time.LocalDate

class HolidayApiAdapterTest :
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

        "should successfully fetch and parse holidays" {
            // Given
            val apiKey = "test-api-key"
            val year = 2023
            val countryCode = "US"

            val client =
                createMockClient { request ->
                    when {
                        request.url.toString().contains("holidays") -> {
                            respond(
                                content =
                                    """
                                    {
                                        "status": 200,
                                        "holidays": [
                                            {
                                                "name": "Independence Day",
                                                "date": "2023-07-04",
                                                "public": true
                                            },
                                            {
                                                "name": "Christmas",
                                                "date": "2023-12-25",
                                                "public": true
                                            }
                                        ]
                                    }
                                    """.trimIndent(),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> error("Unhandled ${request.url}")
                    }
                }

            val adapter = HolidayApiAdapter(client, apiKey)

            // When
            val result = adapter.getHolidays(year, countryCode)

            // Then
            result shouldBe
                listOf(
                    LocalDate.of(2023, 7, 4),
                    LocalDate.of(2023, 12, 25),
                ).right()
        }

        "should handle network error" {
            // Given
            val apiKey = "test-api-key"
            val year = 2023
            val countryCode = "US"

            val client =
                createMockClient { _ ->
                    throw Exception("Network error")
                }

            val adapter = HolidayApiAdapter(client, apiKey)

            // When
            val result = adapter.getHolidays(year, countryCode)

            // Then
            result shouldBe ProviderError.NetworkError("Network error").left()
        }

        "should sort holidays by date" {
            // Given
            val apiKey = "test-api-key"
            val year = 2023
            val countryCode = "US"

            val client =
                createMockClient { request ->
                    respond(
                        content =
                            """
                            {
                                "status": 200,
                                "holidays": [
                                    {
                                        "name": "Christmas",
                                        "date": "2023-12-25",
                                        "public": true
                                    },
                                    {
                                        "name": "New Year",
                                        "date": "2023-01-01",
                                        "public": true
                                    },
                                    {
                                        "name": "Independence Day",
                                        "date": "2023-07-04",
                                        "public": true
                                    }
                                ]
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val adapter = HolidayApiAdapter(client, apiKey)

            // When
            val result = adapter.getHolidays(year, countryCode)

            // Then - holidays should be sorted by date
            result shouldBe
                listOf(
                    LocalDate.of(2023, 1, 1),
                    LocalDate.of(2023, 7, 4),
                    LocalDate.of(2023, 12, 25),
                ).right()
        }

        "should handle payment required error" {
            // Given
            val apiKey = "expired-api-key"
            val year = 2023
            val countryCode = "US"

            val client =
                createMockClient { request ->
                    respond(
                        content =
                            """
                            {
                                "status": 402,
                                "error": "Subscription expired"
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.PaymentRequired,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val adapter = HolidayApiAdapter(client, apiKey)

            // When
            val result = adapter.getHolidays(year, countryCode)

            // Then
            result shouldBe ProviderError.AuthenticationError("Subscription expired").left()
        }
    })
