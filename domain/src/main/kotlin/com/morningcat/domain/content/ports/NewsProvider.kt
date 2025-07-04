package com.morningcat.domain.content.ports

import arrow.core.Either
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.NewsArticle

interface NewsProvider {
    suspend fun fetchNews(countryCode: String): Either<ProviderError, List<NewsArticle>>
}