package com.morningcat.domain.user.valueobject

import java.util.UUID

@JvmInline
value class UserId(val value: UUID) {
    override fun toString(): String = value.toString()
    
    companion object {
        fun generate(): UserId = UserId(UUID.randomUUID())
        fun fromString(value: String): UserId = UserId(UUID.fromString(value))
    }
}