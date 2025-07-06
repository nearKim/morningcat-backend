package com.morningcat.application.user.command

import com.morningcat.application.common.Command
import com.morningcat.application.user.dto.UserSettingsDto

data class UpdateSubscriptionSettingsCommand(
    val userId: String,
    val settingsDto: UserSettingsDto,
) : Command
