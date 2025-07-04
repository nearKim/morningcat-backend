package com.morningcat.domain.notification.valueobject

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DeliveryChannelTypeTest :
    StringSpec({

        "should have Email and PushNotification as objects" {
            DeliveryChannelType.Email.shouldBeInstanceOf<DeliveryChannelType>()
            DeliveryChannelType.PushNotification.shouldBeInstanceOf<DeliveryChannelType>()
        }

        "should return correct display names" {
            DeliveryChannelType.Email.displayName() shouldBe "Email"
            DeliveryChannelType.PushNotification.displayName() shouldBe "Push Notification"
        }

        "should support exhaustive when expression" {
            fun getChannelIcon(channel: DeliveryChannelType): String =
                when (channel) {
                    is DeliveryChannelType.Email -> "📧"
                    is DeliveryChannelType.PushNotification -> "🔔"
                }

            getChannelIcon(DeliveryChannelType.Email) shouldBe "📧"
            getChannelIcon(DeliveryChannelType.PushNotification) shouldBe "🔔"
        }

        "should have object equality" {
            val email1 = DeliveryChannelType.Email
            val email2 = DeliveryChannelType.Email
            val push = DeliveryChannelType.PushNotification

            (email1 === email2) shouldBe true // Same instance
            (email1 == push) shouldBe false
        }

        "should be usable in sets" {
            val channels =
                setOf(
                    DeliveryChannelType.Email,
                    DeliveryChannelType.PushNotification,
                    DeliveryChannelType.Email, // Duplicate
                )

            channels.size shouldBe 2
            channels.contains(DeliveryChannelType.Email) shouldBe true
            channels.contains(DeliveryChannelType.PushNotification) shouldBe true
        }

        "should work with collection operations" {
            val allChannels =
                listOf(
                    DeliveryChannelType.Email,
                    DeliveryChannelType.PushNotification,
                )

            val displayNames = allChannels.map { it.displayName() }
            displayNames shouldBe listOf("Email", "Push Notification")
        }
    })
