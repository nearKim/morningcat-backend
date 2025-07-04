package com.morningcat.domain.schedule.valueobject

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DayTypeTest :
    StringSpec({

        "should have Weekday and WeekendOrHoliday as objects" {
            DayType.Weekday.shouldBeInstanceOf<DayType>()
            DayType.WeekendOrHoliday.shouldBeInstanceOf<DayType>()
        }

        "should correctly identify workdays" {
            DayType.Weekday.isWorkday() shouldBe true
            DayType.WeekendOrHoliday.isWorkday() shouldBe false
        }

        "should correctly identify rest days" {
            DayType.Weekday.isRestDay() shouldBe false
            DayType.WeekendOrHoliday.isRestDay() shouldBe true
        }

        "should support exhaustive when expression" {
            fun getDayTypeMessage(dayType: DayType): String =
                when (dayType) {
                    is DayType.Weekday -> "Time to be productive!"
                    is DayType.WeekendOrHoliday -> "Enjoy your day off!"
                }

            getDayTypeMessage(DayType.Weekday) shouldBe "Time to be productive!"
            getDayTypeMessage(DayType.WeekendOrHoliday) shouldBe "Enjoy your day off!"
        }

        "should have object equality" {
            val weekday1 = DayType.Weekday
            val weekday2 = DayType.Weekday
            val weekend = DayType.WeekendOrHoliday

            (weekday1 === weekday2) shouldBe true // Same instance
            (weekday1 == weekend) shouldBe false
        }

        "should be usable in conditional logic" {
            val dayType: DayType = DayType.Weekday

            val contentType =
                if (dayType.isWorkday()) {
                    "Professional content"
                } else {
                    "Leisure content"
                }

            contentType shouldBe "Professional content"
        }
    })
