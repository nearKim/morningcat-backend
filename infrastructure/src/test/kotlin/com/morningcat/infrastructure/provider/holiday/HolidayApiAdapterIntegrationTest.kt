package com.morningcat.infrastructure.provider.holiday

import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
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
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class HolidayApiAdapterIntegrationTest : StringSpec({
    
    lateinit var mockWebServer: MockWebServer
    lateinit var httpClient: HttpClient
    lateinit var adapter: HolidayApiAdapter
    
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
        adapter = HolidayApiAdapter(httpClient, "test-api-key", baseUrl)
    }
    
    afterEach {
        httpClient.close()
        mockWebServer.shutdown()
    }
    
    "should fetch holidays successfully" {
        // Given
        val year = 2023
        val countryCode = "US"
        val mockResponse = """
            {
                "status": 200,
                "holidays": [
                    {
                        "name": "New Year's Day",
                        "date": "2023-01-01",
                        "observed": "2023-01-02",
                        "public": true
                    },
                    {
                        "name": "Independence Day",
                        "date": "2023-07-04",
                        "observed": "2023-07-04",
                        "public": true
                    },
                    {
                        "name": "Christmas Day",
                        "date": "2023-12-25",
                        "observed": "2023-12-25",
                        "public": true
                    }
                ]
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getHolidays(year, countryCode)
        
        // Then - verify request
        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)!!
        request.path shouldContain "/holidays"
        request.path shouldContain "year=2023"
        request.path shouldContain "country=US"
        request.path shouldContain "api_key=test-api-key"
        request.method shouldBe "GET"
        
        // Verify response parsing
        result shouldBe listOf(
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 7, 4),
            LocalDate.of(2023, 12, 25)
        ).right()
    }
    
    "should handle empty holidays list" {
        // Given
        val year = 2023
        val countryCode = "XX" // Non-existent country
        val emptyResponse = """
            {
                "status": 200,
                "holidays": []
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(emptyResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getHolidays(year, countryCode)
        
        // Then
        result shouldBe emptyList<LocalDate>().right()
    }
    
    "should filter out non-public holidays" {
        // Given
        val year = 2023
        val countryCode = "GB"
        val responseWithMixedHolidays = """
            {
                "status": 200,
                "holidays": [
                    {
                        "name": "New Year's Day",
                        "date": "2023-01-01",
                        "public": true
                    },
                    {
                        "name": "Valentine's Day",
                        "date": "2023-02-14",
                        "public": false
                    },
                    {
                        "name": "Christmas Day",
                        "date": "2023-12-25",
                        "public": true
                    }
                ]
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseWithMixedHolidays)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getHolidays(year, countryCode)
        
        // Then - only public holidays should be returned
        result shouldBe listOf(
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 12, 25)
        ).right()
    }
    
    "should handle API authentication error" {
        // Given
        val year = 2023
        val countryCode = "US"
        val errorResponse = """
            {
                "status": 402,
                "error": "Invalid API key or subscription expired"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(402)
                .setBody(errorResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getHolidays(year, countryCode)
        
        // Then
        result shouldBe ProviderError.AuthenticationError("Invalid API key or subscription expired").left()
    }
    
    "should handle rate limit exceeded" {
        // Given
        val year = 2023
        val countryCode = "US"
        val rateLimitResponse = """
            {
                "status": 429,
                "error": "Rate limit exceeded. Please retry after some time."
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody(rateLimitResponse)
                .addHeader("Content-Type", "application/json")
                .addHeader("Retry-After", "3600")
        )
        
        // When
        val result = adapter.getHolidays(year, countryCode)
        
        // Then
        result shouldBe ProviderError.RateLimitExceeded(3600).left()
    }
    
    "should handle network timeout" {
        // Given
        val year = 2023
        val countryCode = "US"
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .setBodyDelay(10, TimeUnit.SECONDS) // Longer than timeout
        )
        
        // When
        val result = adapter.getHolidays(year, countryCode)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.NetworkError -> true
            else -> false
        } shouldBe true
    }
    
    "should handle server errors" {
        // Given
        val year = 2023
        val countryCode = "US"
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error": "Internal server error"}""")
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getHolidays(year, countryCode)
        
        // Then
        result shouldBe ProviderError.ServiceUnavailable("Holiday API").left()
    }
    
    "should handle malformed JSON response" {
        // Given
        val year = 2023
        val countryCode = "US"
        val malformedJson = """{ invalid json """
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(malformedJson)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getHolidays(year, countryCode)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.InvalidResponse -> {
                error.details shouldContain "Failed to parse holiday data"
            }
            else -> error("Expected InvalidResponse error")
        }
    }
    
    "should handle holidays with invalid date format" {
        // Given
        val year = 2023
        val countryCode = "US"
        val responseWithInvalidDates = """
            {
                "status": 200,
                "holidays": [
                    {
                        "name": "Valid Holiday",
                        "date": "2023-01-01",
                        "public": true
                    },
                    {
                        "name": "Invalid Date Format",
                        "date": "01/15/2023",
                        "public": true
                    },
                    {
                        "name": "Another Valid Holiday",
                        "date": "2023-12-25",
                        "public": true
                    }
                ]
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseWithInvalidDates)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getHolidays(year, countryCode)
        
        // Then - only holidays with valid dates should be returned
        result shouldBe listOf(
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 12, 25)
        ).right()
    }
})