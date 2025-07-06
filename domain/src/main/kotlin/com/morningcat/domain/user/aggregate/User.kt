package com.morningcat.domain.user.aggregate

import com.morningcat.domain.user.valueobject.EmailAddress
import com.morningcat.domain.user.valueobject.FcmToken
import com.morningcat.domain.user.valueobject.UserId

class User private constructor(
    val id: UserId,
    private var email: EmailAddress,
    val name: String,
    private val fcmTokens: MutableSet<FcmToken> = mutableSetOf(),
) {
    init {
        require(name.isNotBlank()) { "User name cannot be blank" }
    }

    fun getEmail(): EmailAddress = email

    fun changeEmail(newEmail: EmailAddress) {
        require(newEmail != email) { "New email must be different from current email" }
        email = newEmail
    }

    fun registerDevice(token: FcmToken) {
        // Remove any existing token for the same device
        fcmTokens.removeIf { it.deviceId == token.deviceId }
        fcmTokens.add(token)
    }

    fun unregisterDevice(deviceId: String) {
        fcmTokens.removeIf { it.deviceId == deviceId }
    }

    fun getActiveTokens(): Set<FcmToken> {
        // Remove expired tokens
        fcmTokens.removeIf { it.isExpired() }
        return fcmTokens.toSet()
    }

    fun hasActiveTokens(): Boolean = getActiveTokens().isNotEmpty()

    fun updateTokenUsage(token: String) {
        fcmTokens.find { it.token == token }?.let { existingToken ->
            fcmTokens.remove(existingToken)
            fcmTokens.add(existingToken.updateLastUsed())
        }
    }

    companion object {
        const val MAX_DEVICES_PER_USER = 10

        fun register(
            id: UserId,
            email: EmailAddress,
            name: String,
        ): User =
            User(
                id = id,
                email = email,
                name = name.trim(),
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
