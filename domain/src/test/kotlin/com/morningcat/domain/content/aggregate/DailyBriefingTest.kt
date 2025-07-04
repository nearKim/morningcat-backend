package com.morningcat.domain.content.aggregate

import com.morningcat.domain.content.entity.NewsArticle
import com.morningcat.domain.content.entity.WeatherInfo
import com.morningcat.domain.schedule.valueobject.DayType
import com.morningcat.domain.user.valueobject.Location
import com.morningcat.domain.user.valueobject.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import java.time.LocalDateTime

class DailyBriefingTest :
    StringSpec({

        val userId = UserId.generate()
        val location = Location("Seoul", "KR")
        val date = LocalDate.now()

        "should create daily briefing with content" {
            val news =
                listOf(
                    NewsArticle("Headline 1", "Summary 1", "https://example.com/1"),
                    NewsArticle("Headline 2", "Summary 2", "https://example.com/2"),
                )

            val briefing =
                DailyBriefing(
                    userId = userId,
                    date = date,
                    dayType = DayType.Weekday,
                    location = location,
                    news = news,
                )

            briefing.userId shouldBe userId
            briefing.date shouldBe date
            briefing.dayType shouldBe DayType.Weekday
            briefing.location shouldBe location
            briefing.news.size shouldBe 2
            briefing.hasContent() shouldBe true
            briefing.isEmpty() shouldBe false
            briefing.contentCount() shouldBe 2
        }

        "should create daily briefing with weather only" {
            val weather =
                WeatherInfo(
                    date = date,
                    temperature = WeatherInfo.Temperature(10.0, 25.0, 18.0),
                    condition = "Sunny",
                    humidity = 65,
                    uvIndex = 5,
                    precipitation = 0,
                )

            val briefing =
                DailyBriefing(
                    userId = userId,
                    date = date,
                    dayType = DayType.WeekendOrHoliday,
                    location = location,
                    weather = weather,
                )

            briefing.weather shouldBe weather
            briefing.hasContent() shouldBe true
            briefing.contentCount() shouldBe 1
        }

        "should reject briefing without any content" {
            shouldThrow<IllegalArgumentException> {
                DailyBriefing(
                    userId = userId,
                    date = date,
                    dayType = DayType.Weekday,
                    location = location,
                )
            }.message shouldBe "Daily briefing must contain at least one piece of content"
        }

        "should generate unique ID for each briefing" {
            val briefing1 =
                DailyBriefing(
                    userId = userId,
                    date = date,
                    dayType = DayType.Weekday,
                    location = location,
                    news = listOf(NewsArticle("News", "Summary", "URL")),
                )

            val briefing2 =
                DailyBriefing(
                    userId = userId,
                    date = date,
                    dayType = DayType.Weekday,
                    location = location,
                    news = listOf(NewsArticle("News", "Summary", "URL")),
                )

            briefing1.id shouldNotBe briefing2.id
        }

        "should track generation timestamp" {
            val beforeCreation = LocalDateTime.now()

            val briefing =
                DailyBriefing(
                    userId = userId,
                    date = date,
                    dayType = DayType.Weekday,
                    location = location,
                    news = listOf(NewsArticle("News", "Summary", "URL")),
                )

            val afterCreation = LocalDateTime.now()

            (briefing.generatedAt >= beforeCreation) shouldBe true
            (briefing.generatedAt <= afterCreation) shouldBe true
        }

        "should calculate content count correctly" {
            val briefing =
                DailyBriefing(
                    userId = userId,
                    date = date,
                    dayType = DayType.Weekday,
                    location = location,
                    news =
                        listOf(
                            NewsArticle("News 1", "Summary 1", "URL 1"),
                            NewsArticle("News 2", "Summary 2", "URL 2"),
                        ),
                    weather =
                        WeatherInfo(
                            date = date,
                            temperature = WeatherInfo.Temperature(10.0, 25.0, 18.0),
                            condition = "Sunny",
                            humidity = 65,
                            uvIndex = 5,
                            precipitation = 0,
                        ),
                    financialQuotes = emptyList(),
                    calendarEvents = emptyList(),
                )

            briefing.contentCount() shouldBe 3 // 2 news + 1 weather
        }

        "should have value semantics for all properties" {
            val id = java.util.UUID.randomUUID()
            val news = listOf(NewsArticle("News", "Summary", "URL"))
            val generatedAt = LocalDateTime.now()

            val briefing1 =
                DailyBriefing(
                    id = id,
                    userId = userId,
                    date = date,
                    dayType = DayType.Weekday,
                    location = location,
                    news = news,
                    generatedAt = generatedAt,
                )

            val briefing2 =
                DailyBriefing(
                    id = id,
                    userId = userId,
                    date = date,
                    dayType = DayType.Weekday,
                    location = location,
                    news = news,
                    generatedAt = generatedAt,
                )

            briefing1 shouldBe briefing2
            briefing1.hashCode() shouldBe briefing2.hashCode()
        }
    })
