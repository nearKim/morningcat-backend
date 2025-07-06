package com.morningcat.application.user.command

import com.morningcat.domain.user.valueobject.FcmToken
import com.morningcat.domain.user.valueobject.UserId

data class RegisterDeviceTokenCommand(
    val userId: UserId,
    val token: String,
    val deviceId: String,
    val deviceName: String?,
    val platform: FcmToken.Platform,
)
