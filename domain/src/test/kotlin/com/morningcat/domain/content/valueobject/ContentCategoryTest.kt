package com.morningcat.domain.content.valueobject

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ContentCategoryTest :
    StringSpec({

        "should have all expected content categories" {
            ContentCategory.values().toList() shouldContainExactly
                listOf(
                    ContentCategory.NEWS,
                    ContentCategory.WEATHER,
                    ContentCategory.FINANCE,
                    ContentCategory.CALENDAR,
                    ContentCategory.SELF_IMPROVEMENT,
                    ContentCategory.ENTERTAINMENT,
                )
        }

        "should return correct display names" {
            ContentCategory.NEWS.displayName() shouldBe "News"
            ContentCategory.WEATHER.displayName() shouldBe "Weather"
            ContentCategory.FINANCE.displayName() shouldBe "Finance"
            ContentCategory.CALENDAR.displayName() shouldBe "Calendar"
            ContentCategory.SELF_IMPROVEMENT.displayName() shouldBe "Self Improvement"
            ContentCategory.ENTERTAINMENT.displayName() shouldBe "Entertainment"
        }

        "should support exhaustive when expression" {
            fun getCategoryDescription(category: ContentCategory): String =
                when (category) {
                    ContentCategory.NEWS -> "Latest news and updates"
                    ContentCategory.WEATHER -> "Weather forecast"
                    ContentCategory.FINANCE -> "Financial information"
                    ContentCategory.CALENDAR -> "Calendar events"
                    ContentCategory.SELF_IMPROVEMENT -> "Personal development"
                    ContentCategory.ENTERTAINMENT -> "Entertainment content"
                }

            getCategoryDescription(ContentCategory.NEWS) shouldBe "Latest news and updates"
            getCategoryDescription(ContentCategory.SELF_IMPROVEMENT) shouldBe "Personal development"
        }

        "should have correct enum properties" {
            ContentCategory.NEWS.name shouldBe "NEWS"
            ContentCategory.NEWS.ordinal shouldBe 0
            ContentCategory.ENTERTAINMENT.ordinal shouldBe 5
        }

        "should support valueOf" {
            ContentCategory.valueOf("NEWS") shouldBe ContentCategory.NEWS
            ContentCategory.valueOf("SELF_IMPROVEMENT") shouldBe ContentCategory.SELF_IMPROVEMENT
        }

        "should be usable in collections" {
            val categories =
                setOf(
                    ContentCategory.NEWS,
                    ContentCategory.WEATHER,
                    ContentCategory.NEWS, // Duplicate
                )

            categories shouldHaveSize 2 // Set removes duplicates
            categories.contains(ContentCategory.NEWS) shouldBe true
            categories.contains(ContentCategory.FINANCE) shouldBe false
        }

        "should iterate over all values" {
            val allCategories = ContentCategory.values().toList()
            allCategories shouldHaveSize 6
            allCategories.first() shouldBe ContentCategory.NEWS
            allCategories.last() shouldBe ContentCategory.ENTERTAINMENT
        }
    })
