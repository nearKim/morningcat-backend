package com.morningcat.domain.user.aggregate

import com.morningcat.domain.user.valueobject.EmailAddress
import com.morningcat.domain.user.valueobject.UserId

class User private constructor(
    val id: UserId,
    private var email: EmailAddress,
    val name: String
) {
    init {
        require(name.isNotBlank()) { "User name cannot be blank" }
    }
    
    fun getEmail(): EmailAddress = email
    
    fun changeEmail(newEmail: EmailAddress) {
        require(newEmail != email) { "New email must be different from current email" }
        email = newEmail
    }
    
    companion object {
        fun register(
            id: UserId,
            email: EmailAddress,
            name: String
        ): User {
            return User(
                id = id,
                email = email,
                name = name.trim()
            )
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
}