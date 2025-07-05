package com.morningcat.application.user.command

import com.morningcat.application.user.dto.UserSettingsDto
import com.morningcat.domain.user.valueobject.UserId

data class UpdateSubscriptionSettingsCommand(
    val userId: UserId,
    val settings: UserSettingsDto
)