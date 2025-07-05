package com.morningcat.application.user.command

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.morningcat.domain.content.valueobject.ContentCategory
import com.morningcat.domain.notification.valueobject.DeliveryChannelType
import com.morningcat.domain.user.aggregate.Subscription
import com.morningcat.domain.user.error.SubscriptionError
import com.morningcat.domain.user.ports.SubscriptionRepository
import com.morningcat.domain.user.valueobject.Location
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class UpdateSubscriptionSettingsCommandHandler(
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend fun handle(command: UpdateSubscriptionSettingsCommand): Either<SubscriptionError, Unit> {
        // Fetch the subscription from repository
        val subscription = subscriptionRepository.findByUserId(command.userId)
            ?: return SubscriptionError.NotFound(command.userId).left()

        // Unpack the UserSettingsDto
        val dto = command.settings

        // Translate DTO data to domain types
        val deliveryTime = try {
            LocalTime.parse(dto.delivery.deliveryTime, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: DateTimeParseException) {
            return SubscriptionError.InvalidDeliveryTime(dto.delivery.deliveryTime).left()
        }

        val location = Location(
            city = dto.general.preferredLocation.city,
            countryCode = dto.general.preferredLocation.countryCode
        )

        val deliveryChannels = buildSet {
            if (dto.delivery.emailEnabled) {
                add(DeliveryChannelType.Email)
            }
            if (dto.delivery.pushNotificationEnabled) {
                add(DeliveryChannelType.PushNotification)
            }
        }

        val contentPreferences = mapOf(
            ContentCategory.NEWS to dto.content.includeNews,
            ContentCategory.FINANCE to dto.content.includeEconomicIndicators,
            ContentCategory.WEATHER to dto.content.includeWeather,
            ContentCategory.CALENDAR to dto.content.includeSchedule,
            ContentCategory.SELF_IMPROVEMENT to dto.content.includeSelfImprovement,
            ContentCategory.ENTERTAINMENT to dto.content.includeEntertainment
        )

        // Call the aggregate's updateSettings method
        return try {
            subscription.updateSettings(
                isEnabled = dto.general.isServiceEnabled,
                newLocation = location,
                newDeliveryTime = deliveryTime,
                weekendDelivery = dto.delivery.receiveOnWeekends,
                newDeliveryChannels = deliveryChannels,
                newContentPreferences = contentPreferences,
                newFinancialInstruments = dto.personalization.financialInstruments
            )

            // Save the updated aggregate
            subscriptionRepository.save(subscription)
            Unit.right()
        } catch (e: IllegalArgumentException) {
            SubscriptionError.ValidationFailed(e.message ?: "Validation failed").left()
        }
    }
}