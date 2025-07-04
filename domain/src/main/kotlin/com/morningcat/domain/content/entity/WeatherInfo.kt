package com.morningcat.domain.content.entity

import java.time.LocalDate

data class WeatherInfo(
    val date: LocalDate,
    val temperature: Temperature,
    val condition: String,
    val humidity: Int,
    val uvIndex: Int,
    val precipitation: Int,
) {
    init {
        require(condition.isNotBlank()) { "Weather condition cannot be blank" }
        require(humidity in 0..100) { "Humidity must be between 0 and 100" }
        require(uvIndex >= 0) { "UV index cannot be negative" }
        require(precipitation >= 0) { "Precipitation cannot be negative" }
    }

    data class Temperature(
        val min: Double,
        val max: Double,
        val current: Double,
    ) {
        init {
            require(min <= max) { "Minimum temperature cannot be greater than maximum" }
            require(current in min..max) { "Current temperature must be between min and max" }
        }
    }
}
