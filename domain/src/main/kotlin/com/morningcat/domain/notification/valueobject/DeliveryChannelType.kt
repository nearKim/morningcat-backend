package com.morningcat.domain.notification.valueobject

sealed class DeliveryChannelType {
    object Email : DeliveryChannelType()
    object PushNotification : DeliveryChannelType()
    
    fun displayName(): String = when (this) {
        Email -> "Email"
        PushNotification -> "Push Notification"
    }
}