package com.morningcat.domain.content.valueobject

enum class ContentCategory(private val displayText: String) {
    NEWS("News"),
    WEATHER("Weather"),
    FINANCE("Finance"),
    CALENDAR("Calendar"),
    SELF_IMPROVEMENT("Self Improvement"),
    ENTERTAINMENT("Entertainment");

    fun displayName(): String = displayText
}
