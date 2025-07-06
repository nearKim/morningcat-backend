package com.morningcat.presentation.api

import arrow.core.fold
import com.morningcat.application.user.command.RegisterDeviceTokenCommand
import com.morningcat.application.user.command.RegisterDeviceTokenHandler
import com.morningcat.domain.user.valueobject.FcmToken
import com.morningcat.domain.user.valueobject.UserId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.util.UUID

@Serializable
data class RegisterDeviceTokenRequest(
    val token: String,
    val deviceId: String,
    val deviceName: String? = null,
    val platform: String, // "android", "ios", or "web"
)

@Serializable
data class RegisterDeviceTokenResponse(
    val success: Boolean,
    val message: String,
)

fun Route.deviceTokenRoutes() {
    val handler by inject<RegisterDeviceTokenHandler>()
    
    route("/api/v1/users/{userId}/devices") {
        post("/register") {
            val userId = call.parameters["userId"] 
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing user ID")
                
            val request = call.receive<RegisterDeviceTokenRequest>()
            
            val platform = when (request.platform.lowercase()) {
                "android" -> FcmToken.Platform.ANDROID
                "ios" -> FcmToken.Platform.IOS
                "web" -> FcmToken.Platform.WEB
                else -> return@post call.respond(
                    HttpStatusCode.BadRequest, 
                    "Invalid platform. Must be 'android', 'ios', or 'web'"
                )
            }
            
            val command = RegisterDeviceTokenCommand(
                userId = UserId(UUID.fromString(userId)),
                token = request.token,
                deviceId = request.deviceId,
                deviceName = request.deviceName,
                platform = platform
            )
            
            handler.handle(command).fold(
                { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        RegisterDeviceTokenResponse(
                            success = false,
                            message = error.toString()
                        )
                    )
                },
                {
                    call.respond(
                        HttpStatusCode.OK,
                        RegisterDeviceTokenResponse(
                            success = true,
                            message = "Device token registered successfully"
                        )
                    )
                }
            )
        }
        
        delete("/{deviceId}") {
            // TODO: Implement device unregistration
            call.respond(HttpStatusCode.NotImplemented)
        }
    }
}