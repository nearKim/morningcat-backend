package com.morningcat.domain.content.aggregate

import com.morningcat.domain.content.entity.CalendarEvent
import com.morningcat.domain.content.entity.EntertainmentRecommendation
import com.morningcat.domain.content.entity.FinancialQuote
import com.morningcat.domain.content.entity.NewsArticle
import com.morningcat.domain.content.entity.SelfImprovementTip
import com.morningcat.domain.content.entity.WeatherInfo
import com.morningcat.domain.schedule.valueobject.DayType
import com.morningcat.domain.user.valueobject.Location
import com.morningcat.domain.user.valueobject.UserId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DailyBriefing(
    val id: UUID = UUID.randomUUID(),
    val userId: UserId,
    val date: LocalDate,
    val dayType: DayType,
    val location: Location,
    val news: List<NewsArticle> = emptyList(),
    val weather: WeatherInfo? = null,
    val financialQuotes: List<FinancialQuote> = emptyList(),
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val selfImprovementTips: List<SelfImprovementTip> = emptyList(),
    val entertainmentRecommendations: List<EntertainmentRecommendation> = emptyList(),
    val generatedAt: LocalDateTime = LocalDateTime.now(),
) {
    init {
        require(hasContent()) { "Daily briefing must contain at least one piece of content" }
    }

    fun hasContent(): Boolean =
        news.isNotEmpty() ||
            weather != null ||
            financialQuotes.isNotEmpty() ||
            calendarEvents.isNotEmpty() ||
            selfImprovementTips.isNotEmpty() ||
            entertainmentRecommendations.isNotEmpty()

    fun contentCount(): Int =
        news.size +
            (if (weather != null) 1 else 0) +
            financialQuotes.size +
            calendarEvents.size +
            selfImprovementTips.size +
            entertainmentRecommendations.size

    fun isEmpty(): Boolean = !hasContent()
}
