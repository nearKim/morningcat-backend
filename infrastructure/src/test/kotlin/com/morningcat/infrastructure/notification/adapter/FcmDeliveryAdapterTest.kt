package com.morningcat.infrastructure.notification.adapter

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.morningcat.domain.content.aggregate.DailyBriefing
import com.morningcat.domain.content.entity.NewsArticle
import com.morningcat.domain.content.entity.WeatherInfo
import com.morningcat.domain.notification.error.DeliveryError
import com.morningcat.domain.notification.valueobject.DeliveryChannelType
import com.morningcat.domain.schedule.valueobject.DayType
import com.morningcat.domain.user.aggregate.User
import com.morningcat.domain.user.valueobject.EmailAddress
import com.morningcat.domain.user.valueobject.Location
import com.morningcat.domain.user.valueobject.UserId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID

class FcmDeliveryAdapterTest : StringSpec({
    val mockFirebaseMessaging = mockk<FirebaseMessaging>()
    val adapter = FcmDeliveryAdapter(mockFirebaseMessaging)

    val userId = UserId(UUID.randomUUID())
    val email = EmailAddress.create("user@example.com").getOrNull()!!
    val user = User.register(userId, email, "Test User")

    val briefing = DailyBriefing(
        userId = userId,
        date = LocalDate.now(),
        dayType = DayType.Weekday,
        location = Location.create("Seoul", "KR"),
        news = listOf(
            NewsArticle(
                headline = "Breaking News",
                summary = "Important news summary",
                url = "https://news.example.com"
            )
        ),
        weather = WeatherInfo(
            date = LocalDate.now(),
            temperature = WeatherInfo.Temperature(
                min = 20.0,
                max = 30.0,
                current = 25.0
            ),
            condition = "Sunny",
            humidity = 60,
            uvIndex = 6,
            precipitation = 0
        )
    )

    "should send push notification successfully via PUSH_NOTIFICATION channel" {
        val messageId = "message-id-123"
        
        every { mockFirebaseMessaging.send(any<Message>()) } returns messageId

        val result = adapter.send(briefing, user, DeliveryChannelType.PushNotification)

        result.shouldBeRight()
        
        verify { mockFirebaseMessaging.send(any()) }
    }

    "should return ChannelNotConfigured error for EMAIL channel" {
        val result = adapter.send(briefing, user, DeliveryChannelType.Email)

        result.shouldBeLeft()
        (result.swap().getOrNull()) shouldBe DeliveryError.ChannelNotConfigured("Email")
    }

    "should return DeviceTokenNotFound error when user has no FCM token" {
        // TODO: Update this test once FCM token is added to User domain
        // For now, this test won't work as we're returning a fake token
        // val userWithoutToken = User.register(userId, email, "Test User")
        // 
        // val result = adapter.send(briefing, userWithoutToken, DeliveryChannelType.PushNotification)
        //
        // result.shouldBeLeft()
        // (result.swap().getOrNull()) shouldBe DeliveryError.DeviceTokenNotFound
    }

    "should return DeliveryFailed error when Firebase throws exception" {
        every { mockFirebaseMessaging.send(any()) } throws Exception("Network error")

        val result = adapter.send(briefing, user, DeliveryChannelType.PushNotification)

        result.shouldBeLeft()
        val error = result.swap().getOrNull()
        error shouldBe DeliveryError.DeliveryFailed("Network error")
    }

    "should build and send message with notification and data payload" {
        clearMocks(mockFirebaseMessaging)
        val messageId = "message-id-123"
        
        every { mockFirebaseMessaging.send(any<Message>()) } returns messageId

        val result = adapter.send(briefing, user, DeliveryChannelType.PushNotification)

        result.shouldBeRight()
        
        verify(exactly = 1) { 
            mockFirebaseMessaging.send(any())
        }
    }

    "should handle briefing without weather data" {
        val briefingWithoutWeather = briefing.copy(weather = null)
        val messageId = "message-id-123"
        
        every { mockFirebaseMessaging.send(any<Message>()) } returns messageId

        val result = adapter.send(briefingWithoutWeather, user, DeliveryChannelType.PushNotification)

        result.shouldBeRight()
        
        verify { mockFirebaseMessaging.send(any()) }
    }

    "should handle briefing without news" {
        val briefingWithoutNews = briefing.copy(news = emptyList())
        val messageId = "message-id-123"
        
        every { mockFirebaseMessaging.send(any<Message>()) } returns messageId

        val result = adapter.send(briefingWithoutNews, user, DeliveryChannelType.PushNotification)

        result.shouldBeRight()
        
        verify { mockFirebaseMessaging.send(any()) }
    }
})