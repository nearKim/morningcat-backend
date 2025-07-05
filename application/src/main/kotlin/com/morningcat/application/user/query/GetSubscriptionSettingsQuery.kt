package com.morningcat.application.user.query

import com.morningcat.application.common.Query
import com.morningcat.application.user.dto.UserSettingsDto

data class GetSubscriptionSettingsQuery(
    val userId: String
) : Query<UserSettingsDto>