package com.morningcat.infrastructure.notification.config

data class SendGridConfig(
    val apiKey: String,
    val fromEmail: String,
    val fromName: String,
    val replyToEmail: String,
    val sandboxMode: Boolean = false,
) {
    init {
        require(apiKey.isNotBlank()) { "SendGrid API key must not be blank" }
        require(fromEmail.isNotBlank()) { "From email must not be blank" }
        require(fromName.isNotBlank()) { "From name must not be blank" }
    }
}
