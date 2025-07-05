package com.morningcat.domain.user.ports

import com.morningcat.domain.user.aggregate.User
import com.morningcat.domain.user.valueobject.EmailAddress
import com.morningcat.domain.user.valueobject.UserId

interface UserRepository {
    suspend fun findById(id: UserId): User?

    suspend fun findByEmail(email: EmailAddress): User?

    suspend fun save(user: User)
}
