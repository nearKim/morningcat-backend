package com.morningcat.domain.content.ports

import arrow.core.Either
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.WeatherInfo
import com.morningcat.domain.user.valueobject.Location

interface WeatherProvider {
    suspend fun fetchWeather(location: Location): Either<ProviderError, WeatherInfo>
}
