package com.morningcat.domain.notification.ports

import arrow.core.Either
import com.morningcat.domain.content.aggregate.DailyBriefing
import com.morningcat.domain.notification.entity.DeliveryJob
import com.morningcat.domain.notification.error.DeliveryError
import com.morningcat.domain.user.aggregate.User

interface DeliveryQueuePort {
    suspend fun enqueue(
        briefing: DailyBriefing,
        user: User,
    ): Either<DeliveryError, Unit>

    suspend fun dequeue(): DeliveryJob?
}