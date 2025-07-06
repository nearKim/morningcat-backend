package com.morningcat.application.content.command

import com.morningcat.application.common.Command

data class GenerateAndEnqueueBriefingCommand(
    val subscriptionId: String,
) : Command
