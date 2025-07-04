package com.morningcat.domain.recommendation.service

import com.morningcat.domain.content.entity.WeatherInfo
import com.morningcat.domain.recommendation.valueobject.ActivityRecommendation

class ActivityRecommender {
    fun recommend(weather: WeatherInfo): List<ActivityRecommendation> {
        return when {
            isGoodWeatherForOutdoor(weather) -> {
                listOf(
                    ActivityRecommendation(
                        title = "Perfect Day for Outdoor Activities",
                        description = "The weather is sunny and warm - ideal for outdoor activities like hiking, picnics, or sports!",
                        type = ActivityRecommendation.ActivityType.OUTDOOR
                    )
                )
            }
            isRainyWeather(weather) -> {
                listOf(
                    ActivityRecommendation(
                        title = "Great Day for Indoor Activities",
                        description = "With the rain outside, it's perfect for indoor activities like reading, visiting museums, or enjoying a cozy cafe!",
                        type = ActivityRecommendation.ActivityType.INDOOR
                    )
                )
            }
            else -> emptyList()
        }
    }
    
    private fun isGoodWeatherForOutdoor(weather: WeatherInfo): Boolean {
        val isSunny = weather.condition.contains("clear", ignoreCase = true) || 
                      weather.condition.contains("sunny", ignoreCase = true)
        val isWarm = weather.temperature.current >= 20.0
        val lowPrecipitation = weather.precipitation < 20
        
        return isSunny && isWarm && lowPrecipitation
    }
    
    private fun isRainyWeather(weather: WeatherInfo): Boolean {
        return weather.condition.contains("rain", ignoreCase = true) ||
               weather.precipitation > 50
    }
}