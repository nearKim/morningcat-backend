package com.morningcat.infrastructure.provider.calendar

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.CalendarEvent
import com.morningcat.domain.content.ports.CalendarProvider
import com.morningcat.domain.user.aggregate.User
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class GoogleCalendarAdapter(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "https://www.googleapis.com/calendar/v3"
) : CalendarProvider {
    
    override suspend fun getEvents(
        user: User,
        date: LocalDate
    ): Either<ProviderError, List<CalendarEvent>> = either {
        val timeMin = date.atStartOfDay(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT)
        val timeMax = date.atTime(23, 59, 59).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT)
        
        val response = try {
            httpClient.get("$baseUrl/calendars/primary/events") {
                parameter("key", apiKey)
                parameter("timeMin", timeMin)
                parameter("timeMax", timeMax)
                parameter("singleEvents", true)
                parameter("orderBy", "startTime")
            }
        } catch (e: Exception) {
            raise(ProviderError.NetworkError(e.message ?: "Unknown error"))
        }
        
        when (response.status) {
            HttpStatusCode.OK -> {
                val eventsResponse = try {
                    response.body<EventsResponse>()
                } catch (e: Exception) {
                    raise(ProviderError.InvalidResponse("Failed to parse calendar events: ${e.message}"))
                }
                
                eventsResponse.items.mapNotNull { item ->
                    parseEvent(item)
                }
            }
            HttpStatusCode.Unauthorized -> {
                raise(ProviderError.AuthenticationError("Google Calendar"))
            }
            HttpStatusCode.TooManyRequests -> {
                val retryAfter = response.headers["Retry-After"]?.toLongOrNull()
                raise(ProviderError.RateLimitExceeded(retryAfter))
            }
            HttpStatusCode.InternalServerError,
            HttpStatusCode.BadGateway,
            HttpStatusCode.ServiceUnavailable,
            HttpStatusCode.GatewayTimeout -> {
                raise(ProviderError.ServiceUnavailable("Google Calendar"))
            }
            else -> {
                raise(ProviderError.ServiceUnavailable("Google Calendar"))
            }
        }
    }
    
    private fun parseEvent(item: EventItem): CalendarEvent? {
        return try {
            val title = item.summary ?: return null // Skip events without title
            val description = item.description ?: ""
            val location = item.location
            
            val (startTime, endTime, isAllDay) = when {
                item.start?.dateTime != null && item.end?.dateTime != null -> {
                    val start = parseDateTime(item.start.dateTime)
                    val end = parseDateTime(item.end.dateTime)
                    Triple(start, end, false)
                }
                item.start?.date != null && item.end?.date != null -> {
                    // All-day event
                    val startDate = LocalDate.parse(item.start.date)
                    val start = startDate.atStartOfDay()
                    val end = startDate.atTime(23, 59, 59)
                    Triple(start, end, true)
                }
                else -> return null // Skip events without proper dates
            }
            
            CalendarEvent(
                title = title,
                description = description,
                startTime = startTime,
                endTime = endTime,
                location = location,
                isAllDay = isAllDay
            )
        } catch (e: Exception) {
            null // Skip invalid events
        }
    }
    
    private fun parseDateTime(dateTimeStr: String): LocalDateTime {
        return ZonedDateTime.parse(dateTimeStr).toLocalDateTime()
    }
    
    @Serializable
    private data class EventsResponse(
        val kind: String? = null,
        val summary: String? = null,
        val items: List<EventItem> = emptyList()
    )
    
    @Serializable
    private data class EventItem(
        val id: String? = null,
        val summary: String? = null,
        val description: String? = null,
        val location: String? = null,
        val start: EventDateTime? = null,
        val end: EventDateTime? = null
    )
    
    @Serializable
    private data class EventDateTime(
        val date: String? = null,
        val dateTime: String? = null,
        val timeZone: String? = null
    )
}