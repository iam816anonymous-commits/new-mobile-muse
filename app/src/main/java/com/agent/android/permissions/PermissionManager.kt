package com.agent.android.permissions

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(
    private val context: Context,
    val registry: PermissionRegistry = PermissionRegistry()
) {

    fun refreshStatus(): Map<String, PermissionDefinition> {
        val all = registry.getAllPermissions()
        for (perm in all) {
            perm.currentStatus = checkStatus(perm)
        }
        return all.associateBy { it.id }
    }

    fun checkStatus(perm: PermissionDefinition): PermissionStatus {
        if (!perm.isObtainableOnApi27) {
            return PermissionStatus.PRIVILEGED_ONLY
        }

        return when (perm.category) {
            PermissionCategory.RUNTIME -> {
                if (perm.androidIdentifier.startsWith("android.permission.")) {
                    val granted = ContextCompat.checkSelfPermission(context, perm.androidIdentifier) == PackageManager.PERMISSION_GRANTED
                    if (granted) PermissionStatus.OBTAINED else PermissionStatus.REQUESTABLE
                } else {
                    PermissionStatus.NOT_GRANTED
                }
            }
            PermissionCategory.SYSTEM_SETTING -> {
                if (perm.id == "write_settings_access") {
                    val canWrite = Settings.System.canWrite(context)
                    if (canWrite) PermissionStatus.OBTAINED else PermissionStatus.SETTINGS_REQUIRED
                } else {
                    PermissionStatus.NOT_GRANTED
                }
            }
            PermissionCategory.SPECIAL_ACCESS -> {
                if (perm.id == "notification_policy_access") {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    val granted = notificationManager?.isNotificationPolicyAccessGranted == true
                    if (granted) PermissionStatus.OBTAINED else PermissionStatus.SETTINGS_REQUIRED
                } else {
                    PermissionStatus.NOT_GRANTED
                }
            }
            PermissionCategory.NOTIFICATION_ACCESS -> {
                val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
                val granted = flat.contains(context.packageName)
                if (granted) PermissionStatus.OBTAINED else PermissionStatus.SETTINGS_REQUIRED
            }
            PermissionCategory.USAGE_ACCESS -> {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
                val mode = appOps?.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.packageName)
                val granted = mode == android.app.AppOpsManager.MODE_ALLOWED
                if (granted) PermissionStatus.OBTAINED else PermissionStatus.SETTINGS_REQUIRED
            }
            PermissionCategory.ACCESSIBILITY -> {
                val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
                val granted = flat.contains(context.packageName)
                if (granted) PermissionStatus.OBTAINED else PermissionStatus.SETTINGS_REQUIRED
            }
            PermissionCategory.PRIVILEGED_ONLY -> PermissionStatus.PRIVILEGED_ONLY
            else -> PermissionStatus.NOT_GRANTED
        }
    }

    fun requestRuntimePermission(activity: Activity, perm: PermissionDefinition, requestCode: Int) {
        if (perm.category == PermissionCategory.RUNTIME && perm.requiresRuntimeRequest) {
            ActivityCompat.requestPermissions(activity, arrayOf(perm.androidIdentifier), requestCode)
        }
    }

    fun openSettings(perm: PermissionDefinition): Boolean {
        val action = perm.settingsAction ?: Settings.ACTION_SETTINGS
        val intent = Intent(action).apply {
            if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                data = Uri.fromParts("package", context.packageName, null)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getMissingRequiredAccess(): List<PermissionDefinition> {
        refreshStatus()
        return registry.getAllPermissions().filter {
            it.currentStatus != PermissionStatus.OBTAINED &&
            it.category != PermissionCategory.PRIVILEGED_ONLY
        }
    }

    fun getOptionalAccess(): List<PermissionDefinition> {
        refreshStatus()
        return registry.getAllPermissions().filter {
            it.category == PermissionCategory.RUNTIME && it.currentStatus != PermissionStatus.OBTAINED
        }
    }

    fun getBlockedAccess(): List<PermissionDefinition> {
        refreshStatus()
        return registry.getAllPermissions().filter {
            it.currentStatus == PermissionStatus.SETTINGS_REQUIRED
        }
    }

    fun getUnavailableAccess(): List<PermissionDefinition> {
        refreshStatus()
        return registry.getAllPermissions().filter {
            !it.isObtainableOnApi27 || it.currentStatus == PermissionStatus.PRIVILEGED_ONLY
        }
    }
}
