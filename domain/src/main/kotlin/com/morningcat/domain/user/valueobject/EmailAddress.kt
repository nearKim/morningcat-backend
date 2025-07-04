package com.morningcat.domain.user.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.morningcat.domain.shared.error.DomainError

@JvmInline
value class EmailAddress private constructor(
    val value: String,
) {
    init {
        require(EMAIL_REGEX.matches(value)) {
            "Invalid email format: $value"
        }
    }

    companion object {
        private val EMAIL_REGEX = """^[A-Za-z0-9+_.-]+@([A-Za-z0-9]+([.-][A-Za-z0-9]+)*\.[A-Za-z]{2,})$""".toRegex()

        fun create(value: String): Either<DomainError.InvalidEmail, EmailAddress> {
            val trimmedValue = value.trim()

            return when {
                trimmedValue.isEmpty() ->
                    DomainError.InvalidEmail(value, "Email address cannot be empty").left()
                !EMAIL_REGEX.matches(trimmedValue) ->
                    DomainError.InvalidEmail(value, "Invalid email format").left()
                else -> EmailAddress(trimmedValue).right()
            }
        }
    }
}
