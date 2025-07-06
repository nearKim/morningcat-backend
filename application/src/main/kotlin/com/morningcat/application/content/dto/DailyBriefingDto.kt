package com.morningcat.application.content.dto

import kotlinx.serialization.Serializable

@Serializable
data class DailyBriefingDto(
    val id: String,
    val userId: String,
    val date: String,
    val dayType: String,
    val location: BriefingLocationDto,
    val news: List<NewsArticleDto>,
    val weather: WeatherInfoDto?,
    val financialQuotes: List<FinancialQuoteDto>,
    val calendarEvents: List<CalendarEventDto>,
    val selfImprovementTips: List<SelfImprovementTipDto>,
    val entertainmentRecommendations: List<EntertainmentRecommendationDto>,
    val generatedAt: String,
)

@Serializable
data class BriefingLocationDto(
    val city: String,
    val countryCode: String,
)

@Serializable
data class NewsArticleDto(
    val title: String,
    val summary: String,
    val source: String,
    val publishedAt: String,
    val url: String?,
    val category: String,
)

@Serializable
data class WeatherInfoDto(
    val date: String,
    val temperature: TemperatureDto,
    val condition: String,
    val humidity: Int,
    val uvIndex: Int,
    val precipitation: Int,
)

@Serializable
data class TemperatureDto(
    val min: Double,
    val max: Double,
    val current: Double,
)

@Serializable
data class FinancialQuoteDto(
    val symbol: String,
    val name: String,
    val currentPrice: String,
    val previousClose: String,
    val change: String,
    val changePercent: String,
    val timestamp: String,
)

@Serializable
data class CalendarEventDto(
    val title: String,
    val description: String,
    val startTime: String,
    val endTime: String,
    val location: String?,
    val isAllDay: Boolean,
)

@Serializable
data class SelfImprovementTipDto(
    val category: String,
    val title: String,
    val content: String,
    val source: String?,
)

@Serializable
data class EntertainmentRecommendationDto(
    val title: String,
    val type: String,
    val description: String,
    val duration: Int?,
    val source: String,
    val url: String?,
)
