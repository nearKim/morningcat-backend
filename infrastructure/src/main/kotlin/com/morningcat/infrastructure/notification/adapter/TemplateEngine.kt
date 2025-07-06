package com.morningcat.infrastructure.notification.adapter

import com.morningcat.domain.content.aggregate.DailyBriefing
import com.morningcat.domain.user.aggregate.User

interface TemplateEngine {
    fun renderDailyBriefing(
        briefing: DailyBriefing,
        user: User,
    ): String
}
