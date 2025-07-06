package com.morningcat.infrastructure.provider.news

import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.NewsArticle
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
import java.util.concurrent.TimeUnit

class NewsApiAdapterIntegrationTest :
    StringSpec({

        lateinit var mockWebServer: MockWebServer
        lateinit var httpClient: HttpClient
        lateinit var adapter: NewsApiAdapter

        beforeEach {
            mockWebServer = MockWebServer()
            mockWebServer.start()

            httpClient =
                HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                            },
                        )
                    }
                    engine {
                        endpoint {
                            connectTimeout = 5000
                            requestTimeout = 5000
                        }
                    }
                }

            val baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
            adapter = NewsApiAdapter(httpClient, "test-api-key", baseUrl)
        }

        afterEach {
            httpClient.close()
            mockWebServer.shutdown()
        }

        "should fetch top headlines successfully" {
            // Given
            val countryCode = "us"
            val mockResponse =
                """
                {
                    "status": "ok",
                    "totalResults": 2,
                    "articles": [
                        {
                            "source": {
                                "id": "bbc-news",
                                "name": "BBC News"
                            },
                            "author": "BBC News",
                            "title": "Breaking: Major Scientific Discovery Announced",
                            "description": "Scientists have made a groundbreaking discovery that could change our understanding of the universe.",
                            "url": "https://www.bbc.com/news/science-123456",
                            "urlToImage": "https://www.bbc.com/news/image.jpg",
                            "publishedAt": "2023-11-12T10:00:00Z",
                            "content": "Full article content here..."
                        },
                        {
                            "source": {
                                "id": "cnn",
                                "name": "CNN"
                            },
                            "author": "CNN Reporter",
                            "title": "Global Economic Summit Begins Today",
                            "description": "World leaders gather to discuss pressing economic challenges and opportunities for international cooperation.",
                            "url": "https://www.cnn.com/business/summit-123",
                            "urlToImage": "https://www.cnn.com/image.jpg",
                            "publishedAt": "2023-11-12T09:30:00Z",
                            "content": "Summit details..."
                        }
                    ]
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(mockResponse)
                    .addHeader("Content-Type", "application/json"),
            )

            // When
            val result = adapter.fetchNews(countryCode)

            // Then - verify request
            val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)!!
            request.path shouldContain "/v2/top-headlines"
            request.path shouldContain "country=us"
            request.path shouldContain "apiKey=test-api-key"
            request.method shouldBe "GET"

            // Verify response parsing
            result shouldBe
                listOf(
                    NewsArticle(
                        headline = "Breaking: Major Scientific Discovery Announced",
                        summary = "Scientists have made a groundbreaking discovery that could change our understanding of the universe.",
                        url = "https://www.bbc.com/news/science-123456",
                    ),
                    NewsArticle(
                        headline = "Global Economic Summit Begins Today",
                        summary = "World leaders gather to discuss pressing economic challenges and opportunities for international cooperation.",
                        url = "https://www.cnn.com/business/summit-123",
                    ),
                ).right()
        }

        "should handle empty results" {
            // Given
            val countryCode = "us"
            val emptyResponse =
                """
                {
                    "status": "ok",
                    "totalResults": 0,
                    "articles": []
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(emptyResponse)
                    .addHeader("Content-Type", "application/json"),
            )

            // When
            val result = adapter.fetchNews(countryCode)

            // Then
            result shouldBe emptyList<NewsArticle>().right()
        }

        "should filter out articles with missing required fields" {
            // Given
            val countryCode = "gb"
            val responseWithIncompleteArticles =
                """
                {
                    "status": "ok",
                    "totalResults": 4,
                    "articles": [
                        {
                            "title": "Valid Article",
                            "description": "This article has all required fields",
                            "url": "https://example.com/valid"
                        },
                        {
                            "title": "",
                            "description": "This article has empty title",
                            "url": "https://example.com/empty-title"
                        },
                        {
                            "title": "Missing Description",
                            "description": null,
                            "url": "https://example.com/no-desc"
                        },
                        {
                            "title": "Missing URL",
                            "description": "This article has no URL"
                        }
                    ]
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseWithIncompleteArticles)
                    .addHeader("Content-Type", "application/json"),
            )

            // When
            val result = adapter.fetchNews(countryCode)

            // Then - only valid article should be returned
            result shouldBe
                listOf(
                    NewsArticle(
                        headline = "Valid Article",
                        summary = "This article has all required fields",
                        url = "https://example.com/valid",
                    ),
                ).right()
        }

        "should handle API authentication error" {
            // Given
            val countryCode = "us"
            val errorResponse =
                """
                {
                    "status": "error",
                    "code": "apiKeyInvalid",
                    "message": "Your API key is invalid or incorrect. Check your key, or go to https://newsapi.org to create a free API key."
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody(errorResponse)
                    .addHeader("Content-Type", "application/json"),
            )

            // When
            val result = adapter.fetchNews(countryCode)

            // Then
            result shouldBe
                ProviderError
                    .AuthenticationError(
                        "Your API key is invalid or incorrect. Check your key, or go to https://newsapi.org to create a free API key.",
                    ).left()
        }

        "should handle rate limit exceeded" {
            // Given
            val countryCode = "us"
            val rateLimitResponse =
                """
                {
                    "status": "error",
                    "code": "rateLimited",
                    "message": "You have made too many requests recently. Developer accounts are limited to 100 requests over a 24 hour period."
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(429)
                    .setBody(rateLimitResponse)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Rate-Limit-Remaining", "0")
                    .addHeader("X-Rate-Limit-Reset", "1699790400"),
            )

            // When
            val result = adapter.fetchNews(countryCode)

            // Then
            result shouldBe ProviderError.RateLimitExceeded(1699790400).left()
        }

        "should handle network timeout" {
            // Given
            val countryCode = "us"

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("{}")
                    .setBodyDelay(10, TimeUnit.SECONDS), // Longer than timeout
            )

            // When
            val result = adapter.fetchNews(countryCode)

            // Then
            result.isLeft() shouldBe true
            when (val error = result.leftOrNull()) {
                is ProviderError.NetworkError -> true
                else -> false
            } shouldBe true
        }

        "should handle server errors" {
            // Given
            val countryCode = "us"

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("""{"message": "Internal server error"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            // When
            val result = adapter.fetchNews(countryCode)

            // Then
            result shouldBe ProviderError.ServiceUnavailable("NewsAPI").left()
        }

        "should handle malformed JSON response" {
            // Given
            val countryCode = "us"
            val malformedJson = """{ invalid json """

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(malformedJson)
                    .addHeader("Content-Type", "application/json"),
            )

            // When
            val result = adapter.fetchNews(countryCode)

            // Then
            result.isLeft() shouldBe true
            when (val error = result.leftOrNull()) {
                is ProviderError.InvalidResponse -> {
                    error.details shouldContain "Failed to parse news data"
                }
                else -> error("Expected InvalidResponse error")
            }
        }
    })
