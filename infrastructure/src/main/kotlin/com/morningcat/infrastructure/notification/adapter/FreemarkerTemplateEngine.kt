package com.morningcat.infrastructure.notification.adapter

import com.morningcat.domain.content.aggregate.DailyBriefing
import com.morningcat.domain.schedule.valueobject.DayType
import com.morningcat.domain.user.aggregate.User
import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import java.io.StringWriter
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class FreemarkerTemplateEngine : TemplateEngine {
    companion object {
        private const val TEMPLATE_NAME = "daily-briefing.ftl"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy")
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }

    private val configuration =
        Configuration(Configuration.VERSION_2_3_32).apply {
            setClassForTemplateLoading(this@FreemarkerTemplateEngine::class.java, "/templates")
            defaultEncoding = "UTF-8"
            templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
            logTemplateExceptions = false
            wrapUncheckedExceptions = true
            fallbackOnNullLoopVariable = false
        }

    override fun renderDailyBriefing(
        briefing: DailyBriefing,
        user: User,
    ): String {
        val template = configuration.getTemplate(TEMPLATE_NAME)
        val dataModel = buildDataModel(briefing, user)

        return StringWriter().use { writer ->
            template.process(dataModel, writer)
            writer.toString()
        }
    }

    private fun buildDataModel(
        briefing: DailyBriefing,
        user: User,
    ): Map<String, Any?> =
        mapOf(
            "user" to
                mapOf(
                    "name" to user.name,
                    "email" to user.getEmail().value,
                ),
            "briefing" to
                mapOf(
                    "date" to briefing.date.format(DATE_FORMATTER),
                    "dayOfWeek" to briefing.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    "location" to "${briefing.location.city}, ${briefing.location.countryCode}",
                    "isWeekend" to (briefing.dayType is DayType.WeekendOrHoliday),
                ),
            "weather" to
                briefing.weather?.let { weather ->
                    mapOf(
                        "currentTemp" to weather.temperature.current,
                        "minTemp" to weather.temperature.min,
                        "maxTemp" to weather.temperature.max,
                        "condition" to weather.condition,
                        "humidity" to weather.humidity,
                        "uvIndex" to weather.uvIndex,
                        "precipitation" to weather.precipitation,
                    )
                },
            "news" to
                briefing.news.map { article ->
                    mapOf(
                        "headline" to article.headline,
                        "summary" to article.summary,
                        "url" to article.url,
                    )
                },
            "financialQuotes" to
                briefing.financialQuotes.map { quote ->
                    mapOf(
                        "symbol" to quote.symbol,
                        "name" to quote.name,
                        "price" to quote.currentPrice,
                        "change" to quote.change,
                        "changePercent" to quote.changePercent,
                        "isPositive" to quote.isGain,
                    )
                },
            "calendarEvents" to
                briefing.calendarEvents.map { event ->
                    mapOf(
                        "title" to event.title,
                        "startTime" to event.startTime.format(TIME_FORMATTER),
                        "endTime" to event.endTime.format(TIME_FORMATTER),
                        "location" to event.location,
                        "description" to event.description,
                    )
                },
            "selfImprovementTips" to
                briefing.selfImprovementTips.map { tip ->
                    mapOf(
                        "category" to tip.category.name,
                        "tip" to tip.content,
                        "source" to tip.title,
                    )
                },
            "entertainmentRecommendations" to
                briefing.entertainmentRecommendations.map { rec ->
                    mapOf(
                        "title" to rec.title,
                        "type" to rec.type.name.replace("_", " "),
                        "description" to rec.description,
                        "rating" to null, // No rating property in the domain model
                    )
                },
        )
}
