package com.morningcat.infrastructure.persistence.user

import com.morningcat.domain.content.valueobject.ContentCategory
import com.morningcat.domain.notification.valueobject.DeliveryChannelType
import com.morningcat.domain.user.aggregate.Subscription
import com.morningcat.domain.user.ports.SubscriptionRepository
import com.morningcat.domain.user.valueobject.Location
import com.morningcat.domain.user.valueobject.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ExposedSubscriptionRepository(private val database: Database) : SubscriptionRepository {
    
    private val json = Json { ignoreUnknownKeys = true }
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    override suspend fun findByUserId(userId: UserId): Subscription? = 
        dbQuery {
            Subscriptions.selectAll()
                .where { Subscriptions.userId eq userId.value }
                .singleOrNull()
                ?.toSubscription()
        }
    
    override suspend fun findAllScheduledFor(time: LocalTime): List<Subscription> = 
        dbQuery {
            val timeString = time.format(timeFormatter)
            Subscriptions.selectAll()
                .where { 
                    (Subscriptions.deliveryTime eq timeString) and
                    (Subscriptions.isEnabled eq true)
                }
                .map { it.toSubscription() }
        }
    
    override suspend fun save(subscription: Subscription) {
        dbQuery {
            val existingSubscription = Subscriptions.selectAll()
                .where { Subscriptions.userId eq subscription.userId.value }
                .singleOrNull()
            
            if (existingSubscription != null) {
                // Update existing subscription
                Subscriptions.update({ Subscriptions.userId eq subscription.userId.value }) {
                    updateSubscription(it, subscription)
                }
            } else {
                // Insert new subscription
                Subscriptions.insert {
                    it[userId] = subscription.userId.value
                    it[createdAt] = Instant.now()
                    updateSubscription(it, subscription)
                }
            }
        }
    }
    
    private fun updateSubscription(it: UpdateStatement, subscription: Subscription) {
        it[Subscriptions.deliveryTime] = subscription.getDeliveryTime().format(timeFormatter)
        it[Subscriptions.locationCity] = subscription.getLocation().city
        it[Subscriptions.locationCountryCode] = subscription.getLocation().countryCode
        it[Subscriptions.deliveryChannels] = json.encodeToString(
            subscription.getDeliveryChannels().map { channel -> channel::class.simpleName }
        )
        it[Subscriptions.contentPreferences] = json.encodeToString(
            subscription.getContentPreferences().mapKeys { (key, _) -> key.name }
        )
        it[Subscriptions.financialPreferences] = json.encodeToString(
            subscription.getFinancialPreferences().toList()
        )
        it[Subscriptions.isEnabled] = subscription.isEnabled()
        it[Subscriptions.weekendDelivery] = subscription.isWeekendDeliveryEnabled()
        it[Subscriptions.updatedAt] = Instant.now()
    }
    
    private fun updateSubscription(it: InsertStatement<Number>, subscription: Subscription) {
        it[Subscriptions.deliveryTime] = subscription.getDeliveryTime().format(timeFormatter)
        it[Subscriptions.locationCity] = subscription.getLocation().city
        it[Subscriptions.locationCountryCode] = subscription.getLocation().countryCode
        it[Subscriptions.deliveryChannels] = json.encodeToString(
            subscription.getDeliveryChannels().map { channel -> channel::class.simpleName }
        )
        it[Subscriptions.contentPreferences] = json.encodeToString(
            subscription.getContentPreferences().mapKeys { (key, _) -> key.name }
        )
        it[Subscriptions.financialPreferences] = json.encodeToString(
            subscription.getFinancialPreferences().toList()
        )
        it[Subscriptions.isEnabled] = subscription.isEnabled()
        it[Subscriptions.weekendDelivery] = subscription.isWeekendDeliveryEnabled()
        it[Subscriptions.updatedAt] = Instant.now()
    }
    
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
    
    private fun ResultRow.toSubscription(): Subscription {
        val deliveryChannels = json.decodeFromString<List<String>>(this[Subscriptions.deliveryChannels])
            .map { channelName ->
                when (channelName) {
                    "Email" -> DeliveryChannelType.Email
                    "PushNotification" -> DeliveryChannelType.PushNotification
                    else -> throw IllegalStateException("Unknown delivery channel: $channelName")
                }
            }.toSet()
        
        val contentPreferences = json.decodeFromString<Map<String, Boolean>>(this[Subscriptions.contentPreferences])
            .mapKeys { (key, _) ->
                ContentCategory.valueOf(key)
            }
        
        val financialPreferences = json.decodeFromString<List<String>>(this[Subscriptions.financialPreferences])
            .toSet()
        
        return Subscription.create(
            userId = UserId(this[Subscriptions.userId]),
            deliveryTime = LocalTime.parse(this[Subscriptions.deliveryTime], timeFormatter),
            location = Location(
                city = this[Subscriptions.locationCity],
                countryCode = this[Subscriptions.locationCountryCode]
            ),
            deliveryChannels = deliveryChannels,
            contentPreferences = contentPreferences,
            financialPreferences = financialPreferences,
            isEnabled = this[Subscriptions.isEnabled],
            weekendDelivery = this[Subscriptions.weekendDelivery]
        )
    }
}