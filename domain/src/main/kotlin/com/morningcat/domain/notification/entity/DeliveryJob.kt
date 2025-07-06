package com.morningcat.domain.notification.entity

import com.morningcat.domain.content.aggregate.DailyBriefing
import com.morningcat.domain.notification.valueobject.DeliveryStatus
import com.morningcat.domain.user.aggregate.User
import java.time.LocalDateTime
import java.util.UUID

data class DeliveryJob(
    val id: UUID = UUID.randomUUID(),
    val briefing: DailyBriefing,
    val user: User,
    val status: DeliveryStatus = DeliveryStatus.PENDING,
    val attempts: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val scheduledFor: LocalDateTime = LocalDateTime.now(),
    val lastAttemptAt: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null,
    val errorMessage: String? = null,
) {
    companion object {
        const val MAX_RETRY_ATTEMPTS = 3
    }

    fun canRetry(): Boolean = attempts < MAX_RETRY_ATTEMPTS && status == DeliveryStatus.FAILED

    fun incrementAttempts(): DeliveryJob =
        copy(
            attempts = attempts + 1,
            lastAttemptAt = LocalDateTime.now(),
        )

    fun markCompleted(): DeliveryJob =
        copy(
            status = DeliveryStatus.COMPLETED,
            completedAt = LocalDateTime.now(),
        )

    fun markFailed(error: String): DeliveryJob =
        copy(
            status = DeliveryStatus.FAILED,
            errorMessage = error,
            lastAttemptAt = LocalDateTime.now(),
        )

    fun markInProgress(): DeliveryJob =
        copy(
            status = DeliveryStatus.IN_PROGRESS,
            lastAttemptAt = LocalDateTime.now(),
        )
}
