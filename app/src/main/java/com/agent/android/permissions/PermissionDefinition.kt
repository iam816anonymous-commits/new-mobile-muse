package com.agent.android.permissions

data class PermissionDefinition(
    val id: String,
    val androidIdentifier: String,
    val displayName: String,
    val category: PermissionCategory,
    val description: String,
    val protectionType: String = "NORMAL",
    val isManifestDeclarable: Boolean = true,
    val requiresRuntimeRequest: Boolean = false,
    val requiresSettingsScreen: Boolean = false,
    val requiresSpecialAccess: Boolean = false,
    val isObtainableOnApi27: Boolean = true,
    val relatedCapabilityIds: List<String> = emptyList(),
    val relatedCommandIds: List<String> = emptyList(),
    var currentStatus: PermissionStatus = PermissionStatus.UNKNOWN,
    val settingsAction: String? = null,
    val explanation: String = "",
    val requiresUserInteraction: Boolean = true
)
