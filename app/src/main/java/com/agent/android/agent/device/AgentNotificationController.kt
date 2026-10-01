package com.agent.android.agent.device

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class AgentNotificationController(private val context: Context?) {

    companion object {
        const val CHANNEL_ID = "localagent_status_channel"
        const val CHANNEL_NAME = "LocalAgent Agent Status"
    }

    @SuppressLint("NotificationPermission")
    fun postAgentNotification(title: String, body: String): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("AGENT_NOTIFICATION", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return SkillResult("AGENT_NOTIFICATION", SkillStatus.UNAVAILABLE, "NotificationManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
                nm.createNotificationChannel(channel)
            }

            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            nm.notify(1001, notif)
            SkillResult("AGENT_NOTIFICATION", SkillStatus.SUCCESS, "Posted user-facing notification: '$title'", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("AGENT_NOTIFICATION", SkillStatus.FAILED, "Error posting notification: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }
}
