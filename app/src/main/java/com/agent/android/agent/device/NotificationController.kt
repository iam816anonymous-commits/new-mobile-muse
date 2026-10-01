package com.agent.android.agent.device

import android.content.Context
import android.provider.Settings
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.service.LocalAgentNotificationListenerService

class NotificationController(private val context: Context?) {

    fun isNotificationListenerGranted(): Boolean {
        if (context == null) return false
        val pkg = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkg)
    }

    fun getNotificationStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("NOTIFICATION", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val isGranted = isNotificationListenerGranted()
        val isConnected = LocalAgentNotificationListenerService.isConnected

        if (!isGranted) {
            return SkillResult("NOTIFICATION", SkillStatus.PERMISSION_REQUIRED, "Notification Listener Access not granted in Android Settings", System.currentTimeMillis() - start, "NOTIFICATION_LISTENER_ACCESS_REQUIRED")
        }

        val msg = "NOTIFICATION_LISTENER_STATUS: GRANTED | Connected: $isConnected"
        return SkillResult("NOTIFICATION", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }

    fun getLatestNotification(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("NOTIFICATION", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        if (!isNotificationListenerGranted()) {
            return SkillResult("NOTIFICATION", SkillStatus.PERMISSION_REQUIRED, "Notification Listener Access required", System.currentTimeMillis() - start, "NOTIFICATION_LISTENER_ACCESS_REQUIRED")
        }

        val latest = LocalAgentNotificationListenerService.latestNotification
        if (latest == null) {
            return SkillResult("NOTIFICATION", SkillStatus.SUCCESS, "No recent notifications received", System.currentTimeMillis() - start)
        }

        val msg = "Latest Notification: [Pkg: ${latest.packageName}] Title: '${latest.title}' Text: '${latest.text}'"
        return SkillResult("NOTIFICATION", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }
}
