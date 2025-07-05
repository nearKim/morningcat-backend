package com.morningcat.infrastructure.provider.calendar

import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.CalendarEvent
import com.morningcat.domain.user.aggregate.User
import com.morningcat.domain.user.valueobject.UserId
import com.morningcat.domain.user.valueobject.EmailAddress
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
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class GoogleCalendarAdapterIntegrationTest : StringSpec({
    
    lateinit var mockWebServer: MockWebServer
    lateinit var httpClient: HttpClient
    lateinit var adapter: GoogleCalendarAdapter
    
    val testUser = User.register(
        id = UserId.generate(),
        email = EmailAddress.create("test@example.com").getOrNull()!!,
        name = "Test User"
    )
    
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
        adapter = GoogleCalendarAdapter(httpClient, "test-api-key", baseUrl)
    }
    
    afterEach {
        try {
            httpClient.close()
            mockWebServer.shutdown()
        } catch (e: Exception) {
            // Ignore shutdown errors for timeout tests
        }
    }
    
    "should fetch calendar events successfully" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        val mockResponse = """
            {
                "kind": "calendar#events",
                "summary": "user@example.com",
                "items": [
                    {
                        "id": "event1",
                        "summary": "Team Meeting",
                        "description": "Weekly team sync",
                        "location": "Conference Room A",
                        "start": {
                            "dateTime": "2023-11-15T10:00:00Z"
                        },
                        "end": {
                            "dateTime": "2023-11-15T11:00:00Z"
                        }
                    },
                    {
                        "id": "event2",
                        "summary": "Lunch with Client",
                        "description": "Discuss project requirements",
                        "location": "Downtown Restaurant",
                        "start": {
                            "dateTime": "2023-11-15T12:30:00Z"
                        },
                        "end": {
                            "dateTime": "2023-11-15T14:00:00Z"
                        }
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
        val result = adapter.getEvents(testUser, date)
        
        // Then - verify request
        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)!!
        request.path shouldContain "events"
        request.path shouldContain "timeMin=2023-11-15T00%3A00%3A00Z" // URL encoded
        request.path shouldContain "timeMax=2023-11-15T23%3A59%3A59Z" // URL encoded
        request.path shouldContain "singleEvents=true"
        request.path shouldContain "orderBy=startTime"
        request.method shouldBe "GET"
        
        // Verify response parsing
        result.isRight() shouldBe true
        val events = result.getOrNull()!!
        events.size shouldBe 2
        
        events[0].title shouldBe "Team Meeting"
        events[0].description shouldBe "Weekly team sync"
        events[0].location shouldBe "Conference Room A"
        events[0].startTime shouldBe LocalDateTime.of(2023, 11, 15, 10, 0)
        events[0].endTime shouldBe LocalDateTime.of(2023, 11, 15, 11, 0)
        events[0].isAllDay shouldBe false
        
        events[1].title shouldBe "Lunch with Client"
        events[1].description shouldBe "Discuss project requirements"
    }
    
    "should handle all-day events" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        val mockResponse = """
            {
                "kind": "calendar#events",
                "items": [
                    {
                        "id": "event1",
                        "summary": "Company Holiday",
                        "description": "Office closed",
                        "start": {
                            "date": "2023-11-15"
                        },
                        "end": {
                            "date": "2023-11-16"
                        }
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
        val result = adapter.getEvents(testUser, date)
        
        // Then
        result.isRight() shouldBe true
        val events = result.getOrNull()!!
        events.size shouldBe 1
        events[0].title shouldBe "Company Holiday"
        events[0].isAllDay shouldBe true
        events[0].startTime shouldBe LocalDateTime.of(2023, 11, 15, 0, 0)
        events[0].endTime shouldBe LocalDateTime.of(2023, 11, 15, 23, 59, 59)
    }
    
    "should handle empty event list" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        val mockResponse = """
            {
                "kind": "calendar#events",
                "summary": "user@example.com",
                "items": []
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getEvents(testUser, date)
        
        // Then
        result.isRight() shouldBe true
        val events = result.getOrNull()!!
        events.size shouldBe 0
    }
    
    "should handle authentication error" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": {"message": "Invalid Credentials"}}""")
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getEvents(testUser, date)
        
        // Then
        result shouldBe ProviderError.AuthenticationError("Google Calendar").left()
    }
    
    "should handle rate limit error" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("""{"error": {"message": "Rate Limit Exceeded"}}""")
                .addHeader("Content-Type", "application/json")
                .addHeader("Retry-After", "60")
        )
        
        // When
        val result = adapter.getEvents(testUser, date)
        
        // Then
        result shouldBe ProviderError.RateLimitExceeded(60).left()
    }
    
    "should handle network timeout" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .setBodyDelay(10, TimeUnit.SECONDS) // Longer than timeout
        )
        
        // When
        val result = adapter.getEvents(testUser, date)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.NetworkError -> true
            else -> false
        } shouldBe true
    }
    
    "should handle server errors" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("""{"error": "Service temporarily unavailable"}""")
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getEvents(testUser, date)
        
        // Then
        result shouldBe ProviderError.ServiceUnavailable("Google Calendar").left()
    }
    
    "should skip events with missing required fields" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        val mockResponse = """
            {
                "kind": "calendar#events",
                "items": [
                    {
                        "id": "event1",
                        "summary": "Valid Event",
                        "start": {
                            "dateTime": "2023-11-15T10:00:00Z"
                        },
                        "end": {
                            "dateTime": "2023-11-15T11:00:00Z"
                        }
                    },
                    {
                        "id": "event2",
                        "description": "Missing summary",
                        "start": {
                            "dateTime": "2023-11-15T12:00:00Z"
                        },
                        "end": {
                            "dateTime": "2023-11-15T13:00:00Z"
                        }
                    },
                    {
                        "id": "event3",
                        "summary": "Missing start time",
                        "end": {
                            "dateTime": "2023-11-15T14:00:00Z"
                        }
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
        val result = adapter.getEvents(testUser, date)
        
        // Then
        result.isRight() shouldBe true
        val events = result.getOrNull()!!
        events.size shouldBe 1
        events[0].title shouldBe "Valid Event"
    }
    
    "should handle malformed JSON response" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        val malformedJson = """{ invalid json """
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(malformedJson)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getEvents(testUser, date)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.InvalidResponse -> {
                error.details shouldContain "Failed to parse calendar events"
            }
            else -> error("Expected InvalidResponse error")
        }
    }
})