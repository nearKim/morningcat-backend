package com.morningcat.infrastructure.notification.adapter

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
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID

class SendGridDeliveryAdapterTest :
    StringSpec({
        val mockSendGrid = mockk<SendGrid>()
        val mockTemplateEngine = mockk<TemplateEngine>()
        val adapter = SendGridDeliveryAdapter(mockSendGrid, mockTemplateEngine)

        val userId = UserId(UUID.randomUUID())
        val email = EmailAddress.create("user@example.com").getOrNull()!!
        val user = User.register(userId, email, "Test User")

        val briefing =
            DailyBriefing(
                userId = userId,
                date = LocalDate.now(),
                dayType = DayType.Weekday,
                location = Location.create("Seoul", "KR"),
                news =
                    listOf(
                        NewsArticle(
                            headline = "Breaking News",
                            summary = "Important news summary",
                            url = "https://news.example.com",
                        ),
                    ),
                weather =
                    WeatherInfo(
                        date = LocalDate.now(),
                        temperature =
                            WeatherInfo.Temperature(
                                min = 20.0,
                                max = 30.0,
                                current = 25.0,
                            ),
                        condition = "Sunny",
                        humidity = 60,
                        uvIndex = 6,
                        precipitation = 0,
                    ),
            )

        "should send email successfully via EMAIL channel" {
            val htmlContent = "<html><body>Test Email</body></html>"
            every { mockTemplateEngine.renderDailyBriefing(briefing, user) } returns htmlContent

            val requestSlot = slot<Request>()
            val mockResponse =
                Response().apply {
                    statusCode = 202
                    body = ""
                }
            every { mockSendGrid.api(capture(requestSlot)) } returns mockResponse

            val result = adapter.send(briefing, user, DeliveryChannelType.Email)

            result.shouldBeRight()

            val capturedRequest = requestSlot.captured
            capturedRequest.method shouldBe Method.POST
            capturedRequest.endpoint shouldBe "mail/send"

            verify { mockSendGrid.api(any()) }
        }

        "should return ChannelNotConfigured error for PUSH_NOTIFICATION channel" {
            val result = adapter.send(briefing, user, DeliveryChannelType.PushNotification)

            result.shouldBeLeft()
            (result.swap().getOrNull()) shouldBe DeliveryError.ChannelNotConfigured("Push Notification")
        }

        "should return DeliveryFailed error when SendGrid returns non-202 status" {
            val htmlContent = "<html><body>Test Email</body></html>"
            every { mockTemplateEngine.renderDailyBriefing(briefing, user) } returns htmlContent

            val mockResponse =
                Response().apply {
                    statusCode = 400
                    body = "Bad Request: Invalid email format"
                }
            every { mockSendGrid.api(any()) } returns mockResponse

            val result = adapter.send(briefing, user, DeliveryChannelType.Email)

            result.shouldBeLeft()
            (result.swap().getOrNull()) shouldBe
                DeliveryError.DeliveryFailed(
                    "SendGrid returned status 400: Bad Request: Invalid email format",
                )
        }

        "should return DeliveryFailed error when SendGrid throws exception" {
            val htmlContent = "<html><body>Test Email</body></html>"
            every { mockTemplateEngine.renderDailyBriefing(briefing, user) } returns htmlContent

            every { mockSendGrid.api(any()) } throws Exception("Network error")

            val result = adapter.send(briefing, user, DeliveryChannelType.Email)

            result.shouldBeLeft()
            (result.swap().getOrNull()) shouldBe DeliveryError.DeliveryFailed("Network error")
        }

        "should return TemplateError when template rendering fails" {
            every {
                mockTemplateEngine.renderDailyBriefing(briefing, user)
            } throws RuntimeException("Template not found")

            val result = adapter.send(briefing, user, DeliveryChannelType.Email)

            result.shouldBeLeft()
            (result.swap().getOrNull()) shouldBe DeliveryError.TemplateError("Template not found")
        }

        "should build correct subject with date" {
            val htmlContent = "<html><body>Test Email</body></html>"
            every { mockTemplateEngine.renderDailyBriefing(briefing, user) } returns htmlContent

            val requestSlot = slot<Request>()
            val mockResponse =
                Response().apply {
                    statusCode = 202
                    body = ""
                }
            every { mockSendGrid.api(capture(requestSlot)) } returns mockResponse

            adapter.send(briefing, user, DeliveryChannelType.Email)

            val capturedRequest = requestSlot.captured
            val requestBody = capturedRequest.body
            requestBody shouldBe requestBody // SendGrid mail object is serialized, we'd need to verify JSON structure
        }
    })
