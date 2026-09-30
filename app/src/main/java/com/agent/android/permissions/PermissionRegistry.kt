package com.agent.android.permissions

import android.Manifest
import android.provider.Settings

class PermissionRegistry {

    private val permissions: MutableMap<String, PermissionDefinition> = mutableMapOf()

    init {
        registerAllPermissions()
    }

    private fun registerAllPermissions() {
        permissions.clear()

        // 1. RUNTIME PERMISSIONS
        register(PermissionDefinition(
            id = "perm_record_audio",
            androidIdentifier = Manifest.permission.RECORD_AUDIO,
            displayName = "Microphone / Audio Recording",
            category = PermissionCategory.RUNTIME,
            description = "Allows recording audio for speech recognition (STT)",
            protectionType = "DANGEROUS",
            requiresRuntimeRequest = true,
            relatedCapabilityIds = listOf("STT"),
            relatedCommandIds = listOf("stt.listen"),
            settingsAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            explanation = "Required by STT listening to capture user speech"
        ))

        register(PermissionDefinition(
            id = "perm_camera",
            androidIdentifier = Manifest.permission.CAMERA,
            displayName = "Camera Hardware Access",
            category = PermissionCategory.RUNTIME,
            description = "Allows controlling camera flashlight and image capture",
            protectionType = "DANGEROUS",
            requiresRuntimeRequest = true,
            relatedCapabilityIds = listOf("FLASHLIGHT", "CAMERA"),
            relatedCommandIds = listOf("flashlight.on", "flashlight.off", "camera.status", "camera.permission", "camera.list"),
            settingsAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            explanation = "Required for camera and flashlight control"
        ))

        register(PermissionDefinition(
            id = "perm_fine_location",
            androidIdentifier = Manifest.permission.ACCESS_FINE_LOCATION,
            displayName = "Fine Location (GPS)",
            category = PermissionCategory.RUNTIME,
            description = "Allows reading high-accuracy GPS device location",
            protectionType = "DANGEROUS",
            requiresRuntimeRequest = true,
            relatedCapabilityIds = listOf("LOCATION"),
            relatedCommandIds = listOf("location.status", "location.providers"),
            settingsAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            explanation = "Required for location provider queries and GPS data"
        ))

        register(PermissionDefinition(
            id = "perm_coarse_location",
            androidIdentifier = Manifest.permission.ACCESS_COARSE_LOCATION,
            displayName = "Coarse Location (Network)",
            category = PermissionCategory.RUNTIME,
            description = "Allows reading network-based cell/Wi-Fi location",
            protectionType = "DANGEROUS",
            requiresRuntimeRequest = true,
            relatedCapabilityIds = listOf("LOCATION"),
            relatedCommandIds = listOf("location.status", "location.providers"),
            settingsAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            explanation = "Required for network location queries"
        ))

        register(PermissionDefinition(
            id = "perm_read_storage",
            androidIdentifier = Manifest.permission.READ_EXTERNAL_STORAGE,
            displayName = "Read External Storage",
            category = PermissionCategory.RUNTIME,
            description = "Allows reading files and notes from external storage",
            protectionType = "DANGEROUS",
            requiresRuntimeRequest = true,
            relatedCapabilityIds = listOf("FILE_ACCESS"),
            relatedCommandIds = listOf("notes.append"),
            settingsAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            explanation = "Required to read persistent files and notes"
        ))

        register(PermissionDefinition(
            id = "perm_write_storage",
            androidIdentifier = Manifest.permission.WRITE_EXTERNAL_STORAGE,
            displayName = "Write External Storage",
            category = PermissionCategory.RUNTIME,
            description = "Allows persisting notes and evidence files to storage",
            protectionType = "DANGEROUS",
            requiresRuntimeRequest = true,
            relatedCapabilityIds = listOf("FILE_ACCESS"),
            relatedCommandIds = listOf("notes.append"),
            settingsAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            explanation = "Required to write persistent notes and evidence JSON"
        ))

        register(PermissionDefinition(
            id = "perm_read_phone_state",
            androidIdentifier = Manifest.permission.READ_PHONE_STATE,
            displayName = "Read Phone State",
            category = PermissionCategory.RUNTIME,
            description = "Allows reading telephony network state and SIM information",
            protectionType = "DANGEROUS",
            requiresRuntimeRequest = true,
            relatedCapabilityIds = listOf("TELEPHONY"),
            relatedCommandIds = listOf("device.snapshot"),
            settingsAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            explanation = "Required for telephony device status snapshot"
        ))

        register(PermissionDefinition(
            id = "perm_vibrate",
            androidIdentifier = Manifest.permission.VIBRATE,
            displayName = "Vibrate Control",
            category = PermissionCategory.RUNTIME,
            description = "Allows triggering device vibration motor",
            protectionType = "NORMAL",
            requiresRuntimeRequest = false,
            relatedCapabilityIds = listOf("VIBRATION"),
            relatedCommandIds = listOf("haptics.vibrate", "ringer.vibrate"),
            explanation = "Normal permission automatically granted at install time"
        ))

        // 2. SPECIAL ACCESS & SETTINGS PERMISSIONS
        register(PermissionDefinition(
            id = "write_settings_access",
            androidIdentifier = Manifest.permission.WRITE_SETTINGS,
            displayName = "Write System Settings",
            category = PermissionCategory.SYSTEM_SETTING,
            description = "Allows modifying system brightness and screen timeout settings",
            protectionType = "SPECIAL_ACCESS",
            requiresSettingsScreen = true,
            requiresSpecialAccess = true,
            relatedCapabilityIds = listOf("BRIGHTNESS_CONTROL"),
            relatedCommandIds = listOf("brightness.set"),
            settingsAction = Settings.ACTION_MANAGE_WRITE_SETTINGS,
            explanation = "Requires user enablement in Settings -> Write System Settings"
        ))

        register(PermissionDefinition(
            id = "notification_policy_access",
            androidIdentifier = "android.permission.ACCESS_NOTIFICATION_POLICY",
            displayName = "Notification Policy Access (DND)",
            category = PermissionCategory.SPECIAL_ACCESS,
            description = "Allows changing ringer mode between Normal, Vibrate, and Silent",
            protectionType = "SPECIAL_ACCESS",
            requiresSettingsScreen = true,
            requiresSpecialAccess = true,
            relatedCapabilityIds = listOf("RINGER_MODE"),
            relatedCommandIds = listOf("ringer.normal", "ringer.vibrate", "ringer.silent"),
            settingsAction = Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS,
            explanation = "Requires user enablement in Settings -> Do Not Disturb Access"
        ))

        register(PermissionDefinition(
            id = "notification_listener_access",
            androidIdentifier = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
            displayName = "Notification Listener Access",
            category = PermissionCategory.NOTIFICATION_ACCESS,
            description = "Allows reading incoming notification titles and content",
            protectionType = "SPECIAL_ACCESS",
            requiresSettingsScreen = true,
            requiresSpecialAccess = true,
            relatedCapabilityIds = listOf("NOTIFICATION_LISTENER"),
            relatedCommandIds = listOf("notification.status", "notification.latest"),
            settingsAction = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
            explanation = "Requires user enablement in Settings -> Notification Listener Access"
        ))

        register(PermissionDefinition(
            id = "usage_stats_access",
            androidIdentifier = Manifest.permission.PACKAGE_USAGE_STATS,
            displayName = "Usage Stats Access",
            category = PermissionCategory.USAGE_ACCESS,
            description = "Allows querying current active foreground application package",
            protectionType = "SPECIAL_ACCESS",
            requiresSettingsScreen = true,
            requiresSpecialAccess = true,
            relatedCapabilityIds = listOf("USAGE_STATS"),
            relatedCommandIds = listOf("app.current"),
            settingsAction = Settings.ACTION_USAGE_ACCESS_SETTINGS,
            explanation = "Requires user enablement in Settings -> Usage Access"
        ))

        register(PermissionDefinition(
            id = "accessibility_service_required",
            androidIdentifier = "android.permission.BIND_ACCESSIBILITY_SERVICE",
            displayName = "Accessibility Service",
            category = PermissionCategory.ACCESSIBILITY,
            description = "Allows LocalAgent accessibility gesture and observation support",
            protectionType = "SPECIAL_ACCESS",
            requiresSettingsScreen = true,
            requiresSpecialAccess = true,
            relatedCapabilityIds = listOf("ACCESSIBILITY"),
            relatedCommandIds = listOf("accessibility.status", "safety.panic"),
            settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
            explanation = "Requires user enablement in Settings -> Accessibility"
        ))

        // 3. PRIVILEGED / SYSTEM-ONLY
        register(PermissionDefinition(
            id = "device_owner_access",
            androidIdentifier = "android.permission.BIND_DEVICE_ADMIN",
            displayName = "Device Owner / Admin Privileges",
            category = PermissionCategory.PRIVILEGED_ONLY,
            description = "Requires system or device-owner provisioning",
            protectionType = "SIGNATURE_OR_SYSTEM",
            isObtainableOnApi27 = false,
            requiresUserInteraction = false,
            explanation = "System-only privilege not obtainable by standard applications"
        ))
    }

    fun register(definition: PermissionDefinition) {
        permissions[definition.id] = definition
    }

    fun getAllPermissions(): List<PermissionDefinition> = permissions.values.toList()

    fun getPermissionById(id: String): PermissionDefinition? = permissions[id]

    fun getPermissionsByCategory(category: PermissionCategory): List<PermissionDefinition> =
        permissions.values.filter { it.category == category }
}
