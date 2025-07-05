package com.morningcat.domain.schedule.ports

import arrow.core.Either
import com.morningcat.domain.common.error.ProviderError
import java.time.LocalDate

interface HolidayProvider {
    suspend fun getHolidays(
        year: Int,
        countryCode: String,
    ): Either<ProviderError, List<LocalDate>>
}
