package com.morningcat.domain.content.entity

data class SelfImprovementTip(
    val title: String,
    val content: String,
    val category: TipCategory,
    val estimatedReadTime: Int // in minutes
) {
    init {
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(content.isNotBlank()) { "Content cannot be blank" }
        require(estimatedReadTime > 0) { "Estimated read time must be positive" }
    }
    
    enum class TipCategory {
        PRODUCTIVITY,
        MINDFULNESS,
        HEALTH,
        LEARNING,
        MOTIVATION
    }
}