package com.morningcat.domain.user.aggregate

import com.morningcat.domain.content.valueobject.ContentCategory
import com.morningcat.domain.notification.valueobject.DeliveryChannelType
import com.morningcat.domain.user.valueobject.Location
import com.morningcat.domain.user.valueobject.UserId
import java.time.LocalTime

class Subscription private constructor(
    val userId: UserId,
    private var deliveryTime: LocalTime,
    private var location: Location,
    private var deliveryChannels: Set<DeliveryChannelType>,
    private var contentPreferences: Map<ContentCategory, Boolean>,
    private var financialPreferences: Set<String>,
    private var isEnabled: Boolean,
    private var weekendDelivery: Boolean,
) {
    init {
        require(deliveryChannels.isNotEmpty()) {
            "At least one delivery channel must be selected"
        }
        require(contentPreferences.values.any { it }) {
            "At least one content category must be enabled"
        }
        validateDeliveryTime(deliveryTime)
    }

    // Getters
    fun getDeliveryTime(): LocalTime = deliveryTime

    fun getLocation(): Location = location

    fun getDeliveryChannels(): Set<DeliveryChannelType> = deliveryChannels.toSet()

    fun getContentPreferences(): Map<ContentCategory, Boolean> = contentPreferences.toMap()

    fun getFinancialPreferences(): Set<String> = financialPreferences.toSet()

    fun isEnabled(): Boolean = isEnabled

    fun isWeekendDeliveryEnabled(): Boolean = weekendDelivery

    // Behavior methods
    fun updateSettings(
        isEnabled: Boolean,
        newLocation: Location,
        newDeliveryTime: LocalTime,
        weekendDelivery: Boolean,
        newDeliveryChannels: Set<DeliveryChannelType>,
        newContentPreferences: Map<ContentCategory, Boolean>,
        newFinancialInstruments: Set<String>
    ) {
        // Update enabled status
        this.isEnabled = isEnabled

        // Update location
        this.location = newLocation

        // Update delivery time with validation
        validateDeliveryTime(newDeliveryTime)
        this.deliveryTime = newDeliveryTime

        // Update weekend delivery preference
        this.weekendDelivery = weekendDelivery

        // Update delivery channels with validation
        require(newDeliveryChannels.isNotEmpty()) {
            "At least one delivery channel must be selected"
        }
        this.deliveryChannels = newDeliveryChannels.toSet()

        // Update content preferences with validation
        require(newContentPreferences.values.any { it }) {
            "At least one content category must be enabled"
        }
        this.contentPreferences = newContentPreferences.toMap()

        // Update financial instruments
        this.financialPreferences = newFinancialInstruments
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun enable() {
        require(!isEnabled) { "Subscription is already enabled" }
        isEnabled = true
    }

    fun disable() {
        require(isEnabled) { "Subscription is already disabled" }
        isEnabled = false
    }

    fun selectContent(category: ContentCategory) {
        contentPreferences = contentPreferences + (category to true)
    }

    fun deselectContent(category: ContentCategory) {
        val updatedPreferences = contentPreferences + (category to false)
        require(updatedPreferences.values.any { it }) {
            "At least one content category must remain enabled"
        }
        contentPreferences = updatedPreferences
    }

    fun isContentSelected(category: ContentCategory): Boolean = contentPreferences[category] ?: false

    fun addFinancialTicker(ticker: String) {
        require(ticker.isNotBlank()) { "Ticker cannot be blank" }
        financialPreferences = financialPreferences + ticker.trim().uppercase()
    }

    fun removeFinancialTicker(ticker: String) {
        financialPreferences = financialPreferences - ticker.trim().uppercase()
    }

    private fun validateDeliveryTime(time: LocalTime) {
        val earliestDelivery = LocalTime.of(5, 0)
        val latestDelivery = LocalTime.of(22, 0)

        require(time >= earliestDelivery && time <= latestDelivery) {
            "Delivery time must be between 5:00 AM and 10:00 PM"
        }
    }

    companion object {
        private const val DEFAULT_DELIVERY_HOUR = 7
        private const val DEFAULT_DELIVERY_MINUTE = 0

        fun create(
            userId: UserId,
            deliveryTime: LocalTime = LocalTime.of(DEFAULT_DELIVERY_HOUR, DEFAULT_DELIVERY_MINUTE),
            location: Location,
            deliveryChannels: Set<DeliveryChannelType> = setOf(DeliveryChannelType.Email),
            contentPreferences: Map<ContentCategory, Boolean> = defaultContentPreferences(),
            financialPreferences: Set<String> = emptySet(),
            isEnabled: Boolean = true,
            weekendDelivery: Boolean = true,
        ): Subscription =
            Subscription(
                userId = userId,
                deliveryTime = deliveryTime,
                location = location,
                deliveryChannels = deliveryChannels.toSet(),
                contentPreferences = contentPreferences.toMap(),
                financialPreferences = financialPreferences.map { it.trim().uppercase() }.toSet(),
                isEnabled = isEnabled,
                weekendDelivery = weekendDelivery,
            )

        private fun defaultContentPreferences(): Map<ContentCategory, Boolean> =
            mapOf(
                ContentCategory.NEWS to true,
                ContentCategory.WEATHER to true,
                ContentCategory.FINANCE to false,
                ContentCategory.CALENDAR to false,
                ContentCategory.SELF_IMPROVEMENT to false,
                ContentCategory.ENTERTAINMENT to false,
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Subscription) return false
        return userId == other.userId
    }

    override fun hashCode(): Int = userId.hashCode()
}
