package com.morningcat.infrastructure.provider.news

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.NewsArticle
import com.morningcat.domain.content.ports.NewsProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class NewsApiAdapter(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "https://newsapi.org"
) : NewsProvider {
    
    companion object {
        private const val API_VERSION = "/v2"
        private const val TOP_HEADLINES_ENDPOINT = "/top-headlines"
    }
    
    override suspend fun fetchNews(countryCode: String): Either<ProviderError, List<NewsArticle>> {
        return try {
            val response = httpClient.get("$baseUrl$API_VERSION$TOP_HEADLINES_ENDPOINT") {
                parameter("country", countryCode)
                parameter("apiKey", apiKey)
            }
            
            when (response.status) {
                HttpStatusCode.OK -> parseNewsResponse(response)
                HttpStatusCode.Unauthorized -> {
                    try {
                        val errorResponse = response.body<ErrorResponse>()
                        ProviderError.AuthenticationError(errorResponse.message).left()
                    } catch (e: Exception) {
                        ProviderError.AuthenticationError("Invalid API key").left()
                    }
                }
                HttpStatusCode.TooManyRequests -> {
                    val resetTime = response.headers["X-Rate-Limit-Reset"]?.toLongOrNull()
                    ProviderError.RateLimitExceeded(resetTime).left()
                }
                HttpStatusCode.InternalServerError,
                HttpStatusCode.BadGateway,
                HttpStatusCode.ServiceUnavailable,
                HttpStatusCode.GatewayTimeout -> {
                    ProviderError.ServiceUnavailable("NewsAPI").left()
                }
                else -> {
                    ProviderError.ServiceUnavailable("NewsAPI").left()
                }
            }
        } catch (e: Exception) {
            ProviderError.NetworkError(e.message ?: "Unknown error").left()
        }
    }
    
    private suspend fun parseNewsResponse(response: HttpResponse): Either<ProviderError, List<NewsArticle>> {
        return try {
            val newsResponse = response.body<NewsResponse>()
            
            if (newsResponse.status != "ok") {
                return ProviderError.InvalidResponse("API returned status: ${newsResponse.status}").left()
            }
            
            val articles = newsResponse.articles.mapNotNull { article ->
                // Filter out articles with missing or empty required fields
                if (article.title.isNullOrBlank() || 
                    article.description.isNullOrBlank() || 
                    article.url.isNullOrBlank()) {
                    null
                } else {
                    try {
                        NewsArticle(
                            headline = article.title,
                            summary = article.description,
                            url = article.url
                        )
                    } catch (e: IllegalArgumentException) {
                        // Skip invalid articles
                        null
                    }
                }
            }
            
            articles.right()
        } catch (e: Exception) {
            ProviderError.InvalidResponse("Failed to parse news data: ${e.message}").left()
        }
    }
    
    @Serializable
    private data class NewsResponse(
        val status: String,
        val totalResults: Int? = null,
        val articles: List<Article> = emptyList(),
        val code: String? = null,
        val message: String? = null
    )
    
    @Serializable
    private data class Article(
        val source: Source? = null,
        val author: String? = null,
        val title: String? = null,
        val description: String? = null,
        val url: String? = null,
        val urlToImage: String? = null,
        val publishedAt: String? = null,
        val content: String? = null
    )
    
    @Serializable
    private data class Source(
        val id: String? = null,
        val name: String? = null
    )
    
    @Serializable
    private data class ErrorResponse(
        val status: String,
        val code: String,
        val message: String
    )
}