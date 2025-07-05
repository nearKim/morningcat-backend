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

class OpenWeatherMapAdapterTest : StringSpec({
    
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
    
    "should successfully fetch weather with One Call API 3.0" {
        // Given
        val apiKey = "test-api-key"
        val location = Location("London", "GB")
        
        val client = createMockClient { request ->
            when {
                request.url.toString().contains("geo/1.0/direct") -> {
                    respond(
                        content = """[{"name": "London", "lat": 51.5074, "lon": -0.1278, "country": "GB"}]""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                request.url.toString().contains("data/3.0/onecall") -> {
                    respond(
                        content = """
                            {
                                "current": {
                                    "dt": 1605182400,
                                    "temp": 20.5,
                                    "humidity": 72,
                                    "uvi": 3.5,
                                    "weather": [{"id": 800, "main": "Clear", "description": "clear sky"}]
                                },
                                "daily": [{
                                    "dt": 1605182400,
                                    "temp": {"min": 18.0, "max": 23.0},
                                    "humidity": 68,
                                    "weather": [{"id": 800, "main": "Clear", "description": "clear sky"}]
                                }]
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> error("Unhandled ${request.url}")
            }
        }
        
        val adapter = OpenWeatherMapAdapter(client, apiKey)
        
        // When
        val result = adapter.fetchWeather(location)
        
        // Then
        result shouldBe WeatherInfo(
            date = LocalDate.now(),
            temperature = WeatherInfo.Temperature(
                min = 18.0,
                max = 23.0,
                current = 20.5
            ),
            condition = "Clear",
            humidity = 72,
            uvIndex = 4, // Rounded from 3.5
            precipitation = 0
        ).right()
    }
    
    "should handle network error during geocoding" {
        // Given
        val apiKey = "test-api-key"
        val location = Location("London", "GB")
        
        val client = createMockClient { _ ->
            throw Exception("Network error")
        }
        
        val adapter = OpenWeatherMapAdapter(client, apiKey)
        
        // When
        val result = adapter.fetchWeather(location)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.NetworkError -> error.cause shouldBe "Failed to get coordinates: Network error"
            else -> error("Expected NetworkError")
        }
    }
    
    "should handle empty geocoding results" {
        // Given
        val apiKey = "test-api-key"
        val location = Location("UnknownCity", "XX")
        
        val client = createMockClient { request ->
            respond(
                content = "[]", // Empty array
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        val adapter = OpenWeatherMapAdapter(client, apiKey)
        
        // When
        val result = adapter.fetchWeather(location)
        
        // Then
        result shouldBe ProviderError.InvalidResponse("Location not found").left()
    }
    
    "should calculate precipitation from rain and snow" {
        // Given
        val apiKey = "test-api-key"
        val location = Location("Oslo", "NO")
        
        val client = createMockClient { request ->
            when {
                request.url.toString().contains("geo/1.0/direct") -> {
                    respond(
                        content = """[{"name": "Oslo", "lat": 59.9139, "lon": 10.7522, "country": "NO"}]""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                request.url.toString().contains("data/3.0/onecall") -> {
                    respond(
                        content = """
                            {
                                "current": {
                                    "dt": 1605182400,
                                    "temp": 5.0,
                                    "humidity": 85,
                                    "uvi": 0.8,
                                    "weather": [{"id": 601, "main": "Snow", "description": "snow"}]
                                },
                                "daily": [{
                                    "dt": 1605182400,
                                    "temp": {"min": 2.0, "max": 6.0},
                                    "humidity": 85,
                                    "weather": [{"id": 601, "main": "Snow", "description": "snow"}],
                                    "rain": 2.5,
                                    "snow": 10.3
                                }]
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> error("Unhandled ${request.url}")
            }
        }
        
        val adapter = OpenWeatherMapAdapter(client, apiKey)
        
        // When
        val result = adapter.fetchWeather(location)
        
        // Then
        result shouldBe WeatherInfo(
            date = LocalDate.now(),
            temperature = WeatherInfo.Temperature(
                min = 2.0,
                max = 6.0,
                current = 5.0
            ),
            condition = "Snow",
            humidity = 85,
            uvIndex = 1, // Rounded from 0.8
            precipitation = 13 // rain (2.5) + snow (10.3) = 12.8, rounded to 13
        ).right()
    }
})