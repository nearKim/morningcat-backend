package com.morningcat.infrastructure.notification.config

import com.sendgrid.SendGrid

object SendGridFactory {
    fun createSendGrid(config: SendGridConfig): SendGrid = SendGrid(config.apiKey)
}
