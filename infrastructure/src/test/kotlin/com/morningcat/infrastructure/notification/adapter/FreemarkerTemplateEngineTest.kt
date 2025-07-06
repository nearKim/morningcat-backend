package com.morningcat.infrastructure.notification.adapter

import com.morningcat.domain.content.aggregate.DailyBriefing
import com.morningcat.domain.content.entity.CalendarEvent
import com.morningcat.domain.content.entity.EntertainmentRecommendation
import com.morningcat.domain.content.entity.FinancialQuote
import com.morningcat.domain.content.entity.NewsArticle
import com.morningcat.domain.content.entity.SelfImprovementTip
import com.morningcat.domain.content.entity.WeatherInfo
import com.morningcat.domain.schedule.valueobject.DayType
import com.morningcat.domain.user.aggregate.User
import com.morningcat.domain.user.valueobject.EmailAddress
import com.morningcat.domain.user.valueobject.Location
import com.morningcat.domain.user.valueobject.UserId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class FreemarkerTemplateEngineTest :
    StringSpec({
        val engine = FreemarkerTemplateEngine()

        val userId = UserId(UUID.randomUUID())
        val email = EmailAddress.create("user@example.com").getOrNull()!!
        val user = User.register(userId, email, "John Doe")

        "should render basic daily briefing with news and weather" {
            val briefing =
                DailyBriefing(
                    userId = userId,
                    date = LocalDate.of(2025, 7, 6),
                    dayType = DayType.Weekday,
                    location = Location.create("Seoul", "KR"),
                    news =
                        listOf(
                            NewsArticle(
                                headline = "Tech Giant Announces New Product",
                                summary = "A major technology company unveiled its latest innovation today.",
                                url = "https://news.example.com/tech-announcement",
                            ),
                            NewsArticle(
                                headline = "Climate Summit Reaches Agreement",
                                summary = "World leaders agree on new climate targets.",
                                url = "https://news.example.com/climate-summit",
                            ),
                        ),
                    weather =
                        WeatherInfo(
                            date = LocalDate.of(2025, 7, 6),
                            temperature =
                                WeatherInfo.Temperature(
                                    min = 22.0,
                                    max = 30.0,
                                    current = 26.0,
                                ),
                            condition = "Partly Cloudy",
                            humidity = 65,
                            uvIndex = 7,
                            precipitation = 10,
                        ),
                )

            val html = engine.renderDailyBriefing(briefing, user)

            // Basic content check
            html shouldContain "John Doe"
            html shouldContain "July 6, 2025"
            html shouldContain "Seoul, KR"

            // Weather section
            html shouldContain "26.0°C"
            html shouldContain "Partly Cloudy"
            html shouldContain "UV Index: 7"
            html shouldContain "Humidity: 65%"

            // News section
            html shouldContain "Tech Giant Announces New Product"
            html shouldContain "A major technology company unveiled its latest innovation today."
            html shouldContain "Climate Summit Reaches Agreement"
        }

        "should render weekend briefing with entertainment recommendations" {
            val briefing =
                DailyBriefing(
                    userId = userId,
                    date = LocalDate.of(2025, 7, 7),
                    dayType = DayType.WeekendOrHoliday,
                    location = Location.create("Seoul", "KR"),
                    entertainmentRecommendations =
                        listOf(
                            EntertainmentRecommendation(
                                title = "The Great Movie",
                                type = EntertainmentRecommendation.EntertainmentType.MOVIE,
                                description = "A highly acclaimed film about adventure.",
                                duration = 120,
                                source = "Netflix",
                                url = "https://netflix.com/movie",
                            ),
                            EntertainmentRecommendation(
                                title = "Amazing Podcast",
                                type = EntertainmentRecommendation.EntertainmentType.PODCAST,
                                description = "A thought-provoking podcast about technology.",
                                duration = 45,
                                source = "Spotify",
                                url = "https://spotify.com/podcast",
                            ),
                        ),
                    selfImprovementTips =
                        listOf(
                            SelfImprovementTip(
                                title = "Morning Wellness Tip",
                                content = "Take a 30-minute walk in nature today.",
                                category = SelfImprovementTip.TipCategory.HEALTH,
                                estimatedReadTime = 2,
                            ),
                        ),
                )

            val html = engine.renderDailyBriefing(briefing, user)

            // Weekend content
            html shouldContain "Weekend Edition"
            html shouldContain "The Great Movie"
            html shouldContain "MOVIE"
            html shouldContain "Amazing Podcast"
            html shouldContain "Take a 30-minute walk in nature today"
        }

        "should render financial quotes and calendar events" {
            val briefing =
                DailyBriefing(
                    userId = userId,
                    date = LocalDate.of(2025, 7, 6),
                    dayType = DayType.Weekday,
                    location = Location.create("Seoul", "KR"),
                    financialQuotes =
                        listOf(
                            FinancialQuote(
                                symbol = "AAPL",
                                name = "Apple Inc.",
                                currentPrice = BigDecimal("185.50"),
                                previousClose = BigDecimal("183.20"),
                                change = BigDecimal("2.30"),
                                changePercent = BigDecimal("1.25"),
                                timestamp = LocalDateTime.now(),
                            ),
                        ),
                    calendarEvents =
                        listOf(
                            CalendarEvent(
                                title = "Team Meeting",
                                description = "Weekly team sync",
                                startTime = LocalDateTime.of(2025, 7, 6, 10, 0),
                                endTime = LocalDateTime.of(2025, 7, 6, 11, 0),
                                location = "Conference Room A",
                                isAllDay = false,
                            ),
                        ),
                )

            val html = engine.renderDailyBriefing(briefing, user)

            // Financial section
            html shouldContain "AAPL"
            html shouldContain "Apple Inc."
            html shouldContain "$185.50"
            html shouldContain "+1.25%"

            // Calendar section
            html shouldContain "Team Meeting"
            html shouldContain "10:00 - 11:00"
            html shouldContain "Conference Room A"
        }

        "should handle empty sections gracefully" {
            val briefing =
                DailyBriefing(
                    userId = userId,
                    date = LocalDate.of(2025, 7, 6),
                    dayType = DayType.Weekday,
                    location = Location.create("Seoul", "KR"),
                    news =
                        listOf(
                            NewsArticle(
                                headline = "Single News Item",
                                summary = "Just one news item today.",
                                url = "https://news.example.com/single",
                            ),
                        ),
                )

            val html = engine.renderDailyBriefing(briefing, user)

            html shouldContain "Single News Item"
            html shouldNotContain "Weather"
            html shouldNotContain "Financial Markets"
            html shouldNotContain "Calendar"
        }

        "should include proper HTML structure and styling" {
            val briefing =
                DailyBriefing(
                    userId = userId,
                    date = LocalDate.of(2025, 7, 6),
                    dayType = DayType.Weekday,
                    location = Location.create("Seoul", "KR"),
                    news =
                        listOf(
                            NewsArticle(
                                headline = "Test News",
                                summary = "Test summary",
                                url = "https://test.com",
                            ),
                        ),
                )

            val html = engine.renderDailyBriefing(briefing, user)

            html shouldContain "<!DOCTYPE html>"
            html shouldContain "<html"
            html shouldContain "<head>"
            html shouldContain "<meta charset=\"UTF-8\">"
            html shouldContain "<title>MorningCat Daily Briefing"
            html shouldContain "<body"
            html shouldContain "</html>"
        }
    })
