package com.morningcat.domain.user.ports

import com.morningcat.domain.user.aggregate.Subscription
import com.morningcat.domain.user.valueobject.UserId
import java.time.LocalTime

interface SubscriptionRepository {
    suspend fun findByUserId(userId: UserId): Subscription?
    suspend fun findAllScheduledFor(time: LocalTime): List<Subscription>
    suspend fun save(subscription: Subscription)
}