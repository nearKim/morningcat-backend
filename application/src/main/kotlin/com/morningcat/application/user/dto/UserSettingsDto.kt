package com.morningcat.application.user.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserSettingsDto(
    val general: GeneralPreferencesDto,
    val delivery: DeliveryPreferencesDto,
    val content: ContentPreferencesDto,
    val personalization: PersonalizationPreferencesDto,
)

@Serializable
data class GeneralPreferencesDto(
    val isServiceEnabled: Boolean,
    val preferredLocation: LocationDto,
)

@Serializable
data class LocationDto(
    val city: String,
    val countryCode: String,
)

@Serializable
data class DeliveryPreferencesDto(
    val deliveryTime: String,
    val receiveOnWeekends: Boolean,
    val emailEnabled: Boolean,
    val pushNotificationEnabled: Boolean,
)

@Serializable
data class ContentPreferencesDto(
    val includeNews: Boolean,
    val includeEconomicIndicators: Boolean,
    val includeWeather: Boolean,
    val includeSchedule: Boolean,
    val includeSelfImprovement: Boolean,
    val includeEntertainment: Boolean,
)

@Serializable
data class PersonalizationPreferencesDto(
    val financialInstruments: Set<String>,
)
