package com.morningcat.domain.recommendation.valueobject

data class ActivityRecommendation(
    val title: String,
    val description: String,
    val type: ActivityType,
) {
    enum class ActivityType {
        INDOOR,
        OUTDOOR,
    }
}
