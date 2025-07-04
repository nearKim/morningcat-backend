package com.morningcat.domain.shared.error

sealed class DomainError {
    data class InvalidEmail(
        val value: String,
        val reason: String,
    ) : DomainError()

    data class InvalidLocation(
        val reason: String,
    ) : DomainError()

    data class ValidationError(
        val field: String,
        val reason: String,
    ) : DomainError()
}
