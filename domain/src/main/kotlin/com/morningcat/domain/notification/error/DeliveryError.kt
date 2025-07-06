package com.morningcat.domain.notification.error

sealed class DeliveryError(
    val message: String,
) {
    data class InvalidChannel(
        val channel: String,
    ) : DeliveryError("Invalid delivery channel: $channel")

    data class RecipientNotFound(
        val userId: String,
    ) : DeliveryError("Recipient not found: $userId")

    data class ChannelNotConfigured(
        val channel: String,
    ) : DeliveryError("Channel not configured: $channel")

    data class DeliveryFailed(
        val reason: String,
    ) : DeliveryError("Delivery failed: $reason")

    data class RateLimitExceeded(
        val retryAfter: Long?,
    ) : DeliveryError("Rate limit exceeded")

    data class ContentTooLarge(
        val maxSize: Long,
    ) : DeliveryError("Content exceeds maximum size: $maxSize bytes")

    data class TemplateError(
        val details: String,
    ) : DeliveryError("Template processing error: $details")

    data class QueueError(
        val details: String,
    ) : DeliveryError("Queue operation error: $details")

    data object DeviceTokenNotFound : DeliveryError("Device token not found for push notification")
}
