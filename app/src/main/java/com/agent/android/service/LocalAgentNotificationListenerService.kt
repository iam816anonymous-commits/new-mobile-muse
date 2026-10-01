package com.agent.android.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

data class NotificationSnapshot(
    val packageName: String,
    val title: String,
    val text: String,
    val postTimeMs: Long
)

class LocalAgentNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "LocalAgentNotifService"
        @Volatile
        var isConnected = false
            private set

        @Volatile
        var latestNotification: NotificationSnapshot? = null
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.i(TAG, "NotificationListenerService connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.i(TAG, "NotificationListenerService disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        try {
            val pkg = sbn.packageName ?: ""
            val extras = sbn.notification?.extras
            val title = extras?.getCharSequence("android.title")?.toString() ?: ""
            val text = extras?.getCharSequence("android.text")?.toString() ?: ""
            latestNotification = NotificationSnapshot(pkg, title, text, sbn.postTime)
        } catch (ignored: Exception) {}
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
