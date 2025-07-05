package com.morningcat.application.user.command

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.morningcat.domain.user.aggregate.Subscription
import com.morningcat.domain.user.aggregate.User
import com.morningcat.domain.user.error.UserError
import com.morningcat.domain.user.ports.SubscriptionRepository
import com.morningcat.domain.user.ports.UserRepository
import com.morningcat.domain.user.valueobject.EmailAddress
import com.morningcat.domain.user.valueobject.Location
import com.morningcat.domain.user.valueobject.UserId
import java.util.UUID

class RegisterUserCommandHandler(
    private val userRepository: UserRepository,
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend fun handle(command: RegisterUserCommand): Either<UserError, Unit> {
        // Parse and validate email
        val emailResult = EmailAddress.create(command.email)
        val email = when (emailResult) {
            is Either.Left -> return UserError.InvalidEmail(command.email, emailResult.value.reason).left()
            is Either.Right -> emailResult.value
        }
        
        // Check if user already exists
        val existingUser = userRepository.findByEmail(email)
        if (existingUser != null) {
            return UserError.EmailAlreadyExists(command.email).left()
        }
        
        // Create new user
        val userId = UserId(UUID.randomUUID())
        val user = User.register(
            id = userId,
            email = email,
            name = command.name
        )
        
        // Create default subscription for the user with default location
        val defaultLocation = Location(city = "Seoul", countryCode = "KR")
        val subscription = Subscription.create(
            userId = userId,
            location = defaultLocation,
            isEnabled = false // Start disabled until user configures preferences
        )
        
        // Save both user and subscription
        userRepository.save(user)
        subscriptionRepository.save(subscription)
        
        return Unit.right()
    }
}