package com.agent.android.agent.device

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

data class SettingsActionDefinition(
    val actionId: String,
    val name: String,
    val intentAction: String,
    val description: String
)

class SettingsActionRegistry(private val context: Context?) {

    private val actions: MutableMap<String, SettingsActionDefinition> = mutableMapOf()

    init {
        registerDefaultActions()
    }

    private fun registerDefaultActions() {
        actions["settings.accessibility"] = SettingsActionDefinition("settings.accessibility", "Accessibility Settings", Settings.ACTION_ACCESSIBILITY_SETTINGS, "Opens Android Accessibility Settings")
        actions["settings.display"] = SettingsActionDefinition("settings.display", "Display Settings", Settings.ACTION_DISPLAY_SETTINGS, "Opens Android Display Settings")
        actions["settings.sound"] = SettingsActionDefinition("settings.sound", "Sound Settings", Settings.ACTION_SOUND_SETTINGS, "Opens Android Sound Settings")
        actions["settings.wifi"] = SettingsActionDefinition("settings.wifi", "Wi-Fi Settings", Settings.ACTION_WIFI_SETTINGS, "Opens Android Wi-Fi Settings")
        actions["settings.bluetooth"] = SettingsActionDefinition("settings.bluetooth", "Bluetooth Settings", Settings.ACTION_BLUETOOTH_SETTINGS, "Opens Android Bluetooth Settings")
        actions["settings.location"] = SettingsActionDefinition("settings.location", "Location Settings", Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Opens Android Location Settings")
        actions["settings.battery"] = SettingsActionDefinition("settings.battery", "Battery Saver Settings", Settings.ACTION_BATTERY_SAVER_SETTINGS, "Opens Android Battery Saver Settings")
        actions["settings.apps"] = SettingsActionDefinition("settings.apps", "Application Settings", Settings.ACTION_APPLICATION_SETTINGS, "Opens Android Applications List")
        actions["settings.storage"] = SettingsActionDefinition("settings.storage", "Storage Settings", Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "Opens Android Internal Storage Settings")
        actions["settings.security"] = SettingsActionDefinition("settings.security", "Security Settings", Settings.ACTION_SECURITY_SETTINGS, "Opens Android Security Settings")
        actions["settings.date_time"] = SettingsActionDefinition("settings.date_time", "Date & Time Settings", Settings.ACTION_DATE_SETTINGS, "Opens Android Date & Time Settings")
        actions["settings.input"] = SettingsActionDefinition("settings.input", "Language & Input Settings", Settings.ACTION_INPUT_METHOD_SETTINGS, "Opens Android Input Settings")
        actions["settings.developer"] = SettingsActionDefinition("settings.developer", "Developer Options", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, "Opens Android Developer Options")
    }

    fun getAllActions(): List<SettingsActionDefinition> = actions.values.toList()

    fun launchSettingsAction(actionId: String): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("SETTINGS_ACTION", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val actionDef = actions[actionId]
            ?: return SkillResult("SETTINGS_ACTION", SkillStatus.FAILED, "Unknown settings action '$actionId'", System.currentTimeMillis() - start, "INVALID_ACTION")

        return try {
            val intent = Intent(actionDef.intentAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pm = context.packageManager
            val resolveInfo = pm.resolveActivity(intent, 0)

            if (resolveInfo == null) {
                SkillResult("SETTINGS_ACTION", SkillStatus.UNAVAILABLE, "Settings activity unavailable on this device for ${actionDef.name}", System.currentTimeMillis() - start, "SETTINGS_UNAVAILABLE")
            } else {
                context.startActivity(intent)
                SkillResult("SETTINGS_ACTION", SkillStatus.SUCCESS, "Opened ${actionDef.name} [Pkg: ${resolveInfo.activityInfo.packageName}]", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            SkillResult("SETTINGS_ACTION", SkillStatus.FAILED, "Error launching ${actionDef.name}: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }
}
