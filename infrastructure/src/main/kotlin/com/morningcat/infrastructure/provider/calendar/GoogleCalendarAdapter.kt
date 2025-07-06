package com.morningcat.infrastructure.provider.calendar

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.CalendarEvent
import com.morningcat.domain.content.ports.CalendarProvider
import com.morningcat.domain.user.aggregate.User
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class GoogleCalendarAdapter(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "https://www.googleapis.com/calendar/v3",
) : CalendarProvider {
    
    private sealed class ParseError {
        object MissingTitle : ParseError()
        object MissingDates : ParseError()
        data class InvalidDateTime(val error: String) : ParseError()
    }
    override suspend fun getEvents(
        user: User,
        date: LocalDate,
    ): Either<ProviderError, List<CalendarEvent>> =
        either {
            val timeMin = date.atStartOfDay(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT)
            val timeMax = date.atTime(23, 59, 59).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT)

            val response =
                try {
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
                    val eventsResponse =
                        try {
                            response.body<EventsResponse>()
                        } catch (e: Exception) {
                            raise(ProviderError.InvalidResponse("Failed to parse calendar events: ${e.message}"))
                        }

                    eventsResponse.items.mapNotNull { item ->
                        parseEvent(item).getOrElse { null }
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
                HttpStatusCode.GatewayTimeout,
                -> {
                    raise(ProviderError.ServiceUnavailable("Google Calendar"))
                }

                else -> {
                    raise(ProviderError.ServiceUnavailable("Google Calendar"))
                }
            }
        }

    private fun parseEvent(item: EventItem): Either<ParseError, CalendarEvent> = either {
        // Validate and extract title
        val title = item.summary.toOption()
            .getOrElse { raise(ParseError.MissingTitle) }
        
        // Extract optional fields
        val description = item.description ?: ""
        val location = item.location
        
        // Parse event times
        val eventTiming = extractEventTimes(item).bind()
        
        CalendarEvent(
            title = title,
            description = description,
            startTime = eventTiming.startTime,
            endTime = eventTiming.endTime,
            location = location,
            isAllDay = eventTiming.isAllDay,
        )
    }
    
    private fun extractEventTimes(item: EventItem): Either<ParseError, EventTiming> = either {
        when {
            hasTimedDates(item) -> parseTimedEvent(item).bind()
            hasAllDayDates(item) -> parseAllDayEvent(item).bind()
            else -> raise(ParseError.MissingDates)
        }
    }
    
    private fun hasTimedDates(item: EventItem): Boolean =
        item.start?.dateTime != null && item.end?.dateTime != null
    
    private fun hasAllDayDates(item: EventItem): Boolean =
        item.start?.date != null && item.end?.date != null
    
    private fun parseTimedEvent(item: EventItem): Either<ParseError, EventTiming> = either {
        val startDateTime = item.start?.dateTime ?: raise(ParseError.MissingDates)
        val endDateTime = item.end?.dateTime ?: raise(ParseError.MissingDates)
        
        val start = parseDateTime(startDateTime).bind()
        val end = parseDateTime(endDateTime).bind()
        
        EventTiming(start, end, isAllDay = false)
    }
    
    private fun parseAllDayEvent(item: EventItem): Either<ParseError, EventTiming> = either {
        val startDateStr = item.start?.date ?: raise(ParseError.MissingDates)
        val endDateStr = item.end?.date ?: raise(ParseError.MissingDates)
        
        val startDate = try {
            LocalDate.parse(startDateStr)
        } catch (e: Exception) {
            raise(ParseError.InvalidDateTime("Invalid start date: ${e.message}"))
        }
        
        // For all-day events, use the full day
        val start = startDate.atStartOfDay()
        val end = startDate.atTime(23, 59, 59)
        
        EventTiming(start, end, isAllDay = true)
    }
    
    private fun parseDateTime(dateTimeStr: String): Either<ParseError, LocalDateTime> = either {
        try {
            ZonedDateTime.parse(dateTimeStr).toLocalDateTime()
        } catch (e: Exception) {
            raise(ParseError.InvalidDateTime("Invalid datetime: ${e.message}"))
        }
    }
    
    private data class EventTiming(
        val startTime: LocalDateTime,
        val endTime: LocalDateTime,
        val isAllDay: Boolean
    )

    @Serializable
    private data class EventsResponse(
        val kind: String? = null,
        val summary: String? = null,
        val items: List<EventItem> = emptyList(),
    )

    @Serializable
    private data class EventItem(
        val id: String? = null,
        val summary: String? = null,
        val description: String? = null,
        val location: String? = null,
        val start: EventDateTime? = null,
        val end: EventDateTime? = null,
    )

    @Serializable
    private data class EventDateTime(
        val date: String? = null,
        val dateTime: String? = null,
        val timeZone: String? = null,
    )
}
