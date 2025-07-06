package com.morningcat.domain.user.error

import com.morningcat.domain.user.valueobject.UserId

sealed class SubscriptionError {
    data class NotFound(
        val userId: UserId,
    ) : SubscriptionError()

    data class InvalidDeliveryTime(
        val time: String,
    ) : SubscriptionError()

    data class ValidationFailed(
        val message: String,
    ) : SubscriptionError()

    data class UpdateFailed(
        val reason: String,
    ) : SubscriptionError()
}
