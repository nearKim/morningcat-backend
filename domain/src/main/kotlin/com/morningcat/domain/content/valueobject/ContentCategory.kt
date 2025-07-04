package com.morningcat.domain.content.valueobject

sealed class ContentCategory {
    object News : ContentCategory()
    object Weather : ContentCategory()
    object Finance : ContentCategory()
    object Calendar : ContentCategory()
    object SelfImprovement : ContentCategory()
    object Entertainment : ContentCategory()
    
    fun displayName(): String = when (this) {
        News -> "News"
        Weather -> "Weather"
        Finance -> "Finance"
        Calendar -> "Calendar"
        SelfImprovement -> "Self Improvement"
        Entertainment -> "Entertainment"
    }
}