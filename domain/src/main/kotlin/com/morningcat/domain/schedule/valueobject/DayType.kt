package com.morningcat.domain.schedule.valueobject

sealed class DayType {
    object Weekday : DayType()

    object WeekendOrHoliday : DayType()

    fun isWorkday(): Boolean = this is Weekday

    fun isRestDay(): Boolean = this is WeekendOrHoliday
}
