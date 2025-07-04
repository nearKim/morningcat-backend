package com.morningcat.domain.notification.ports

import arrow.core.Either
import com.morningcat.domain.content.aggregate.DailyBriefing
import com.morningcat.domain.notification.error.DeliveryError
import com.morningcat.domain.notification.valueobject.DeliveryChannelType
import com.morningcat.domain.user.aggregate.User

interface DeliveryPort {
    suspend fun send(
        briefing: DailyBriefing,
        user: User,
        channel: DeliveryChannelType
    ): Either<DeliveryError, Unit>
}