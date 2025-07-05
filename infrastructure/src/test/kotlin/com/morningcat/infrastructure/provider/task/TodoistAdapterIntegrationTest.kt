package com.morningcat.infrastructure.provider.task

import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.Task
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

class TodoistAdapterIntegrationTest : StringSpec({
    
    lateinit var mockWebServer: MockWebServer
    lateinit var httpClient: HttpClient
    lateinit var adapter: TodoistAdapter
    
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
        adapter = TodoistAdapter(httpClient, "test-api-token", baseUrl)
    }
    
    afterEach {
        try {
            httpClient.close()
            mockWebServer.shutdown()
        } catch (e: Exception) {
            // Ignore shutdown errors for timeout tests
        }
    }
    
    "should fetch tasks successfully" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        val mockResponse = """
            [
                {
                    "id": "task1",
                    "content": "Complete project report",
                    "description": "Finalize Q4 report for management",
                    "due": {
                        "date": "2023-11-15",
                        "datetime": "2023-11-15T17:00:00Z",
                        "timezone": "UTC"
                    },
                    "priority": 4,
                    "is_completed": false
                },
                {
                    "id": "task2",
                    "content": "Review code changes",
                    "description": "",
                    "due": {
                        "date": "2023-11-15",
                        "datetime": "2023-11-15T14:30:00Z",
                        "timezone": "UTC"
                    },
                    "priority": 3,
                    "is_completed": false
                },
                {
                    "id": "task3",
                    "content": "Team standup",
                    "description": null,
                    "due": {
                        "date": "2023-11-15"
                    },
                    "priority": 2,
                    "is_completed": true
                }
            ]
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getTasks(testUser, date)
        
        // Then - verify request
        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)!!
        request.path shouldContain "tasks"
        request.path shouldContain "filter=" // Should have date filter
        request.headers["Authorization"] shouldBe "Bearer test-api-token"
        request.method shouldBe "GET"
        
        // Verify response parsing
        result.isRight() shouldBe true
        val tasks = result.getOrNull()!!
        tasks.size shouldBe 3
        
        tasks[0].title shouldBe "Complete project report"
        tasks[0].description shouldBe "Finalize Q4 report for management"
        tasks[0].dueDate shouldBe LocalDateTime.of(2023, 11, 15, 17, 0)
        tasks[0].priority shouldBe Task.Priority.HIGH
        tasks[0].isCompleted shouldBe false
        
        tasks[1].title shouldBe "Review code changes"
        tasks[1].description shouldBe null  // Empty string in JSON becomes null
        tasks[1].priority shouldBe Task.Priority.MEDIUM
        
        tasks[2].title shouldBe "Team standup"
        tasks[2].description shouldBe null
        tasks[2].priority shouldBe Task.Priority.MEDIUM
        tasks[2].isCompleted shouldBe true
    }
    
    "should handle tasks without due dates" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        val mockResponse = """
            [
                {
                    "id": "task1",
                    "content": "Ongoing research",
                    "description": "Research new technologies",
                    "priority": 2,
                    "is_completed": false
                }
            ]
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getTasks(testUser, date)
        
        // Then
        result.isRight() shouldBe true
        val tasks = result.getOrNull()!!
        tasks.size shouldBe 1
        tasks[0].title shouldBe "Ongoing research"
        tasks[0].dueDate shouldBe null
    }
    
    "should handle empty task list" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        val mockResponse = "[]"
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getTasks(testUser, date)
        
        // Then
        result.isRight() shouldBe true
        val tasks = result.getOrNull()!!
        tasks.size shouldBe 0
    }
    
    "should handle authentication error" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "Invalid token"}""")
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getTasks(testUser, date)
        
        // Then
        result shouldBe ProviderError.AuthenticationError("Todoist").left()
    }
    
    "should handle rate limit error" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("""{"error": "Too many requests"}""")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-RateLimit-Reset", "1700000000")
        )
        
        // When
        val result = adapter.getTasks(testUser, date)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.RateLimitExceeded -> {
                error.retryAfter shouldBe 1700000000L
            }
            else -> error("Expected RateLimitExceeded error")
        }
    }
    
    "should handle network timeout" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]")
                .setBodyDelay(10, TimeUnit.SECONDS) // Longer than timeout
        )
        
        // When
        val result = adapter.getTasks(testUser, date)
        
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
        val result = adapter.getTasks(testUser, date)
        
        // Then
        result shouldBe ProviderError.ServiceUnavailable("Todoist").left()
    }
    
    "should skip tasks with invalid data" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        val mockResponse = """
            [
                {
                    "id": "task1",
                    "content": "Valid task",
                    "priority": 3,
                    "is_completed": false
                },
                {
                    "id": "task2",
                    "content": "",
                    "priority": 2,
                    "is_completed": false
                },
                {
                    "id": "task3",
                    "priority": 1,
                    "is_completed": false
                }
            ]
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getTasks(testUser, date)
        
        // Then
        result.isRight() shouldBe true
        val tasks = result.getOrNull()!!
        tasks.size shouldBe 1
        tasks[0].title shouldBe "Valid task"
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
        val result = adapter.getTasks(testUser, date)
        
        // Then
        result.isLeft() shouldBe true
        when (val error = result.leftOrNull()) {
            is ProviderError.InvalidResponse -> {
                error.details shouldContain "Failed to parse tasks"
            }
            else -> error("Expected InvalidResponse error")
        }
    }
    
    "should map Todoist priority to domain priority correctly" {
        // Given
        val date = LocalDate.of(2023, 11, 15)
        val mockResponse = """
            [
                {
                    "id": "task1",
                    "content": "Priority 4 - High",
                    "priority": 4,
                    "is_completed": false
                },
                {
                    "id": "task2", 
                    "content": "Priority 3 - Medium",
                    "priority": 3,
                    "is_completed": false
                },
                {
                    "id": "task3",
                    "content": "Priority 2 - Medium",
                    "priority": 2,
                    "is_completed": false
                },
                {
                    "id": "task4",
                    "content": "Priority 1 - Low",
                    "priority": 1,
                    "is_completed": false
                }
            ]
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )
        
        // When
        val result = adapter.getTasks(testUser, date)
        
        // Then
        result.isRight() shouldBe true
        val tasks = result.getOrNull()!!
        tasks.size shouldBe 4
        
        // Todoist priority 4 = HIGH
        tasks[0].priority shouldBe Task.Priority.HIGH
        
        // Todoist priority 3 & 2 = MEDIUM  
        tasks[1].priority shouldBe Task.Priority.MEDIUM
        tasks[2].priority shouldBe Task.Priority.MEDIUM
        
        // Todoist priority 1 = LOW
        tasks[3].priority shouldBe Task.Priority.LOW
    }
})