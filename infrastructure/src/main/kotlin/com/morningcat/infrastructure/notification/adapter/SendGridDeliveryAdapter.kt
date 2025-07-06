package com.morningcat.infrastructure.notification.adapter

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.morningcat.domain.content.aggregate.DailyBriefing
import com.morningcat.domain.notification.error.DeliveryError
import com.morningcat.domain.notification.ports.DeliveryPort
import com.morningcat.domain.notification.valueobject.DeliveryChannelType
import com.morningcat.domain.user.aggregate.User
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SendGridDeliveryAdapter(
    private val sendGrid: SendGrid,
    private val templateEngine: TemplateEngine,
) : DeliveryPort {
    companion object {
        private const val FROM_NAME = "MorningCat"
        private const val FROM_EMAIL = "noreply@morningcat.app"
        private const val REPLY_TO_EMAIL = "support@morningcat.app"
        private const val SUBJECT_PREFIX = "Your MorningCat Daily Briefing"
        private const val CONTENT_TYPE_HTML = "text/html"
        private const val SEND_ENDPOINT = "mail/send"
        private const val SUCCESS_STATUS_CODE = 202
    }

    override suspend fun send(
        briefing: DailyBriefing,
        user: User,
        channel: DeliveryChannelType,
    ): Either<DeliveryError, Unit> =
        withContext(Dispatchers.IO) {
            when (channel) {
                is DeliveryChannelType.Email -> sendEmail(briefing, user)
                is DeliveryChannelType.PushNotification ->
                    DeliveryError.ChannelNotConfigured(channel.displayName()).left()
            }
        }

    private suspend fun sendEmail(
        briefing: DailyBriefing,
        user: User,
    ): Either<DeliveryError, Unit> =
        try {
            val htmlContent = templateEngine.renderDailyBriefing(briefing, user)

            val from = Email(FROM_EMAIL, FROM_NAME)
            val to = Email(user.getEmail().value, user.name)
            val subject = buildSubject(briefing.date)
            val content = Content(CONTENT_TYPE_HTML, htmlContent)

            val mail =
                Mail(from, subject, to, content).apply {
                    setReplyTo(Email(REPLY_TO_EMAIL))
                }

            val request =
                Request().apply {
                    method = Method.POST
                    endpoint = SEND_ENDPOINT
                    body = mail.build()
                }

            val response = sendGrid.api(request)

            if (response.statusCode == SUCCESS_STATUS_CODE) {
                Unit.right()
            } else {
                DeliveryError
                    .DeliveryFailed(
                        "SendGrid returned status ${response.statusCode}: ${response.body}",
                    ).left()
            }
        } catch (e: Exception) {
            when (e) {
                is RuntimeException ->
                    DeliveryError
                        .TemplateError(
                            e.message ?: "Template rendering failed",
                        ).left()
                else ->
                    DeliveryError
                        .DeliveryFailed(
                            e.message ?: "Email sending failed",
                        ).left()
            }
        }

    private fun buildSubject(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return "$SUBJECT_PREFIX - ${date.format(formatter)}"
    }
}
