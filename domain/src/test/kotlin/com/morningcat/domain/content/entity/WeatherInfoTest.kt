package com.morningcat.domain.content.entity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class WeatherInfoTest :
    StringSpec({

        "should create valid weather info" {
            val weather =
                WeatherInfo(
                    date = LocalDate.now(),
                    temperature = WeatherInfo.Temperature(min = 10.0, max = 25.0, current = 18.0),
                    condition = "Partly Cloudy",
                    humidity = 65,
                    uvIndex = 5,
                    precipitation = 20,
                )

            weather.condition shouldBe "Partly Cloudy"
            weather.humidity shouldBe 65
            weather.uvIndex shouldBe 5
            weather.precipitation shouldBe 20
        }

        "should create valid temperature" {
            val temp = WeatherInfo.Temperature(min = 10.0, max = 25.0, current = 18.0)

            temp.min shouldBe 10.0
            temp.max shouldBe 25.0
            temp.current shouldBe 18.0
        }

        "should reject invalid temperature range" {
            shouldThrow<IllegalArgumentException> {
                WeatherInfo.Temperature(min = 25.0, max = 10.0, current = 18.0)
            }.message shouldBe "Minimum temperature cannot be greater than maximum"
        }

        "should reject current temperature outside range" {
            shouldThrow<IllegalArgumentException> {
                WeatherInfo.Temperature(min = 10.0, max = 25.0, current = 30.0)
            }.message shouldBe "Current temperature must be between min and max"

            shouldThrow<IllegalArgumentException> {
                WeatherInfo.Temperature(min = 10.0, max = 25.0, current = 5.0)
            }.message shouldBe "Current temperature must be between min and max"
        }

        "should reject blank condition" {
            shouldThrow<IllegalArgumentException> {
                WeatherInfo(
                    date = LocalDate.now(),
                    temperature = WeatherInfo.Temperature(10.0, 25.0, 18.0),
                    condition = "",
                    humidity = 65,
                    uvIndex = 5,
                    precipitation = 20,
                )
            }.message shouldBe "Weather condition cannot be blank"
        }

        "should reject invalid humidity" {
            shouldThrow<IllegalArgumentException> {
                WeatherInfo(
                    date = LocalDate.now(),
                    temperature = WeatherInfo.Temperature(10.0, 25.0, 18.0),
                    condition = "Sunny",
                    humidity = -1,
                    uvIndex = 5,
                    precipitation = 20,
                )
            }.message shouldBe "Humidity must be between 0 and 100"

            shouldThrow<IllegalArgumentException> {
                WeatherInfo(
                    date = LocalDate.now(),
                    temperature = WeatherInfo.Temperature(10.0, 25.0, 18.0),
                    condition = "Sunny",
                    humidity = 101,
                    uvIndex = 5,
                    precipitation = 20,
                )
            }.message shouldBe "Humidity must be between 0 and 100"
        }

        "should reject negative UV index" {
            shouldThrow<IllegalArgumentException> {
                WeatherInfo(
                    date = LocalDate.now(),
                    temperature = WeatherInfo.Temperature(10.0, 25.0, 18.0),
                    condition = "Sunny",
                    humidity = 65,
                    uvIndex = -1,
                    precipitation = 20,
                )
            }.message shouldBe "UV index cannot be negative"
        }

        "should reject negative precipitation" {
            shouldThrow<IllegalArgumentException> {
                WeatherInfo(
                    date = LocalDate.now(),
                    temperature = WeatherInfo.Temperature(10.0, 25.0, 18.0),
                    condition = "Sunny",
                    humidity = 65,
                    uvIndex = 5,
                    precipitation = -1,
                )
            }.message shouldBe "Precipitation cannot be negative"
        }

        "should accept edge case values" {
            val weather =
                WeatherInfo(
                    date = LocalDate.now(),
                    temperature = WeatherInfo.Temperature(min = 0.0, max = 0.0, current = 0.0),
                    condition = "Freezing",
                    humidity = 0,
                    uvIndex = 0,
                    precipitation = 0,
                )

            weather.humidity shouldBe 0
            weather.uvIndex shouldBe 0
            weather.precipitation shouldBe 0
        }
    })
