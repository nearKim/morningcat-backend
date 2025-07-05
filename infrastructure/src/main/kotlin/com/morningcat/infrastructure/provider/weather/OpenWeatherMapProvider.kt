package com.morningcat.infrastructure.provider.weather

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.WeatherInfo
import com.morningcat.domain.content.ports.WeatherProvider
import com.morningcat.domain.user.valueobject.Location
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.math.roundToInt

class OpenWeatherMapProvider(
    private val httpClient: HttpClient,
    private val apiKey: String
) : WeatherProvider {
    
    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5"
    }
    
    override suspend fun fetchWeather(location: Location): Either<ProviderError, WeatherInfo> {
        return try {
            val response = httpClient.get("$BASE_URL/weather") {
                parameter("q", "${location.city},${location.countryCode}")
                parameter("appid", apiKey)
                parameter("units", "metric")
            }
            
            when (response.status) {
                HttpStatusCode.OK -> parseWeatherResponse(response)
                HttpStatusCode.Unauthorized -> {
                    val errorResponse = response.body<ErrorResponse>()
                    ProviderError.AuthenticationError(errorResponse.message).left()
                }
                HttpStatusCode.TooManyRequests -> {
                    val retryAfter = response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()
                    ProviderError.RateLimitExceeded(retryAfter).left()
                }
                else -> {
                    ProviderError.ServiceUnavailable("OpenWeatherMap").left()
                }
            }
        } catch (e: Exception) {
            ProviderError.NetworkError(e.message ?: "Unknown error").left()
        }
    }
    
    private suspend fun parseWeatherResponse(response: HttpResponse): Either<ProviderError, WeatherInfo> {
        return try {
            val weatherResponse = response.body<WeatherResponse>()
            
            // Fetch UV index separately (requires a different endpoint in OpenWeatherMap)
            // For simplicity, we'll use a default value in this example
            val uvIndex = 5 // Would normally fetch from UV endpoint
            
            WeatherInfo(
                date = LocalDate.now(),
                temperature = WeatherInfo.Temperature(
                    min = weatherResponse.main.tempMin,
                    max = weatherResponse.main.tempMax,
                    current = weatherResponse.main.temp
                ),
                condition = weatherResponse.weather.firstOrNull()?.main ?: "Unknown",
                humidity = weatherResponse.main.humidity,
                uvIndex = uvIndex,
                precipitation = (weatherResponse.rain?.oneHour ?: 0.0).roundToInt()
            ).right()
        } catch (e: Exception) {
            ProviderError.InvalidResponse("Failed to parse weather data: ${e.message}").left()
        }
    }
    
    @Serializable
    private data class WeatherResponse(
        val weather: List<WeatherCondition>,
        val main: MainWeatherData,
        val rain: RainData? = null
    )
    
    @Serializable
    private data class WeatherCondition(
        val main: String,
        val description: String
    )
    
    @Serializable
    private data class MainWeatherData(
        val temp: Double,
        @SerialName("temp_min")
        val tempMin: Double,
        @SerialName("temp_max")
        val tempMax: Double,
        val humidity: Int
    )
    
    @Serializable
    private data class RainData(
        @SerialName("1h")
        val oneHour: Double? = null
    )
    
    @Serializable
    private data class ErrorResponse(
        val message: String
    )
}