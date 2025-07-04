package com.morningcat.domain.schedule.service

import com.morningcat.domain.user.valueobject.Location
import java.time.LocalDate

interface HolidayService {
    suspend fun isWeekendOrHoliday(
        date: LocalDate,
        location: Location,
    ): Boolean
}
