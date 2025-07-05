package com.morningcat.domain.content.ports

import arrow.core.Either
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.CalendarEvent
import com.morningcat.domain.user.aggregate.User
import java.time.LocalDate

interface CalendarProvider {
    suspend fun getEvents(
        user: User,
        date: LocalDate,
    ): Either<ProviderError, List<CalendarEvent>>
}
