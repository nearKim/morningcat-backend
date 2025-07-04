package com.morningcat.domain.content.entity

data class EntertainmentRecommendation(
    val title: String,
    val type: EntertainmentType,
    val description: String,
    val duration: Int?, // in minutes, null for articles/books
    val source: String,
    val url: String?
) {
    init {
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(description.isNotBlank()) { "Description cannot be blank" }
        require(source.isNotBlank()) { "Source cannot be blank" }
        duration?.let { require(it > 0) { "Duration must be positive" } }
    }
    
    enum class EntertainmentType {
        MOVIE,
        TV_SHOW,
        BOOK,
        PODCAST,
        ARTICLE,
        VIDEO
    }
}