package com.morningcat.application.user.command

import com.morningcat.application.common.Command

data class RegisterUserCommand(
    val email: String,
    val name: String,
    val initialPassword: String,
) : Command
