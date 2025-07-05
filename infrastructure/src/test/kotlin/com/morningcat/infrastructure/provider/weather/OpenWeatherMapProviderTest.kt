package com.morningcat.infrastructure.provider.weather

import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.WeatherInfo
import com.morningcat.domain.user.valueobject.Location
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.time.LocalDate

class OpenWeatherMapProviderTest : StringSpec({
    
    val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    fun createMockClient(handler: MockRequestHandler): HttpClient {
        return HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
            engine {
                addHandler(handler)
            }
        }
    }
    
    "should fetch weather successfully" {
        // Given
        val apiKey = "test-api-key"
        val location = Location("London", "GB")
        val mockResponse = """
            {
                "weather": [
                    {
                        "main": "Clear",
                        "description": "clear sky"
                    }
                ],
                "main": {
                    "temp": 20.0,
                    "temp_min": 15.0,
                    "temp_max": 25.0,
                    "humidity": 65
                },
                "uvi": 5.2,
                "rain": {
                    "1h": 0.5
                }
            }
        """.trimIndent()
        
        val client = createMockClient { request ->
            when {
                request.url.toString().contains("openweathermap.org") -> {
                    respond(
                        content = mockResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> error("Unhandled ${request.url}")
            }
        }
        
        val provider = OpenWeatherMapProvider(client, apiKey)
        
        // When
        val result = provider.fetchWeather(location)
        
        // Then
        result.shouldBe(
            WeatherInfo(
                date = LocalDate.now(),
                temperature = WeatherInfo.Temperature(
                    min = 15.0,
                    max = 25.0,
                    current = 20.0
                ),
                condition = "Clear",
                humidity = 65,
                uvIndex = 5,
                precipitation = 1
            ).right()
        )
    }
    
    "should handle network error" {
        // Given
        val apiKey = "test-api-key"
        val location = Location("London", "GB")
        
        val client = createMockClient { _ ->
            throw Exception("Network error")
        }
        
        val provider = OpenWeatherMapProvider(client, apiKey)
        
        // When
        val result = provider.fetchWeather(location)
        
        // Then
        result shouldBe ProviderError.NetworkError("Network error").left()
    }
    
    "should handle invalid response" {
        // Given
        val apiKey = "test-api-key"
        val location = Location("London", "GB")
        val invalidResponse = """{"invalid": "response"}"""
        
        val client = createMockClient { request ->
            respond(
                content = invalidResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        val provider = OpenWeatherMapProvider(client, apiKey)
        
        // When
        val result = provider.fetchWeather(location)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.InvalidResponse -> true
            else -> false
        } shouldBe true
    }
    
    "should handle authentication error" {
        // Given
        val apiKey = "invalid-api-key"
        val location = Location("London", "GB")
        
        val client = createMockClient { request ->
            respond(
                content = """{"message": "Invalid API key"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        val provider = OpenWeatherMapProvider(client, apiKey)
        
        // When
        val result = provider.fetchWeather(location)
        
        // Then
        result shouldBe ProviderError.AuthenticationError("Invalid API key").left()
    }
    
    "should handle rate limit exceeded" {
        // Given
        val apiKey = "test-api-key"
        val location = Location("London", "GB")
        
        val client = createMockClient { request ->
            respond(
                content = """{"message": "Rate limit exceeded"}""",
                status = HttpStatusCode.TooManyRequests,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, "application/json")
                    append(HttpHeaders.RetryAfter, "3600")
                }
            )
        }
        
        val provider = OpenWeatherMapProvider(client, apiKey)
        
        // When
        val result = provider.fetchWeather(location)
        
        // Then
        result shouldBe ProviderError.RateLimitExceeded(3600).left()
    }
})