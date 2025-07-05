package com.morningcat.infrastructure.provider.weather

import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.WeatherInfo
import com.morningcat.domain.user.valueobject.Location
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class OpenWeatherMapProviderIntegrationTest : StringSpec({
    
    lateinit var mockWebServer: MockWebServer
    lateinit var httpClient: HttpClient
    lateinit var provider: OpenWeatherMapProvider
    
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
        
        // Create provider with mock server URL
        val baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        provider = OpenWeatherMapProvider(httpClient, "test-api-key", baseUrl)
    }
    
    afterEach {
        httpClient.close()
        mockWebServer.shutdown()
    }
    
    "should send correct request and parse successful weather response" {
        // Given
        val location = Location("London", "GB")
        val mockResponse = """
            {
                "coord": {
                    "lon": -0.1257,
                    "lat": 51.5085
                },
                "weather": [
                    {
                        "id": 800,
                        "main": "Clear",
                        "description": "clear sky",
                        "icon": "01d"
                    }
                ],
                "base": "stations",
                "main": {
                    "temp": 20.5,
                    "feels_like": 19.8,
                    "temp_min": 18.0,
                    "temp_max": 23.0,
                    "pressure": 1013,
                    "humidity": 72
                },
                "visibility": 10000,
                "wind": {
                    "speed": 3.6,
                    "deg": 290
                },
                "clouds": {
                    "all": 0
                },
                "dt": 1605182400,
                "sys": {
                    "type": 1,
                    "id": 1414,
                    "country": "GB",
                    "sunrise": 1605165600,
                    "sunset": 1605198000
                },
                "timezone": 0,
                "id": 2643743,
                "name": "London",
                "cod": 200
            }
        """.trimIndent()
        
        // Also mock the UV index endpoint
        val uvResponse = """
            {
                "lat": 51.5085,
                "lon": -0.1257,
                "date_iso": "2023-11-12T12:00:00Z",
                "date": 1699790400,
                "value": 3.5
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(uvResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = provider.fetchWeather(location)
        
        // Then - verify the request
        val weatherRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)!!
        weatherRequest.path shouldContain "/data/2.5/weather"
        weatherRequest.path shouldContain "q=London%2CGB" // URL encoded comma
        weatherRequest.path shouldContain "appid=test-api-key"
        weatherRequest.path shouldContain "units=metric"
        weatherRequest.method shouldBe "GET"
        
        val uvRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)!!
        uvRequest.path shouldContain "/data/2.5/uvi"
        uvRequest.path shouldContain "lat=51.5085"
        uvRequest.path shouldContain "lon=-0.1257"
        
        // Verify the response parsing
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
    
    "should handle API authentication error" {
        // Given
        val location = Location("London", "GB")
        val errorResponse = """
            {
                "cod": 401,
                "message": "Invalid API key. Please see https://openweathermap.org/faq#error401 for more info."
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(errorResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = provider.fetchWeather(location)
        
        // Then
        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)!!
        request.path shouldContain "appid=test-api-key"
        
        result shouldBe ProviderError.AuthenticationError(
            "Invalid API key. Please see https://openweathermap.org/faq#error401 for more info."
        ).left()
    }
    
    "should handle rate limit exceeded with retry-after header" {
        // Given
        val location = Location("London", "GB")
        val errorResponse = """
            {
                "cod": 429,
                "message": "You have exceeded your API call limit."
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody(errorResponse)
                .addHeader("Content-Type", "application/json")
                .addHeader("Retry-After", "3600")
        )
        
        // When
        val result = provider.fetchWeather(location)
        
        // Then
        result shouldBe ProviderError.RateLimitExceeded(3600).left()
    }
    
    "should handle malformed JSON response" {
        // Given
        val location = Location("London", "GB")
        val malformedResponse = """
            {
                "weather": "not an array",
                "main": {
                    "temp": "not a number"
                }
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(malformedResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = provider.fetchWeather(location)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.InvalidResponse -> {
                error.details shouldContain "Failed to parse weather data"
            }
            else -> error("Expected InvalidResponse error")
        }
    }
    
    "should handle network timeout" {
        // Given
        val location = Location("London", "GB")
        
        // Enqueue a response with a long delay
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .setBodyDelay(10, TimeUnit.SECONDS) // Longer than our timeout
        )
        
        // When
        val result = provider.fetchWeather(location)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.NetworkError -> true
            else -> false
        } shouldBe true
    }
    
    "should handle 5xx server errors" {
        // Given
        val location = Location("London", "GB")
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("""{"message": "Service temporarily unavailable"}""")
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = provider.fetchWeather(location)
        
        // Then
        result shouldBe ProviderError.ServiceUnavailable("OpenWeatherMap").left()
    }
    
    "should handle missing required fields in response" {
        // Given
        val location = Location("London", "GB")
        val incompleteResponse = """
            {
                "weather": [
                    {
                        "main": "Clear"
                    }
                ],
                "main": {
                    "temp": 20.0
                }
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(incompleteResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = provider.fetchWeather(location)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.InvalidResponse -> true
            else -> false
        } shouldBe true
    }
    
    "should include rain data when available" {
        // Given
        val location = Location("Paris", "FR")
        val responseWithRain = """
            {
                "coord": {"lon": 2.3488, "lat": 48.8534},
                "weather": [{"main": "Rain", "description": "light rain"}],
                "main": {
                    "temp": 15.0,
                    "temp_min": 14.0,
                    "temp_max": 16.0,
                    "humidity": 85
                },
                "rain": {
                    "1h": 2.5,
                    "3h": 5.0
                },
                "cod": 200
            }
        """.trimIndent()
        
        val uvResponse = """{"value": 2.0}"""
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseWithRain)
                .addHeader("Content-Type", "application/json")
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(uvResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = provider.fetchWeather(location)
        
        // Then
        result shouldBe WeatherInfo(
            date = LocalDate.now(),
            temperature = WeatherInfo.Temperature(
                min = 14.0,
                max = 16.0,
                current = 15.0
            ),
            condition = "Rain",
            humidity = 85,
            uvIndex = 2,
            precipitation = 3 // Rounded from 2.5
        ).right()
    }
})