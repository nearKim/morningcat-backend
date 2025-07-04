package com.morningcat.domain.content.valueobject

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ContentCategoryTest :
    StringSpec({

        "should have all content categories as objects" {
            ContentCategory.News.shouldBeInstanceOf<ContentCategory>()
            ContentCategory.Weather.shouldBeInstanceOf<ContentCategory>()
            ContentCategory.Finance.shouldBeInstanceOf<ContentCategory>()
            ContentCategory.Calendar.shouldBeInstanceOf<ContentCategory>()
            ContentCategory.SelfImprovement.shouldBeInstanceOf<ContentCategory>()
            ContentCategory.Entertainment.shouldBeInstanceOf<ContentCategory>()
        }

        "should return correct display names" {
            ContentCategory.News.displayName() shouldBe "News"
            ContentCategory.Weather.displayName() shouldBe "Weather"
            ContentCategory.Finance.displayName() shouldBe "Finance"
            ContentCategory.Calendar.displayName() shouldBe "Calendar"
            ContentCategory.SelfImprovement.displayName() shouldBe "Self Improvement"
            ContentCategory.Entertainment.displayName() shouldBe "Entertainment"
        }

        "should support exhaustive when expression" {
            fun getCategoryDescription(category: ContentCategory): String =
                when (category) {
                    is ContentCategory.News -> "Latest news and updates"
                    is ContentCategory.Weather -> "Weather forecast"
                    is ContentCategory.Finance -> "Financial information"
                    is ContentCategory.Calendar -> "Calendar events"
                    is ContentCategory.SelfImprovement -> "Personal development"
                    is ContentCategory.Entertainment -> "Entertainment content"
                }

            getCategoryDescription(ContentCategory.News) shouldBe "Latest news and updates"
            getCategoryDescription(ContentCategory.SelfImprovement) shouldBe "Personal development"
        }

        "should have object equality" {
            val news1 = ContentCategory.News
            val news2 = ContentCategory.News
            val weather = ContentCategory.Weather

            (news1 === news2) shouldBe true // Same instance
            (news1 == weather) shouldBe false
        }

        "should be usable in collections" {
            val categories =
                setOf(
                    ContentCategory.News,
                    ContentCategory.Weather,
                    ContentCategory.News, // Duplicate
                )

            categories.size shouldBe 2 // Set removes duplicates
            categories.contains(ContentCategory.News) shouldBe true
            categories.contains(ContentCategory.Finance) shouldBe false
        }
    })
