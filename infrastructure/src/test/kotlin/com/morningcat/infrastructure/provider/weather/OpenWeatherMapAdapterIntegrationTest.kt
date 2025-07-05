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
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class OpenWeatherMapAdapterIntegrationTest : StringSpec({
    
    lateinit var mockWebServer: MockWebServer
    lateinit var httpClient: HttpClient
    lateinit var adapter: OpenWeatherMapAdapter
    
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
        
        // Create adapter with mock server URL
        val baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        adapter = OpenWeatherMapAdapter(httpClient, "test-api-key", baseUrl)
    }
    
    afterEach {
        httpClient.close()
        mockWebServer.shutdown()
    }
    
    "should fetch weather using One Call API 3.0 with geocoding" {
        // Given
        val location = Location("London", "GB")
        
        // Mock geocoding response
        val geocodingResponse = """
            [
                {
                    "name": "London",
                    "lat": 51.5074,
                    "lon": -0.1278,
                    "country": "GB",
                    "state": "England"
                }
            ]
        """.trimIndent()
        
        // Mock One Call API 3.0 response
        val oneCallResponse = """
            {
                "lat": 51.5074,
                "lon": -0.1278,
                "timezone": "Europe/London",
                "timezone_offset": 0,
                "current": {
                    "dt": 1605182400,
                    "sunrise": 1605165600,
                    "sunset": 1605198000,
                    "temp": 20.5,
                    "feels_like": 19.8,
                    "pressure": 1013,
                    "humidity": 72,
                    "dew_point": 15.3,
                    "uvi": 3.5,
                    "clouds": 10,
                    "visibility": 10000,
                    "wind_speed": 3.6,
                    "wind_deg": 290,
                    "weather": [
                        {
                            "id": 800,
                            "main": "Clear",
                            "description": "clear sky",
                            "icon": "01d"
                        }
                    ]
                },
                "daily": [
                    {
                        "dt": 1605182400,
                        "sunrise": 1605165600,
                        "sunset": 1605198000,
                        "moonrise": 1605178800,
                        "moonset": 1605213600,
                        "moon_phase": 0.25,
                        "temp": {
                            "day": 21.0,
                            "min": 18.0,
                            "max": 23.0,
                            "night": 19.0,
                            "eve": 20.0,
                            "morn": 18.5
                        },
                        "feels_like": {
                            "day": 20.5,
                            "night": 18.5,
                            "eve": 19.5,
                            "morn": 18.0
                        },
                        "pressure": 1013,
                        "humidity": 68,
                        "dew_point": 15.0,
                        "wind_speed": 3.5,
                        "wind_deg": 290,
                        "wind_gust": 5.0,
                        "weather": [
                            {
                                "id": 800,
                                "main": "Clear",
                                "description": "clear sky",
                                "icon": "01d"
                            }
                        ],
                        "clouds": 10,
                        "pop": 0.1,
                        "uvi": 3.8
                    }
                ]
            }
        """.trimIndent()
        
        // Enqueue responses
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(geocodingResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(oneCallResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.fetchWeather(location)
        
        // Then - verify geocoding request
        val geocodingRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)!!
        geocodingRequest.path shouldContain "/geo/1.0/direct"
        geocodingRequest.path shouldContain "q=London%2CGB"
        geocodingRequest.path shouldContain "limit=1"
        geocodingRequest.path shouldContain "appid=test-api-key"
        geocodingRequest.method shouldBe "GET"
        
        // Verify One Call API request
        val oneCallRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)!!
        oneCallRequest.path shouldContain "/data/3.0/onecall"
        oneCallRequest.path shouldContain "lat=51.5074"
        oneCallRequest.path shouldContain "lon=-0.1278"
        oneCallRequest.path shouldContain "units=metric"
        oneCallRequest.path shouldContain "exclude=minutely%2Chourly%2Calerts"
        
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
    
    "should handle rain and snow precipitation data" {
        // Given
        val location = Location("Oslo", "NO")
        
        val geocodingResponse = """[{"name": "Oslo", "lat": 59.9139, "lon": 10.7522, "country": "NO"}]"""
        
        val oneCallResponseWithPrecipitation = """
            {
                "lat": 59.9139,
                "lon": 10.7522,
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
                    "snow": 10.3,
                    "uvi": 0.8
                }]
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(geocodingResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(oneCallResponseWithPrecipitation)
                .addHeader("Content-Type", "application/json")
        )
        
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
    
    "should handle geocoding failure" {
        // Given
        val location = Location("NonexistentCity", "XX")
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]") // Empty array means no location found
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.fetchWeather(location)
        
        // Then
        result shouldBe ProviderError.InvalidResponse("Location not found").left()
    }
    
    "should handle One Call API authentication error" {
        // Given
        val location = Location("London", "GB")
        
        // Mock successful geocoding
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"name": "London", "lat": 51.5074, "lon": -0.1278, "country": "GB"}]""")
                .addHeader("Content-Type", "application/json")
        )
        
        // Mock 401 from One Call API
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"cod": 401, "message": "Invalid API key. Please see https://openweathermap.org/faq#error401 for more info."}""")
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.fetchWeather(location)
        
        // Then
        result shouldBe ProviderError.AuthenticationError(
            "Invalid API key. Please see https://openweathermap.org/faq#error401 for more info."
        ).left()
    }
    
    "should handle rate limit exceeded" {
        // Given
        val location = Location("London", "GB")
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"name": "London", "lat": 51.5074, "lon": -0.1278, "country": "GB"}]""")
                .addHeader("Content-Type", "application/json")
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("""{"message": "You have exceeded your API call limit."}""")
                .addHeader("Content-Type", "application/json")
                .addHeader("Retry-After", "3600")
        )
        
        // When
        val result = adapter.fetchWeather(location)
        
        // Then
        result shouldBe ProviderError.RateLimitExceeded(3600).left()
    }
    
    "should handle network timeout" {
        // Given
        val location = Location("London", "GB")
        
        // Enqueue a response with a long delay for geocoding
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]")
                .setBodyDelay(10, TimeUnit.SECONDS) // Longer than our timeout
        )
        
        // When
        val result = adapter.fetchWeather(location)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.NetworkError -> true
            else -> false
        } shouldBe true
    }
    
    "should handle malformed One Call API response" {
        // Given
        val location = Location("London", "GB")
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"name": "London", "lat": 51.5074, "lon": -0.1278, "country": "GB"}]""")
                .addHeader("Content-Type", "application/json")
        )
        
        // Invalid One Call response
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"invalid": "response"}""")
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.fetchWeather(location)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.InvalidResponse -> {
                error.details shouldContain "Failed to parse weather data"
            }
            else -> error("Expected InvalidResponse error")
        }
    }
    
    "should handle server errors gracefully" {
        // Given
        val location = Location("London", "GB")
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"name": "London", "lat": 51.5074, "lon": -0.1278, "country": "GB"}]""")
                .addHeader("Content-Type", "application/json")
        )
        
        // Test 500 Internal Server Error
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"message": "Internal server error"}""")
                .addHeader("Content-Type", "application/json")
        )
        
        val result500 = adapter.fetchWeather(location)
        result500 shouldBe ProviderError.ServiceUnavailable("OpenWeatherMap").left()
        
        // Test 503 Service Unavailable
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"name": "London", "lat": 51.5074, "lon": -0.1278, "country": "GB"}]""")
                .addHeader("Content-Type", "application/json")
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("""{"message": "Service temporarily unavailable"}""")
                .addHeader("Content-Type", "application/json")
        )
        
        val result503 = adapter.fetchWeather(location)
        result503 shouldBe ProviderError.ServiceUnavailable("OpenWeatherMap").left()
    }
})