package com.morningcat.domain.schedule.service

import com.morningcat.domain.schedule.valueobject.DayType
import com.morningcat.domain.user.valueobject.Location
import java.time.LocalDate

class ContentDecisionService(
    private val holidayService: HolidayService,
) {
    suspend fun determineDayType(
        date: LocalDate,
        location: Location,
    ): DayType =
        if (holidayService.isWeekendOrHoliday(date, location)) {
            DayType.WeekendOrHoliday
        } else {
            DayType.Weekday
        }
}
