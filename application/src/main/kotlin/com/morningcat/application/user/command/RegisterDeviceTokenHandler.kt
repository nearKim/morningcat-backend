package com.morningcat.application.user.command

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.morningcat.domain.user.error.UserError
import com.morningcat.domain.user.repository.UserRepository
import com.morningcat.domain.user.valueobject.FcmToken

class RegisterDeviceTokenHandler(
    private val userRepository: UserRepository,
) {
    suspend fun handle(command: RegisterDeviceTokenCommand): Either<UserError, Unit> {
        val user = userRepository.findById(command.userId)
            ?: return UserError.UserNotFound(command.userId.value.toString()).left()
        
        val fcmToken = FcmToken(
            token = command.token,
            deviceId = command.deviceId,
            deviceName = command.deviceName,
            platform = command.platform
        )
        
        user.registerDevice(fcmToken)
        
        userRepository.save(user)
        
        return Unit.right()
    }
}