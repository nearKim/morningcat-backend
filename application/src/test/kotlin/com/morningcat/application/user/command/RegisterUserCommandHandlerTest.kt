package com.morningcat.application.user.command

import arrow.core.right
import com.morningcat.domain.user.aggregate.Subscription
import com.morningcat.domain.user.aggregate.User
import com.morningcat.domain.user.ports.SubscriptionRepository
import com.morningcat.domain.user.ports.UserRepository
import com.morningcat.domain.user.valueobject.EmailAddress
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class RegisterUserCommandHandlerTest : StringSpec({
    
    "RegisterUserCommandHandler should save both User and Subscription when registering a new user" {
        // Given
        val savedUsers = mutableListOf<User>()
        val savedSubscriptions = mutableListOf<Subscription>()
        
        val mockUserRepository = object : UserRepository {
            override suspend fun findById(id: com.morningcat.domain.user.valueobject.UserId): User? = null
            override suspend fun findByEmail(email: EmailAddress): User? = null
            override suspend fun save(user: User) {
                savedUsers.add(user)
            }
        }
        
        val mockSubscriptionRepository = object : SubscriptionRepository {
            override suspend fun findByUserId(userId: com.morningcat.domain.user.valueobject.UserId): Subscription? = null
            override suspend fun findAllScheduledFor(time: java.time.LocalTime): List<Subscription> = emptyList()
            override suspend fun save(subscription: Subscription) {
                savedSubscriptions.add(subscription)
            }
        }
        
        val handler = RegisterUserCommandHandler(mockUserRepository, mockSubscriptionRepository)
        
        val command = RegisterUserCommand(
            email = "test@example.com",
            name = "Test User",
            initialPassword = "SecurePassword123!"
        )
        
        // When
        val result = handler.handle(command)
        
        // Then
        result.shouldBeRight()
        
        // Verify saved user
        savedUsers.size shouldBe 1
        val savedUser = savedUsers.first()
        savedUser.name shouldBe "Test User"
        savedUser.getEmail().value shouldBe "test@example.com"
        
        // Verify saved subscription
        savedSubscriptions.size shouldBe 1
        val savedSubscription = savedSubscriptions.first()
        savedSubscription.userId shouldBe savedUser.id
        savedSubscription.isEnabled() shouldBe false
    }
    
    "RegisterUserCommandHandler should return error when user with email already exists" {
        // Given
        val existingUser = User.register(
            id = com.morningcat.domain.user.valueobject.UserId(java.util.UUID.randomUUID()),
            email = EmailAddress.create("existing@example.com").getOrNull()!!,
            name = "Existing User"
        )
        
        val mockUserRepository = object : UserRepository {
            override suspend fun findById(id: com.morningcat.domain.user.valueobject.UserId): User? = null
            override suspend fun findByEmail(email: EmailAddress): User? = 
                if (email.value == "existing@example.com") existingUser else null
            override suspend fun save(user: User) {
                throw AssertionError("Should not save when user already exists")
            }
        }
        
        val mockSubscriptionRepository = object : SubscriptionRepository {
            override suspend fun findByUserId(userId: com.morningcat.domain.user.valueobject.UserId): Subscription? = null
            override suspend fun findAllScheduledFor(time: java.time.LocalTime): List<Subscription> = emptyList()
            override suspend fun save(subscription: Subscription) {
                throw AssertionError("Should not save subscription when user already exists")
            }
        }
        
        val handler = RegisterUserCommandHandler(mockUserRepository, mockSubscriptionRepository)
        
        val command = RegisterUserCommand(
            email = "existing@example.com",
            name = "New User",
            initialPassword = "SecurePassword123!"
        )
        
        // When
        val result = handler.handle(command)
        
        // Then
        result.shouldBeLeft()
    }
})