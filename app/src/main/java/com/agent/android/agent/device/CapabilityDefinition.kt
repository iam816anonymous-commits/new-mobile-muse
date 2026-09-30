package com.agent.android.agent.device

data class CapabilityDefinition(
    val capabilityId: String,
    val name: String,
    val category: String,
    val description: String,
    val classification: CapabilityClassification,
    val capabilityExists: Boolean,
    val capabilityPermitted: Boolean,
    val capabilityUsable: Boolean,
    val requiredPermissions: List<String> = emptyList(),
    val requiredSpecialAccess: List<String> = emptyList(),
    val settingsAction: String? = null,
    val physicalVerificationRequired: Boolean = false,
    val knownRestrictions: String? = null
)
