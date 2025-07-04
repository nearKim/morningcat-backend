package com.morningcat.domain.content.entity

data class NewsArticle(
    val headline: String,
    val summary: String,
    val url: String
) {
    init {
        require(headline.isNotBlank()) { "Headline cannot be blank" }
        require(summary.isNotBlank()) { "Summary cannot be blank" }
        require(url.isNotBlank()) { "URL cannot be blank" }
    }
}