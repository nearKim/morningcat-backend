package com.morningcat.domain.schedule.service

import com.morningcat.domain.schedule.valueobject.DayType
import com.morningcat.domain.user.valueobject.Location
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate

class ContentDecisionServiceTest :
    DescribeSpec({
        describe("ContentDecisionService") {
            val mockHolidayService = mockk<HolidayService>()
            val contentDecisionService = ContentDecisionService(mockHolidayService)

            describe("determineDayType") {
                it("should return WeekendOrHoliday when HolidayService returns true") {
                    // Given
                    val date = LocalDate.of(2024, 1, 1)
                    val location = Location("Seoul", "KR")
                    coEvery { mockHolidayService.isWeekendOrHoliday(date, location) } returns true

                    // When
                    val result = contentDecisionService.determineDayType(date, location)

                    // Then
                    result shouldBe DayType.WeekendOrHoliday
                }

                it("should return Weekday when HolidayService returns false") {
                    // Given
                    val date = LocalDate.of(2024, 1, 2)
                    val location = Location("Seoul", "KR")
                    coEvery { mockHolidayService.isWeekendOrHoliday(date, location) } returns false

                    // When
                    val result = contentDecisionService.determineDayType(date, location)

                    // Then
                    result shouldBe DayType.Weekday
                }
            }
        }
    })
