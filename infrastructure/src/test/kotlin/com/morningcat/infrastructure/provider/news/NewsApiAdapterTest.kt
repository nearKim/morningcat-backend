package com.morningcat.infrastructure.provider.news

import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.NewsArticle
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class NewsApiAdapterTest : StringSpec({
    
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
    
    "should successfully fetch and parse news articles" {
        // Given
        val apiKey = "test-api-key"
        val countryCode = "us"
        
        val client = createMockClient { request ->
            when {
                request.url.toString().contains("top-headlines") -> {
                    respond(
                        content = """
                            {
                                "status": "ok",
                                "totalResults": 1,
                                "articles": [
                                    {
                                        "title": "Test Article",
                                        "description": "Test description",
                                        "url": "https://example.com/article"
                                    }
                                ]
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> error("Unhandled ${request.url}")
            }
        }
        
        val adapter = NewsApiAdapter(client, apiKey)
        
        // When
        val result = adapter.fetchNews(countryCode)
        
        // Then
        result shouldBe listOf(
            NewsArticle(
                headline = "Test Article",
                summary = "Test description",
                url = "https://example.com/article"
            )
        ).right()
    }
    
    "should handle network error" {
        // Given
        val apiKey = "test-api-key"
        val countryCode = "us"
        
        val client = createMockClient { _ ->
            throw Exception("Network error")
        }
        
        val adapter = NewsApiAdapter(client, apiKey)
        
        // When
        val result = adapter.fetchNews(countryCode)
        
        // Then
        result shouldBe ProviderError.NetworkError("Network error").left()
    }
    
    "should filter out articles with null fields" {
        // Given
        val apiKey = "test-api-key"
        val countryCode = "gb"
        
        val client = createMockClient { request ->
            respond(
                content = """
                    {
                        "status": "ok",
                        "totalResults": 3,
                        "articles": [
                            {
                                "title": "Valid Article",
                                "description": "Valid description",
                                "url": "https://example.com/valid"
                            },
                            {
                                "title": null,
                                "description": "Missing title",
                                "url": "https://example.com/no-title"
                            },
                            {
                                "title": "Missing URL",
                                "description": "Valid description",
                                "url": null
                            }
                        ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        val adapter = NewsApiAdapter(client, apiKey)
        
        // When
        val result = adapter.fetchNews(countryCode)
        
        // Then
        result shouldBe listOf(
            NewsArticle(
                headline = "Valid Article",
                summary = "Valid description",
                url = "https://example.com/valid"
            )
        ).right()
    }
    
    "should handle error status in response" {
        // Given
        val apiKey = "test-api-key"
        val countryCode = "us"
        
        val client = createMockClient { request ->
            respond(
                content = """
                    {
                        "status": "error",
                        "code": "parametersMissing",
                        "message": "Required parameters are missing"
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        val adapter = NewsApiAdapter(client, apiKey)
        
        // When
        val result = adapter.fetchNews(countryCode)
        
        // Then
        result shouldBe ProviderError.InvalidResponse("API returned status: error").left()
    }
})