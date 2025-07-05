package com.morningcat.domain.recommendation.service

import com.morningcat.domain.content.entity.WeatherInfo
import com.morningcat.domain.recommendation.valueobject.ActivityRecommendation
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate

class ActivityRecommenderTest :
    DescribeSpec({
        describe("ActivityRecommender") {
            val activityRecommender = ActivityRecommender()

            describe("recommend") {
                it("should return outdoor activity recommendation for sunny and warm weather") {
                    // Given
                    val sunnyWeather =
                        WeatherInfo(
                            date = LocalDate.now(),
                            temperature =
                                WeatherInfo.Temperature(
                                    min = 20.0,
                                    max = 30.0,
                                    current = 25.0,
                                ),
                            condition = "Clear sky",
                            humidity = 60,
                            uvIndex = 7,
                            precipitation = 0,
                        )

                    // When
                    val recommendations = activityRecommender.recommend(sunnyWeather)

                    // Then
                    recommendations shouldHaveSize 1
                    recommendations.first().apply {
                        title shouldBe "Perfect Day for Outdoor Activities"
                        description shouldContain "sunny"
                        type shouldBe ActivityRecommendation.ActivityType.OUTDOOR
                    }
                }

                it("should return indoor activity recommendation for rainy weather") {
                    // Given
                    val rainyWeather =
                        WeatherInfo(
                            date = LocalDate.now(),
                            temperature =
                                WeatherInfo.Temperature(
                                    min = 10.0,
                                    max = 18.0,
                                    current = 15.0,
                                ),
                            condition = "Heavy rain",
                            humidity = 85,
                            uvIndex = 1,
                            precipitation = 80,
                        )

                    // When
                    val recommendations = activityRecommender.recommend(rainyWeather)

                    // Then
                    recommendations shouldHaveSize 1
                    recommendations.first().apply {
                        title shouldBe "Great Day for Indoor Activities"
                        description shouldContain "rain"
                        type shouldBe ActivityRecommendation.ActivityType.INDOOR
                    }
                }
            }
        }
    })
