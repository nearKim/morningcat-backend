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
import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.math.roundToInt

class OpenWeatherMapAdapter(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "https://api.openweathermap.org",
) : WeatherProvider {
    companion object {
        private const val ONE_CALL_API_PATH = "/data/3.0/onecall"
        private const val GEOCODING_API_PATH = "/geo/1.0/direct"
    }

    override suspend fun fetchWeather(location: Location): Either<ProviderError, WeatherInfo> {
        return try {
            // First, get coordinates for the location
            val coordinates =
                getCoordinates(location).fold(
                    { return it.left() },
                    { it },
                )

            // Then fetch weather data using One Call API 3.0
            val response =
                httpClient.get("$baseUrl$ONE_CALL_API_PATH") {
                    parameter("lat", coordinates.lat)
                    parameter("lon", coordinates.lon)
                    parameter("appid", apiKey)
                    parameter("units", "metric")
                    parameter("exclude", "minutely,hourly,alerts") // We only need current and daily data
                }

            when (response.status) {
                HttpStatusCode.OK -> parseOneCallResponse(response)
                HttpStatusCode.Unauthorized -> {
                    try {
                        val errorResponse = response.body<ErrorResponse>()
                        ProviderError.AuthenticationError(errorResponse.message).left()
                    } catch (e: Exception) {
                        ProviderError.AuthenticationError("Invalid API key").left()
                    }
                }
                HttpStatusCode.TooManyRequests -> {
                    val retryAfter = response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()
                    ProviderError.RateLimitExceeded(retryAfter).left()
                }
                HttpStatusCode.ServiceUnavailable,
                HttpStatusCode.InternalServerError,
                HttpStatusCode.BadGateway,
                HttpStatusCode.GatewayTimeout,
                -> {
                    ProviderError.ServiceUnavailable("OpenWeatherMap").left()
                }
                else -> {
                    ProviderError.ServiceUnavailable("OpenWeatherMap").left()
                }
            }
        } catch (e: Exception) {
            ProviderError.NetworkError(e.message ?: "Unknown error").left()
        }
    }

    private suspend fun getCoordinates(location: Location): Either<ProviderError, Coordinates> =
        try {
            val response =
                httpClient.get("$baseUrl$GEOCODING_API_PATH") {
                    parameter("q", "${location.city},${location.countryCode}")
                    parameter("limit", 1)
                    parameter("appid", apiKey)
                }

            if (response.status == HttpStatusCode.OK) {
                val locations = response.body<List<GeocodingResponse>>()
                if (locations.isNotEmpty()) {
                    Coordinates(locations[0].lat, locations[0].lon).right()
                } else {
                    ProviderError.InvalidResponse("Location not found").left()
                }
            } else {
                ProviderError.ServiceUnavailable("Geocoding service").left()
            }
        } catch (e: Exception) {
            ProviderError.NetworkError("Failed to get coordinates: ${e.message}").left()
        }

    private suspend fun parseOneCallResponse(response: HttpResponse): Either<ProviderError, WeatherInfo> {
        return try {
            val oneCallResponse = response.body<OneCallResponse>()

            // Get today's weather from daily forecast (first entry)
            val todayForecast =
                oneCallResponse.daily.firstOrNull()
                    ?: return ProviderError.InvalidResponse("No daily forecast data").left()

            WeatherInfo(
                date = LocalDate.now(),
                temperature =
                    WeatherInfo.Temperature(
                        min = todayForecast.temp.min,
                        max = todayForecast.temp.max,
                        current = oneCallResponse.current.temp,
                    ),
                condition =
                    oneCallResponse.current.weather
                        .firstOrNull()
                        ?.main ?: "Unknown",
                humidity = oneCallResponse.current.humidity,
                uvIndex = oneCallResponse.current.uvi.roundToInt(),
                precipitation = calculatePrecipitation(todayForecast),
            ).right()
        } catch (e: Exception) {
            ProviderError.InvalidResponse("Failed to parse weather data: ${e.message}").left()
        }
    }

    private fun calculatePrecipitation(daily: DailyWeather): Int {
        // One Call API provides precipitation in mm
        // We'll use rain if available, otherwise snow, otherwise 0
        val rain = daily.rain ?: 0.0
        val snow = daily.snow ?: 0.0
        return (rain + snow).roundToInt()
    }

    @Serializable
    private data class OneCallResponse(
        val current: CurrentWeather,
        val daily: List<DailyWeather>,
    )

    @Serializable
    private data class CurrentWeather(
        val dt: Long,
        val temp: Double,
        val humidity: Int,
        val uvi: Double,
        val weather: List<WeatherCondition>,
    )

    @Serializable
    private data class DailyWeather(
        val dt: Long,
        val temp: Temperature,
        val humidity: Int,
        val weather: List<WeatherCondition>,
        val rain: Double? = null,
        val snow: Double? = null,
    )

    @Serializable
    private data class Temperature(
        val min: Double,
        val max: Double,
    )

    @Serializable
    private data class WeatherCondition(
        val id: Int,
        val main: String,
        val description: String,
    )

    @Serializable
    private data class Coordinates(
        val lat: Double,
        val lon: Double,
    )

    @Serializable
    private data class GeocodingResponse(
        val name: String,
        val lat: Double,
        val lon: Double,
        val country: String,
    )

    @Serializable
    private data class ErrorResponse(
        val message: String,
        val cod: Int? = null,
    )
}
