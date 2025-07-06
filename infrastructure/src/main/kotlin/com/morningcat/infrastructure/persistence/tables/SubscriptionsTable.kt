package com.morningcat.infrastructure.persistence.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.kotlin.datetime.time

/**
 * Database table for persisting Subscription aggregates.
 *
 * This table flattens the Subscription aggregate structure:
 * - Value objects (Location) are stored as separate columns
 * - Collections (delivery channels, content preferences) are denormalized as boolean columns
 * - Financial instruments are stored in a separate table for normalization
 */
object SubscriptionsTable : Table("subscriptions") {
    // Primary key
    val userId = uuid("user_id")

    // Delivery settings
    val deliveryTime = time("delivery_time")
    val weekendDelivery = bool("weekend_delivery")

    // Location (flattened value object)
    val city = varchar("city", 100)
    val countryCode = varchar("country_code", 2)

    // Delivery channels (stored as separate boolean columns)
    val emailEnabled = bool("email_enabled")
    val pushNotificationEnabled = bool("push_notification_enabled")

    // Content preferences (stored as separate boolean columns)
    val contentNews = bool("content_news")
    val contentFinance = bool("content_finance")
    val contentWeather = bool("content_weather")
    val contentCalendar = bool("content_calendar")
    val contentSelfImprovement = bool("content_self_improvement")
    val contentEntertainment = bool("content_entertainment")

    // Financial preferences are stored in a separate table (SubscriptionFinancialInstrumentsTable)
    // for proper normalization and to avoid storing collections as strings

    // Subscription status
    val isEnabled = bool("is_enabled")

    // Audit fields
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(userId)

    init {
        // Add indexes for common query patterns
        index(false, deliveryTime)
        index(false, isEnabled)
        index(false, countryCode, city)
    }
}
