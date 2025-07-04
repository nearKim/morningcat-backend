package com.morningcat.domain.user.aggregate

import com.morningcat.domain.content.valueobject.ContentCategory
import com.morningcat.domain.notification.valueobject.DeliveryChannelType
import com.morningcat.domain.user.valueobject.Location
import com.morningcat.domain.user.valueobject.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.time.LocalTime

class SubscriptionTest :
    StringSpec({

        val userId = UserId.generate()
        val location = Location("Seoul", "KR")

        "should create subscription with defaults" {
            val subscription =
                Subscription.create(
                    userId = userId,
                    location = location,
                )

            subscription.userId shouldBe userId
            subscription.getDeliveryTime() shouldBe LocalTime.of(7, 0)
            subscription.getLocation() shouldBe location
            subscription.getDeliveryChannels() shouldBe setOf(DeliveryChannelType.Email)
            subscription.isEnabled() shouldBe true

            // Check default content preferences
            subscription.isContentSelected(ContentCategory.News) shouldBe true
            subscription.isContentSelected(ContentCategory.Weather) shouldBe true
            subscription.isContentSelected(ContentCategory.Finance) shouldBe false
        }

        "should create subscription with custom settings" {
            val deliveryTime = LocalTime.of(8, 30)
            val channels = setOf(DeliveryChannelType.Email, DeliveryChannelType.PushNotification)
            val contentPrefs =
                mapOf(
                    ContentCategory.News to false,
                    ContentCategory.Finance to true,
                )

            val subscription =
                Subscription.create(
                    userId = userId,
                    deliveryTime = deliveryTime,
                    location = location,
                    deliveryChannels = channels,
                    contentPreferences = contentPrefs,
                    financialPreferences = setOf("AAPL", "GOOGL"),
                    isEnabled = false,
                )

            subscription.getDeliveryTime() shouldBe deliveryTime
            subscription.getDeliveryChannels() shouldBe channels
            subscription.isContentSelected(ContentCategory.Finance) shouldBe true
            subscription.getFinancialPreferences() shouldBe setOf("AAPL", "GOOGL")
            subscription.isEnabled() shouldBe false
        }

        "should validate delivery time range" {
            shouldThrow<IllegalArgumentException> {
                Subscription.create(
                    userId = userId,
                    deliveryTime = LocalTime.of(4, 59), // Too early
                    location = location,
                )
            }.message shouldBe "Delivery time must be between 5:00 AM and 10:00 PM"

            shouldThrow<IllegalArgumentException> {
                Subscription.create(
                    userId = userId,
                    deliveryTime = LocalTime.of(22, 1), // Too late
                    location = location,
                )
            }.message shouldBe "Delivery time must be between 5:00 AM and 10:00 PM"
        }

        "should require at least one delivery channel" {
            shouldThrow<IllegalArgumentException> {
                Subscription.create(
                    userId = userId,
                    location = location,
                    deliveryChannels = emptySet(),
                )
            }.message shouldBe "At least one delivery channel must be selected"
        }

        "should require at least one content category enabled" {
            val allDisabled =
                ContentCategory::class
                    .sealedSubclasses
                    .mapNotNull { it.objectInstance }
                    .associateWith { false }

            shouldThrow<IllegalArgumentException> {
                Subscription.create(
                    userId = userId,
                    location = location,
                    contentPreferences = allDisabled,
                )
            }.message shouldBe "At least one content category must be enabled"
        }

        "should update settings successfully" {
            val subscription = Subscription.create(userId = userId, location = location)

            val newTime = LocalTime.of(9, 0)
            val newLocation = Location("Tokyo", "JP")
            val newChannels = setOf(DeliveryChannelType.PushNotification)
            val newTickers = setOf("MSFT", "AMZN")

            subscription.updateSettings(
                newDeliveryTime = newTime,
                newLocation = newLocation,
                newDeliveryChannels = newChannels,
                newFinancialPreferences = newTickers,
            )

            subscription.getDeliveryTime() shouldBe newTime
            subscription.getLocation() shouldBe newLocation
            subscription.getDeliveryChannels() shouldBe newChannels
            subscription.getFinancialPreferences() shouldBe setOf("MSFT", "AMZN")
        }

        "should enable and disable subscription" {
            val subscription = Subscription.create(userId = userId, location = location, isEnabled = true)

            subscription.disable()
            subscription.isEnabled() shouldBe false

            subscription.enable()
            subscription.isEnabled() shouldBe true
        }

        "should prevent enabling already enabled subscription" {
            val subscription = Subscription.create(userId = userId, location = location, isEnabled = true)

            shouldThrow<IllegalArgumentException> {
                subscription.enable()
            }.message shouldBe "Subscription is already enabled"
        }

        "should select and deselect content categories" {
            val subscription = Subscription.create(userId = userId, location = location)

            subscription.selectContent(ContentCategory.Finance)
            subscription.isContentSelected(ContentCategory.Finance) shouldBe true

            subscription.deselectContent(ContentCategory.News)
            subscription.isContentSelected(ContentCategory.News) shouldBe false
        }

        "should prevent deselecting last content category" {
            val subscription =
                Subscription.create(
                    userId = userId,
                    location = location,
                    contentPreferences =
                        mapOf(
                            ContentCategory.News to true,
                            ContentCategory.Weather to false,
                        ),
                )

            shouldThrow<IllegalArgumentException> {
                subscription.deselectContent(ContentCategory.News)
            }.message shouldBe "At least one content category must remain enabled"
        }

        "should manage financial tickers" {
            val subscription = Subscription.create(userId = userId, location = location)

            subscription.addFinancialTicker("aapl") // Should be normalized
            subscription.addFinancialTicker("GOOGL")

            subscription.getFinancialPreferences() shouldContain "AAPL"
            subscription.getFinancialPreferences() shouldContain "GOOGL"

            subscription.removeFinancialTicker("aapl")
            subscription.getFinancialPreferences() shouldNotContain "AAPL"
        }

        "should normalize financial tickers" {
            val subscription =
                Subscription.create(
                    userId = userId,
                    location = location,
                    financialPreferences = setOf("  aapl  ", "googl", "MSFT"),
                )

            subscription.getFinancialPreferences() shouldBe setOf("AAPL", "GOOGL", "MSFT")
        }

        "should have identity based on userId" {
            val subscription1 = Subscription.create(userId = userId, location = location)
            val subscription2 =
                Subscription.create(
                    userId = userId,
                    location = Location("Tokyo", "JP"),
                    deliveryTime = LocalTime.of(10, 0),
                )

            subscription1 shouldBe subscription2
            subscription1.hashCode() shouldBe subscription2.hashCode()
        }
    })
