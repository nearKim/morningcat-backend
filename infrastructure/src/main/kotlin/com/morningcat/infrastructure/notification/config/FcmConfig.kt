package com.morningcat.infrastructure.notification.config

data class FcmConfig(
    val projectId: String,
    val privateKeyId: String,
    val privateKey: String,
    val clientEmail: String,
    val clientId: String,
    val dryRun: Boolean = false,
) {
    init {
        require(projectId.isNotBlank()) { "FCM project ID must not be blank" }
        require(privateKeyId.isNotBlank()) { "FCM private key ID must not be blank" }
        require(privateKey.isNotBlank()) { "FCM private key must not be blank" }
        require(clientEmail.isNotBlank()) { "FCM client email must not be blank" }
        require(clientId.isNotBlank()) { "FCM client ID must not be blank" }
    }
}