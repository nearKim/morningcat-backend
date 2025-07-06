package com.morningcat.infrastructure.notification

import arrow.core.left
import arrow.core.right
import com.morningcat.domain.content.aggregate.DailyBriefing
import com.morningcat.domain.content.entity.WeatherInfo
import com.morningcat.domain.notification.error.DeliveryError
import com.morningcat.domain.notification.valueobject.DeliveryStatus
import com.morningcat.domain.schedule.valueobject.DayType
import com.morningcat.domain.user.aggregate.User
import com.morningcat.domain.user.valueobject.Location
import com.morningcat.domain.user.valueobject.EmailAddress
import com.morningcat.domain.user.valueobject.UserId
import com.morningcat.infrastructure.notification.adapter.DbDeliveryQueueAdapter
import com.morningcat.infrastructure.notification.table.DeliveryQueueTable
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.UUID

class DbDeliveryQueueAdapterTest : StringSpec({
    lateinit var database: Database
    lateinit var adapter: DbDeliveryQueueAdapter

    beforeSpec {
        database = Database.connect(
            url = "jdbc:h2:mem:test_delivery_queue;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver",
        )

        transaction(database) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(DeliveryQueueTable)
        }

        adapter = DbDeliveryQueueAdapter(database)
    }

    beforeTest {
        transaction(database) {
            DeliveryQueueTable.deleteAll()
        }
    }

    afterSpec {
        transaction(database) {
            SchemaUtils.drop(DeliveryQueueTable)
        }
    }

    "should enqueue a delivery job successfully" {
        // Given
        val userId = UserId(UUID.randomUUID())
        val emailResult = EmailAddress.create("test@example.com")
        val email = emailResult.getOrNull() ?: throw IllegalStateException("Invalid test email")
        val user = User.register(
            id = userId,
            email = email,
            name = "Test User",
        )
        val briefing = DailyBriefing(
            userId = userId,
            date = LocalDate.now(),
            dayType = DayType.Weekday,
            location = Location.create(city = "Seoul", countryCode = "KR"),
            weather = WeatherInfo(
                date = LocalDate.now(),
                temperature = WeatherInfo.Temperature(min = 10.0, max = 20.0, current = 15.0),
                condition = "Sunny",
                humidity = 50,
                uvIndex = 5,
                precipitation = 0,
            ),
        )

        // When
        val result = runBlocking { adapter.enqueue(briefing, user) }

        // Then
        result.shouldBeRight()
    }

    "should dequeue the oldest pending job" {
        // Given
        val userId = UserId(UUID.randomUUID())
        val emailResult = EmailAddress.create("test@example.com")
        val email = emailResult.getOrNull() ?: throw IllegalStateException("Invalid test email")
        val user = User.register(
            id = userId,
            email = email,
            name = "Test User",
        )
        val briefing1 = DailyBriefing(
            userId = userId,
            date = LocalDate.now(),
            dayType = DayType.Weekday,
            location = Location.create(city = "Seoul", countryCode = "KR"),
            weather = WeatherInfo(
                date = LocalDate.now(),
                temperature = WeatherInfo.Temperature(min = 10.0, max = 20.0, current = 15.0),
                condition = "Sunny",
                humidity = 50,
                uvIndex = 5,
                precipitation = 0,
            ),
        )
        val briefing2 = DailyBriefing(
            userId = userId,
            date = LocalDate.now().plusDays(1),
            dayType = DayType.Weekday,
            location = Location.create(city = "Seoul", countryCode = "KR"),
            weather = WeatherInfo(
                date = LocalDate.now().plusDays(1),
                temperature = WeatherInfo.Temperature(min = 12.0, max = 22.0, current = 17.0),
                condition = "Cloudy",
                humidity = 60,
                uvIndex = 3,
                precipitation = 5,
            ),
        )

        runBlocking {
            adapter.enqueue(briefing1, user)
            Thread.sleep(100) // Ensure different timestamps
            adapter.enqueue(briefing2, user)
        }

        // When
        val dequeuedJob = runBlocking { adapter.dequeue() }

        // Then
        dequeuedJob.shouldNotBeNull()
        dequeuedJob.briefing.date shouldBe briefing1.date
        dequeuedJob.status shouldBe DeliveryStatus.IN_PROGRESS
    }

    "should return null when queue is empty" {
        // When
        val result = runBlocking { adapter.dequeue() }

        // Then
        result.shouldBeNull()
    }

    "should not dequeue jobs that are already in progress" {
        // Given
        val userId = UserId(UUID.randomUUID())
        val emailResult = EmailAddress.create("test@example.com")
        val email = emailResult.getOrNull() ?: throw IllegalStateException("Invalid test email")
        val user = User.register(
            id = userId,
            email = email,
            name = "Test User",
        )
        val briefing = DailyBriefing(
            userId = userId,
            date = LocalDate.now(),
            dayType = DayType.Weekday,
            location = Location.create(city = "Seoul", countryCode = "KR"),
            weather = WeatherInfo(
                date = LocalDate.now(),
                temperature = WeatherInfo.Temperature(min = 10.0, max = 20.0, current = 15.0),
                condition = "Sunny",
                humidity = 50,
                uvIndex = 5,
                precipitation = 0,
            ),
        )

        runBlocking {
            adapter.enqueue(briefing, user)
            adapter.dequeue() // This will mark it as IN_PROGRESS
        }

        // When
        val secondDequeue = runBlocking { adapter.dequeue() }

        // Then
        secondDequeue.shouldBeNull()
    }

})