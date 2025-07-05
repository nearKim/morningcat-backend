package com.morningcat.domain.content.ports

import arrow.core.Either
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.Task
import com.morningcat.domain.user.aggregate.User
import java.time.LocalDate

interface TaskProvider {
    suspend fun getTasks(
        user: User,
        date: LocalDate,
    ): Either<ProviderError, List<Task>>
}
