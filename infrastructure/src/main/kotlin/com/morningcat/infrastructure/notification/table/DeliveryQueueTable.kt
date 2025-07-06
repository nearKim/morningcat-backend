package com.morningcat.infrastructure.notification.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime

object DeliveryQueueTable : UUIDTable("delivery_queue") {
    val briefingId = uuid("briefing_id")
    val userId = uuid("user_id")
    val userEmail = varchar("user_email", 255)
    val briefingData = text("briefing_data") // JSON serialized briefing
    val userData = text("user_data") // JSON serialized user
    val status = varchar("status", 50)
    val attempts = integer("attempts").default(0)
    val createdAt = datetime("created_at")
    val scheduledFor = datetime("scheduled_for")
    val lastAttemptAt = datetime("last_attempt_at").nullable()
    val completedAt = datetime("completed_at").nullable()
    val errorMessage = text("error_message").nullable()
}
