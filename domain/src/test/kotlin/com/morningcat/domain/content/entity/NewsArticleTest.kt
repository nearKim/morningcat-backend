package com.morningcat.domain.content.entity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class NewsArticleTest :
    StringSpec({

        "should create valid news article" {
            val article =
                NewsArticle(
                    headline = "Breaking News",
                    summary = "Important events happening today",
                    url = "https://example.com/news/123",
                )

            article.headline shouldBe "Breaking News"
            article.summary shouldBe "Important events happening today"
            article.url shouldBe "https://example.com/news/123"
        }

        "should reject blank headline" {
            shouldThrow<IllegalArgumentException> {
                NewsArticle(
                    headline = "",
                    summary = "Summary",
                    url = "https://example.com",
                )
            }.message shouldBe "Headline cannot be blank"

            shouldThrow<IllegalArgumentException> {
                NewsArticle(
                    headline = "   ",
                    summary = "Summary",
                    url = "https://example.com",
                )
            }.message shouldBe "Headline cannot be blank"
        }

        "should reject blank summary" {
            shouldThrow<IllegalArgumentException> {
                NewsArticle(
                    headline = "Headline",
                    summary = "",
                    url = "https://example.com",
                )
            }.message shouldBe "Summary cannot be blank"
        }

        "should reject blank URL" {
            shouldThrow<IllegalArgumentException> {
                NewsArticle(
                    headline = "Headline",
                    summary = "Summary",
                    url = "",
                )
            }.message shouldBe "URL cannot be blank"
        }

        "should have value semantics" {
            val article1 =
                NewsArticle(
                    headline = "Breaking News",
                    summary = "Important events",
                    url = "https://example.com/123",
                )

            val article2 =
                NewsArticle(
                    headline = "Breaking News",
                    summary = "Important events",
                    url = "https://example.com/123",
                )

            article1 shouldBe article2
            article1.hashCode() shouldBe article2.hashCode()
        }
    })
