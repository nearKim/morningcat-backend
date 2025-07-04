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
    private var isEnabled: Boolean
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
    
    // Behavior methods
    fun updateSettings(
        newDeliveryTime: LocalTime? = null,
        newLocation: Location? = null,
        newDeliveryChannels: Set<DeliveryChannelType>? = null,
        newFinancialPreferences: Set<String>? = null
    ) {
        newDeliveryTime?.let { 
            validateDeliveryTime(it)
            deliveryTime = it 
        }
        
        newLocation?.let { 
            location = it 
        }
        
        newDeliveryChannels?.let { channels ->
            require(channels.isNotEmpty()) { 
                "At least one delivery channel must be selected" 
            }
            deliveryChannels = channels.toSet()
        }
        
        newFinancialPreferences?.let { prefs ->
            financialPreferences = prefs.map { it.trim().uppercase() }
                .filter { it.isNotBlank() }
                .toSet()
        }
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
    
    fun isContentSelected(category: ContentCategory): Boolean {
        return contentPreferences[category] ?: false
    }
    
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
            isEnabled: Boolean = true
        ): Subscription {
            return Subscription(
                userId = userId,
                deliveryTime = deliveryTime,
                location = location,
                deliveryChannels = deliveryChannels.toSet(),
                contentPreferences = contentPreferences.toMap(),
                financialPreferences = financialPreferences.map { it.trim().uppercase() }.toSet(),
                isEnabled = isEnabled
            )
        }
        
        private fun defaultContentPreferences(): Map<ContentCategory, Boolean> {
            return mapOf(
                ContentCategory.News to true,
                ContentCategory.Weather to true,
                ContentCategory.Finance to false,
                ContentCategory.Calendar to false,
                ContentCategory.SelfImprovement to false,
                ContentCategory.Entertainment to false
            )
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Subscription) return false
        return userId == other.userId
    }
    
    override fun hashCode(): Int {
        return userId.hashCode()
    }
}