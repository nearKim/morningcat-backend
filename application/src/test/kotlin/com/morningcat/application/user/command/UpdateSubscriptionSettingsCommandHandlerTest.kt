package com.morningcat.application.user.command

import arrow.core.left
import arrow.core.right
import com.morningcat.application.user.dto.*
import com.morningcat.domain.content.valueobject.ContentCategory
import com.morningcat.domain.notification.valueobject.DeliveryChannelType
import com.morningcat.domain.user.aggregate.Subscription
import com.morningcat.domain.user.error.SubscriptionError
import com.morningcat.domain.user.ports.SubscriptionRepository
import com.morningcat.domain.user.valueobject.Location
import com.morningcat.domain.user.valueobject.UserId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalTime
import java.util.*

class UpdateSubscriptionSettingsCommandHandlerTest : StringSpec({
    val repository = mockk<SubscriptionRepository>()
    val handler = UpdateSubscriptionSettingsCommandHandler(repository)

    beforeEach {
        clearAllMocks()
    }

    "should successfully update subscription settings" {
        // Given
        val userId = UserId(UUID.randomUUID())
        val subscription = mockk<Subscription>(relaxed = true)
        
        val dto = UserSettingsDto(
            general = GeneralPreferencesDto(
                isServiceEnabled = true,
                preferredLocation = LocationDto("Seoul", "KR")
            ),
            delivery = DeliveryPreferencesDto(
                deliveryTime = "08:30",
                receiveOnWeekends = true,
                emailEnabled = true,
                pushNotificationEnabled = true
            ),
            content = ContentPreferencesDto(
                includeNews = true,
                includeEconomicIndicators = true,
                includeWeather = true,
                includeSchedule = false,
                includeSelfImprovement = false,
                includeEntertainment = false
            ),
            personalization = PersonalizationPreferencesDto(
                financialInstruments = setOf("AAPL", "GOOGL")
            )
        )

        val command = UpdateSubscriptionSettingsCommand(userId, dto)

        coEvery { repository.findByUserId(userId) } returns subscription
        coEvery { repository.save(subscription) } just Runs

        // When
        val result = handler.handle(command)

        // Then
        result.shouldBeRight(Unit)

        verify {
            subscription.updateSettings(
                isEnabled = true,
                newLocation = Location("Seoul", "KR"),
                newDeliveryTime = LocalTime.of(8, 30),
                weekendDelivery = true,
                newDeliveryChannels = setOf(DeliveryChannelType.Email, DeliveryChannelType.PushNotification),
                newContentPreferences = mapOf(
                    ContentCategory.NEWS to true,
                    ContentCategory.FINANCE to true,
                    ContentCategory.WEATHER to true,
                    ContentCategory.CALENDAR to false,
                    ContentCategory.SELF_IMPROVEMENT to false,
                    ContentCategory.ENTERTAINMENT to false
                ),
                newFinancialInstruments = setOf("AAPL", "GOOGL")
            )
        }

        coVerify { repository.save(subscription) }
    }

    "should return NotFound error when subscription doesn't exist" {
        // Given
        val userId = UserId(UUID.randomUUID())
        val dto = UserSettingsDto(
            general = GeneralPreferencesDto(
                isServiceEnabled = true,
                preferredLocation = LocationDto("Seoul", "KR")
            ),
            delivery = DeliveryPreferencesDto(
                deliveryTime = "08:30",
                receiveOnWeekends = true,
                emailEnabled = true,
                pushNotificationEnabled = false
            ),
            content = ContentPreferencesDto(
                includeNews = true,
                includeEconomicIndicators = false,
                includeWeather = false,
                includeSchedule = false,
                includeSelfImprovement = false,
                includeEntertainment = false
            ),
            personalization = PersonalizationPreferencesDto(
                financialInstruments = emptySet()
            )
        )

        val command = UpdateSubscriptionSettingsCommand(userId, dto)

        coEvery { repository.findByUserId(userId) } returns null

        // When
        val result = handler.handle(command)

        // Then
        result.shouldBeLeft(SubscriptionError.NotFound(userId))
        coVerify(exactly = 0) { repository.save(any()) }
    }

    "should return InvalidDeliveryTime error for malformed time" {
        // Given
        val userId = UserId(UUID.randomUUID())
        val subscription = mockk<Subscription>()

        val dto = UserSettingsDto(
            general = GeneralPreferencesDto(
                isServiceEnabled = true,
                preferredLocation = LocationDto("Seoul", "KR")
            ),
            delivery = DeliveryPreferencesDto(
                deliveryTime = "25:99", // Invalid time
                receiveOnWeekends = true,
                emailEnabled = true,
                pushNotificationEnabled = false
            ),
            content = ContentPreferencesDto(
                includeNews = true,
                includeEconomicIndicators = false,
                includeWeather = false,
                includeSchedule = false,
                includeSelfImprovement = false,
                includeEntertainment = false
            ),
            personalization = PersonalizationPreferencesDto(
                financialInstruments = emptySet()
            )
        )

        val command = UpdateSubscriptionSettingsCommand(userId, dto)

        coEvery { repository.findByUserId(userId) } returns subscription

        // When
        val result = handler.handle(command)

        // Then
        result.shouldBeLeft(SubscriptionError.InvalidDeliveryTime("25:99"))
        verify(exactly = 0) { subscription.updateSettings(any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.save(any()) }
    }

    "should return ValidationFailed error when domain validation fails" {
        // Given
        val userId = UserId(UUID.randomUUID())
        val subscription = mockk<Subscription>()

        val dto = UserSettingsDto(
            general = GeneralPreferencesDto(
                isServiceEnabled = true,
                preferredLocation = LocationDto("Seoul", "KR")
            ),
            delivery = DeliveryPreferencesDto(
                deliveryTime = "03:00", // Too early, will fail domain validation
                receiveOnWeekends = true,
                emailEnabled = true,
                pushNotificationEnabled = false
            ),
            content = ContentPreferencesDto(
                includeNews = true,
                includeEconomicIndicators = false,
                includeWeather = false,
                includeSchedule = false,
                includeSelfImprovement = false,
                includeEntertainment = false
            ),
            personalization = PersonalizationPreferencesDto(
                financialInstruments = emptySet()
            )
        )

        val command = UpdateSubscriptionSettingsCommand(userId, dto)

        coEvery { repository.findByUserId(userId) } returns subscription
        every {
            subscription.updateSettings(any(), any(), any(), any(), any(), any(), any())
        } throws IllegalArgumentException("Delivery time must be between 5:00 AM and 10:00 PM")

        // When
        val result = handler.handle(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        (error is SubscriptionError.ValidationFailed) shouldBe true
        (error as SubscriptionError.ValidationFailed).message shouldBe "Delivery time must be between 5:00 AM and 10:00 PM"
        
        coVerify(exactly = 0) { repository.save(any()) }
    }

    "should handle both delivery channels disabled" {
        // Given
        val userId = UserId(UUID.randomUUID())
        val subscription = mockk<Subscription>()

        val dto = UserSettingsDto(
            general = GeneralPreferencesDto(
                isServiceEnabled = true,
                preferredLocation = LocationDto("Seoul", "KR")
            ),
            delivery = DeliveryPreferencesDto(
                deliveryTime = "08:00",
                receiveOnWeekends = true,
                emailEnabled = false,
                pushNotificationEnabled = false // Both channels disabled
            ),
            content = ContentPreferencesDto(
                includeNews = true,
                includeEconomicIndicators = false,
                includeWeather = false,
                includeSchedule = false,
                includeSelfImprovement = false,
                includeEntertainment = false
            ),
            personalization = PersonalizationPreferencesDto(
                financialInstruments = emptySet()
            )
        )

        val command = UpdateSubscriptionSettingsCommand(userId, dto)

        coEvery { repository.findByUserId(userId) } returns subscription
        every {
            subscription.updateSettings(any(), any(), any(), any(), any(), any(), any())
        } throws IllegalArgumentException("At least one delivery channel must be selected")

        // When
        val result = handler.handle(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        (error is SubscriptionError.ValidationFailed) shouldBe true
        (error as SubscriptionError.ValidationFailed).message shouldBe "At least one delivery channel must be selected"
    }

    "should correctly map all content preferences" {
        // Given
        val userId = UserId(UUID.randomUUID())
        val subscription = mockk<Subscription>(relaxed = true)

        val dto = UserSettingsDto(
            general = GeneralPreferencesDto(
                isServiceEnabled = false,
                preferredLocation = LocationDto("Tokyo", "JP")
            ),
            delivery = DeliveryPreferencesDto(
                deliveryTime = "19:45",
                receiveOnWeekends = false,
                emailEnabled = true,
                pushNotificationEnabled = false
            ),
            content = ContentPreferencesDto(
                includeNews = false,
                includeEconomicIndicators = true,
                includeWeather = false,
                includeSchedule = true,
                includeSelfImprovement = true,
                includeEntertainment = true
            ),
            personalization = PersonalizationPreferencesDto(
                financialInstruments = setOf("MSFT", "TSLA", "SPY")
            )
        )

        val command = UpdateSubscriptionSettingsCommand(userId, dto)

        coEvery { repository.findByUserId(userId) } returns subscription
        coEvery { repository.save(subscription) } just Runs

        val contentSlot = slot<Map<ContentCategory, Boolean>>()

        // When
        handler.handle(command)

        // Then
        verify {
            subscription.updateSettings(
                isEnabled = false,
                newLocation = Location("Tokyo", "JP"),
                newDeliveryTime = LocalTime.of(19, 45),
                weekendDelivery = false,
                newDeliveryChannels = setOf(DeliveryChannelType.Email),
                newContentPreferences = capture(contentSlot),
                newFinancialInstruments = setOf("MSFT", "TSLA", "SPY")
            )
        }

        val capturedContentPrefs = contentSlot.captured
        capturedContentPrefs[ContentCategory.NEWS] shouldBe false
        capturedContentPrefs[ContentCategory.FINANCE] shouldBe true
        capturedContentPrefs[ContentCategory.WEATHER] shouldBe false
        capturedContentPrefs[ContentCategory.CALENDAR] shouldBe true
        capturedContentPrefs[ContentCategory.SELF_IMPROVEMENT] shouldBe true
        capturedContentPrefs[ContentCategory.ENTERTAINMENT] shouldBe true
    }

    "should handle empty financial instruments" {
        // Given
        val userId = UserId(UUID.randomUUID())
        val subscription = mockk<Subscription>(relaxed = true)

        val dto = UserSettingsDto(
            general = GeneralPreferencesDto(
                isServiceEnabled = true,
                preferredLocation = LocationDto("Seoul", "KR")
            ),
            delivery = DeliveryPreferencesDto(
                deliveryTime = "07:00",
                receiveOnWeekends = true,
                emailEnabled = true,
                pushNotificationEnabled = false
            ),
            content = ContentPreferencesDto(
                includeNews = true,
                includeEconomicIndicators = false,
                includeWeather = false,
                includeSchedule = false,
                includeSelfImprovement = false,
                includeEntertainment = false
            ),
            personalization = PersonalizationPreferencesDto(
                financialInstruments = emptySet() // Empty set
            )
        )

        val command = UpdateSubscriptionSettingsCommand(userId, dto)

        coEvery { repository.findByUserId(userId) } returns subscription
        coEvery { repository.save(subscription) } just Runs

        // When
        val result = handler.handle(command)

        // Then
        result.shouldBeRight(Unit)

        verify {
            subscription.updateSettings(
                isEnabled = any(),
                newLocation = any(),
                newDeliveryTime = any(),
                weekendDelivery = any(),
                newDeliveryChannels = any(),
                newContentPreferences = any(),
                newFinancialInstruments = emptySet()
            )
        }
    }
})