package com.morningcat.domain.user.valueobject

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LocationTest :
    StringSpec({

        "should create valid location" {
            val location = Location("Seoul", "KR")

            location.city shouldBe "Seoul"
            location.countryCode shouldBe "KR"
        }

        "should trim whitespace and uppercase country code" {
            val location = Location.create("  Seoul  ", "  kr  ")

            location.city shouldBe "Seoul"
            location.countryCode shouldBe "KR"
        }

        "should reject blank city" {
            shouldThrow<IllegalArgumentException> {
                Location("", "KR")
            }.message shouldBe "City cannot be blank"

            shouldThrow<IllegalArgumentException> {
                Location("   ", "KR")
            }.message shouldBe "City cannot be blank"
        }

        "should reject blank country code" {
            shouldThrow<IllegalArgumentException> {
                Location("Seoul", "")
            }.message shouldBe "Country code cannot be blank"

            shouldThrow<IllegalArgumentException> {
                Location("Seoul", "   ")
            }.message shouldBe "Country code cannot be blank"
        }

        "should enforce 2-letter country code" {
            shouldThrow<IllegalArgumentException> {
                Location("Seoul", "K")
            }.message shouldBe "Country code must be a 2-letter ISO code"

            shouldThrow<IllegalArgumentException> {
                Location("Seoul", "KOR")
            }.message shouldBe "Country code must be a 2-letter ISO code"
        }

        "should accept various valid locations" {
            val validLocations =
                listOf(
                    Location.create("New York", "US"),
                    Location.create("London", "GB"),
                    Location.create("Tokyo", "JP"),
                    Location.create("São Paulo", "BR"),
                    Location.create("Paris", "FR"),
                )

            validLocations.forEach { location ->
                location.countryCode.length shouldBe 2
                location.city.isNotBlank() shouldBe true
            }
        }

        "should have value semantics" {
            val location1 = Location("Seoul", "KR")
            val location2 = Location("Seoul", "KR")
            val location3 = Location("Tokyo", "JP")

            location1 shouldBe location2
            location1.hashCode() shouldBe location2.hashCode()
            location1 shouldBe location1.copy()

            (location1 == location3) shouldBe false
        }
    })
