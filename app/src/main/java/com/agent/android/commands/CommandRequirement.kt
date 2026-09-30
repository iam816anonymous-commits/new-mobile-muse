package com.agent.android.commands

data class CommandRequirement(
    val requiredPermissions: List<String> = emptyList(),
    val requiredSpecialAccess: List<String> = emptyList(),
    val requiredCapabilities: List<String> = emptyList(),
    val requiredSystemServices: List<String> = emptyList(),
    val accessibilityRequired: Boolean = false,
    val physicalObservationRequired: Boolean = false,
    val changesDeviceState: Boolean = false,
    val launchesApplication: Boolean = false,
    val requiresUserInteraction: Boolean = false
)
