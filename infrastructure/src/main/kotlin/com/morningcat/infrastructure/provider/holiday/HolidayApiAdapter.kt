package com.morningcat.infrastructure.provider.holiday

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.schedule.ports.HolidayProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class HolidayApiAdapter(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "https://holidayapi.com/v1",
) : HolidayProvider {
    companion object {
        private const val HOLIDAYS_ENDPOINT = "/holidays"
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }

    override suspend fun getHolidays(
        year: Int,
        countryCode: String,
    ): Either<ProviderError, List<LocalDate>> =
        try {
            val response =
                httpClient.get("$baseUrl$HOLIDAYS_ENDPOINT") {
                    parameter("year", year)
                    parameter("country", countryCode)
                    parameter("api_key", apiKey)
                    parameter("public", true) // Only fetch public holidays
                }

            when (response.status) {
                HttpStatusCode.OK -> parseHolidayResponse(response)
                HttpStatusCode.PaymentRequired -> {
                    try {
                        val errorResponse = response.body<ErrorResponse>()
                        ProviderError.AuthenticationError(errorResponse.error).left()
                    } catch (e: Exception) {
                        ProviderError.AuthenticationError("Invalid API key or subscription expired").left()
                    }
                }
                HttpStatusCode.TooManyRequests -> {
                    val retryAfter = response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()
                    ProviderError.RateLimitExceeded(retryAfter).left()
                }
                HttpStatusCode.InternalServerError,
                HttpStatusCode.BadGateway,
                HttpStatusCode.ServiceUnavailable,
                HttpStatusCode.GatewayTimeout,
                -> {
                    ProviderError.ServiceUnavailable("Holiday API").left()
                }
                else -> {
                    ProviderError.ServiceUnavailable("Holiday API").left()
                }
            }
        } catch (e: Exception) {
            ProviderError.NetworkError(e.message ?: "Unknown error").left()
        }

    private suspend fun parseHolidayResponse(response: HttpResponse): Either<ProviderError, List<LocalDate>> =
        try {
            val holidayResponse = response.body<HolidayResponse>()

            val holidays =
                holidayResponse.holidays.mapNotNull { holiday ->
                    // Only include public holidays with valid dates
                    if (holiday.public == true) {
                        try {
                            LocalDate.parse(holiday.date, DATE_FORMATTER)
                        } catch (e: DateTimeParseException) {
                            // Skip holidays with invalid date format
                            null
                        }
                    } else {
                        null
                    }
                }

            holidays.sorted().right()
        } catch (e: Exception) {
            ProviderError.InvalidResponse("Failed to parse holiday data: ${e.message}").left()
        }

    @Serializable
    private data class HolidayResponse(
        val status: Int,
        val holidays: List<Holiday> = emptyList(),
    )

    @Serializable
    private data class Holiday(
        val name: String,
        val date: String,
        val observed: String? = null,
        val public: Boolean? = null,
    )

    @Serializable
    private data class ErrorResponse(
        val status: Int,
        val error: String,
    )
}
