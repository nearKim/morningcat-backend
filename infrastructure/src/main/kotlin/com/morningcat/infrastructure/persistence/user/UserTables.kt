package com.morningcat.infrastructure.persistence.user

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Users : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object Subscriptions : Table("subscriptions") {
    val userId = uuid("user_id").references(Users.id)
    val deliveryTime = varchar("delivery_time", 10) // HH:mm format
    val locationCity = varchar("location_city", 255)
    val locationCountryCode = varchar("location_country_code", 2)
    val deliveryChannels = text("delivery_channels") // JSON array
    val contentPreferences = text("content_preferences") // JSON object
    val financialPreferences = text("financial_preferences") // JSON array
    val isEnabled = bool("is_enabled").default(false)
    val weekendDelivery = bool("weekend_delivery").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(userId)
}
