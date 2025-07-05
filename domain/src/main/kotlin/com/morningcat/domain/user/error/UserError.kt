package com.morningcat.domain.user.error

sealed class UserError {
    data class InvalidEmail(val email: String, val reason: String) : UserError()
    data class EmailAlreadyExists(val email: String) : UserError()
    data class UserNotFound(val userId: String) : UserError()
    data class ValidationFailed(val message: String) : UserError()
}