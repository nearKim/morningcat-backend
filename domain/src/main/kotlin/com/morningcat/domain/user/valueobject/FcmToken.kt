package com.morningcat.domain.user.valueobject

import java.time.LocalDateTime

data class FcmToken(
    val token: String,
    val deviceId: String,
    val deviceName: String? = null,
    val platform: Platform,
    val registeredAt: LocalDateTime = LocalDateTime.now(),
    val lastUsedAt: LocalDateTime = LocalDateTime.now(),
) {
    init {
        require(token.isNotBlank()) { "FCM token cannot be blank" }
        require(deviceId.isNotBlank()) { "Device ID cannot be blank" }
        require(token.length in MIN_TOKEN_LENGTH..MAX_TOKEN_LENGTH) { 
            "FCM token length must be between $MIN_TOKEN_LENGTH and $MAX_TOKEN_LENGTH characters" 
        }
    }
    
    fun updateLastUsed(): FcmToken = copy(lastUsedAt = LocalDateTime.now())
    
    fun isExpired(): Boolean {
        // FCM tokens typically expire after 60 days of inactivity
        return lastUsedAt.isBefore(LocalDateTime.now().minusDays(TOKEN_EXPIRY_DAYS))
    }
    
    enum class Platform {
        ANDROID,
        IOS,
        WEB
    }
    
    companion object {
        private const val MIN_TOKEN_LENGTH = 100
        private const val MAX_TOKEN_LENGTH = 200
        private const val TOKEN_EXPIRY_DAYS = 60L
    }
}