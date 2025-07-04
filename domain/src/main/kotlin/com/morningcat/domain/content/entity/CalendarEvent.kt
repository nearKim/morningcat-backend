package com.morningcat.domain.content.entity

import java.time.LocalDateTime

data class CalendarEvent(
    val title: String,
    val description: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val location: String?,
    val isAllDay: Boolean = false,
) {
    init {
        require(title.isNotBlank()) { "Event title cannot be blank" }
        require(endTime >= startTime) { "End time must be after or equal to start time" }
    }

    val duration: java.time.Duration
        get() = java.time.Duration.between(startTime, endTime)
}
