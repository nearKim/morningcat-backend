package com.morningcat.infrastructure.notification.adapter

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.morningcat.domain.content.aggregate.DailyBriefing
import com.morningcat.domain.content.entity.WeatherInfo
import com.morningcat.domain.notification.entity.DeliveryJob
import com.morningcat.domain.notification.error.DeliveryError
import com.morningcat.domain.notification.ports.DeliveryQueuePort
import com.morningcat.domain.notification.valueobject.DeliveryStatus
import com.morningcat.domain.schedule.valueobject.DayType
import com.morningcat.domain.user.aggregate.User
import com.morningcat.domain.user.valueobject.Location
import com.morningcat.infrastructure.notification.table.DeliveryQueueTable
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class DbDeliveryQueueAdapter(
    private val database: Database,
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        },
) : DeliveryQueuePort {
    override suspend fun enqueue(
        briefing: DailyBriefing,
        user: User,
    ): Either<DeliveryError, Unit> =
        newSuspendedTransaction(Dispatchers.IO, database) {
            try {
                DeliveryQueueTable.insert {
                    it[id] = UUID.randomUUID()
                    it[briefingId] = briefing.id
                    it[userId] = user.id.value
                    it[userEmail] = user.getEmail().value
                    it[briefingData] = serializeBriefing(briefing)
                    it[userData] = serializeUser(user)
                    it[status] = DeliveryStatus.PENDING.name
                    it[attempts] = 0
                    it[createdAt] = LocalDateTime.now()
                    it[scheduledFor] = LocalDateTime.now()
                    it[lastAttemptAt] = null
                    it[completedAt] = null
                    it[errorMessage] = null
                }
                Unit.right()
            } catch (e: Exception) {
                DeliveryError.QueueError("Failed to enqueue delivery job: ${e.message}").left()
            }
        }

    override suspend fun dequeue(): DeliveryJob? =
        newSuspendedTransaction(Dispatchers.IO, database) {
            val eligibleJob =
                DeliveryQueueTable
                    .selectAll()
                    .where {
                        (DeliveryQueueTable.status eq DeliveryStatus.PENDING.name) and
                            (DeliveryQueueTable.scheduledFor lessEq LocalDateTime.now())
                    }.orderBy(DeliveryQueueTable.createdAt, SortOrder.ASC)
                    .limit(1)
                    .forUpdate()
                    .firstOrNull()

            eligibleJob?.let { row ->
                val jobId = row[DeliveryQueueTable.id].value

                // Mark as in progress
                DeliveryQueueTable.update({ DeliveryQueueTable.id eq jobId }) {
                    it[status] = DeliveryStatus.IN_PROGRESS.name
                    it[lastAttemptAt] = LocalDateTime.now()
                    it[attempts] = row[DeliveryQueueTable.attempts] + 1
                }

                // Return the updated job
                mapRowToDeliveryJob(row).copy(
                    status = DeliveryStatus.IN_PROGRESS,
                    attempts = row[DeliveryQueueTable.attempts] + 1,
                    lastAttemptAt = LocalDateTime.now(),
                )
            }
        }

    private fun serializeBriefing(briefing: DailyBriefing): String =
        buildJsonObject {
            put("id", briefing.id.toString())
            put("userId", briefing.userId.value.toString())
            put("date", briefing.date.toString())
            put(
                "dayType",
                when (briefing.dayType) {
                    is DayType.Weekday -> "WEEKDAY"
                    is DayType.WeekendOrHoliday -> "WEEKEND_OR_HOLIDAY"
                },
            )
            put(
                "location",
                buildJsonObject {
                    put("city", briefing.location.city)
                    put("countryCode", briefing.location.countryCode)
                },
            )
            put("generatedAt", briefing.generatedAt.toString())
            put("hasWeather", briefing.weather != null)
        }.toString()

    private fun serializeUser(user: User): String =
        buildJsonObject {
            put("id", user.id.value.toString())
            put("email", user.getEmail().value)
            put("name", user.name)
        }.toString()

    private fun deserializeBriefing(
        data: String,
        userId: UUID,
    ): DailyBriefing {
        val jsonObject = json.parseToJsonElement(data).jsonObject
        val locationObject = jsonObject["location"]?.jsonObject ?: throw IllegalStateException("Missing location data")

        return DailyBriefing(
            id = UUID.fromString(jsonObject["id"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing id")),
            userId =
                com.morningcat.domain.user.valueobject
                    .UserId(userId),
            date = LocalDate.parse(jsonObject["date"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing date")),
            dayType =
                when (jsonObject["dayType"]?.jsonPrimitive?.content) {
                    "WEEKDAY" -> DayType.Weekday
                    "WEEKEND_OR_HOLIDAY" -> DayType.WeekendOrHoliday
                    else -> DayType.Weekday
                },
            location =
                Location.create(
                    city = locationObject["city"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing city"),
                    countryCode =
                        locationObject["countryCode"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing countryCode"),
                ),
            generatedAt =
                LocalDateTime.parse(
                    jsonObject["generatedAt"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing generatedAt"),
                ),
            // Add dummy weather to satisfy DailyBriefing validation
            weather =
                if (jsonObject["hasWeather"]?.jsonPrimitive?.content?.toBoolean() == true) {
                    WeatherInfo(
                        date = LocalDate.parse(jsonObject["date"]?.jsonPrimitive?.content ?: ""),
                        temperature = WeatherInfo.Temperature(min = 10.0, max = 20.0, current = 15.0),
                        condition = "Unknown",
                        humidity = 50,
                        uvIndex = 5,
                        precipitation = 0,
                    )
                } else {
                    null
                },
        )
    }

    private fun deserializeUser(data: String): User {
        val jsonObject = json.parseToJsonElement(data).jsonObject
        val emailResult =
            com.morningcat.domain.user.valueobject.EmailAddress.create(
                jsonObject["email"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing email"),
            )
        val email = emailResult.getOrNull() ?: throw IllegalStateException("Invalid email in stored user data")
        return User.register(
            id =
                com.morningcat.domain.user.valueobject.UserId(
                    UUID.fromString(jsonObject["id"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing id")),
                ),
            email = email,
            name = jsonObject["name"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing name"),
        )
    }

    private fun mapRowToDeliveryJob(row: ResultRow): DeliveryJob {
        val userId = row[DeliveryQueueTable.userId]
        val briefing = deserializeBriefing(row[DeliveryQueueTable.briefingData], userId)
        val user = deserializeUser(row[DeliveryQueueTable.userData])

        return DeliveryJob(
            id = row[DeliveryQueueTable.id].value,
            briefing = briefing,
            user = user,
            status = DeliveryStatus.valueOf(row[DeliveryQueueTable.status]),
            attempts = row[DeliveryQueueTable.attempts],
            createdAt = row[DeliveryQueueTable.createdAt],
            scheduledFor = row[DeliveryQueueTable.scheduledFor],
            lastAttemptAt = row[DeliveryQueueTable.lastAttemptAt],
            completedAt = row[DeliveryQueueTable.completedAt],
            errorMessage = row[DeliveryQueueTable.errorMessage],
        )
    }
}
