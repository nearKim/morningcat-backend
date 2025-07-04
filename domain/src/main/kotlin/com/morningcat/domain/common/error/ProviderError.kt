package com.morningcat.domain.common.error

sealed class ProviderError(val message: String) {
    data class NetworkError(val cause: String) : ProviderError("Network error: $cause")
    data class InvalidResponse(val details: String) : ProviderError("Invalid response: $details")
    data class ServiceUnavailable(val service: String) : ProviderError("Service unavailable: $service")
    data class RateLimitExceeded(val retryAfter: Long?) : ProviderError("Rate limit exceeded")
    data class AuthenticationError(val reason: String) : ProviderError("Authentication failed: $reason")
    data class UnknownError(val cause: String) : ProviderError("Unknown error: $cause")
}