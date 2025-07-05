package com.morningcat.application.content.query

import com.morningcat.application.common.Query
import com.morningcat.application.content.dto.DailyBriefingDto

data class PreviewDailyBriefingQuery(
    val userId: String
) : Query<DailyBriefingDto>