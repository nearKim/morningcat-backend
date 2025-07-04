package com.morningcat.domain.user.valueobject

data class Location(
    val city: String,
    val countryCode: String
) {
    init {
        require(city.isNotBlank()) {
            "City cannot be blank"
        }
        require(countryCode.isNotBlank()) {
            "Country code cannot be blank"
        }
        require(countryCode.length == 2) {
            "Country code must be a 2-letter ISO code"
        }
    }
    
    companion object {
        fun create(city: String, countryCode: String): Location {
            return Location(
                city = city.trim(),
                countryCode = countryCode.trim().uppercase()
            )
        }
    }
}