package com.morningcat.infrastructure.notification.adapter

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.morningcat.domain.content.aggregate.DailyBriefing
import com.morningcat.domain.notification.error.DeliveryError
import com.morningcat.domain.notification.ports.DeliveryPort
import com.morningcat.domain.notification.valueobject.DeliveryChannelType
import com.morningcat.domain.user.aggregate.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FcmDeliveryAdapter(
    private val firebaseMessaging: FirebaseMessaging,
) : DeliveryPort {
    
    companion object {
        private const val NOTIFICATION_TITLE = "Your MorningCat Daily Briefing"
        private const val WEATHER_EMOJI = "🌤"
        private const val NEWS_EMOJI = "📰"
        private const val DEGREE_SYMBOL = "°C"
    }

    override suspend fun send(
        briefing: DailyBriefing,
        user: User,
        channel: DeliveryChannelType,
    ): Either<DeliveryError, Unit> = withContext(Dispatchers.IO) {
        when (channel) {
            is DeliveryChannelType.PushNotification -> sendPushNotification(briefing, user)
            is DeliveryChannelType.Email -> 
                DeliveryError.ChannelNotConfigured(channel.displayName()).left()
        }
    }

    private suspend fun sendPushNotification(
        briefing: DailyBriefing,
        user: User,
    ): Either<DeliveryError, Unit> {
        val activeTokens = user.getActiveTokens()
        if (activeTokens.isEmpty()) {
            return DeliveryError.DeviceTokenNotFound.left()
        }
        
        // Send to all active devices
        val results = activeTokens.map { fcmToken ->
            try {
                val message = buildMessage(briefing, fcmToken.token)
                firebaseMessaging.send(message)
                user.updateTokenUsage(fcmToken.token)
                true
            } catch (e: Exception) {
                // Log the error but continue with other devices
                false
            }
        }
        
        return if (results.any { it }) {
            Unit.right()
        } else {
            DeliveryError.DeliveryFailed(
                "Failed to send push notification to any device"
            ).left()
        }
    }

    private fun buildMessage(briefing: DailyBriefing, token: String): Message {
        val notification = Notification.builder()
            .setTitle(NOTIFICATION_TITLE)
            .setBody(buildNotificationBody(briefing))
            .build()

        return Message.builder()
            .setToken(token)
            .setNotification(notification)
            .putAllData(buildDataPayload(briefing))
            .build()
    }

    private fun buildNotificationBody(briefing: DailyBriefing): String {
        val weatherText = briefing.weather?.let { weather ->
            "$WEATHER_EMOJI ${weather.condition}, ${weather.temperature.current}$DEGREE_SYMBOL"
        } ?: ""
        
        val newsText = if (briefing.news.isNotEmpty()) {
            "$NEWS_EMOJI ${briefing.news.size} news article${if (briefing.news.size != 1) "s" else ""}"
        } else ""
        
        return when {
            weatherText.isNotEmpty() && newsText.isNotEmpty() -> "$weatherText | $newsText"
            weatherText.isNotEmpty() -> weatherText
            newsText.isNotEmpty() -> newsText
            else -> "Your daily briefing is ready!"
        }
    }

    private fun buildDataPayload(briefing: DailyBriefing): Map<String, String> {
        val data = mutableMapOf(
            "briefingDate" to briefing.date.toString(),
            "newsCount" to briefing.news.size.toString()
        )
        
        briefing.weather?.let { weather ->
            data["weatherCondition"] = weather.condition
            data["temperature"] = weather.temperature.current.toString()
        }
        
        return data
    }

}