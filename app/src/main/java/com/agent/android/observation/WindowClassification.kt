package com.agent.android.observation

enum class WindowClassification {
    APPLICATION,
    LOCAL_AGENT,
    SYSTEM_UI,
    LAUNCHER,
    RECENTS,
    SETTINGS,
    NOTIFICATION_SURFACE,
    QUICK_SETTINGS,
    SYSTEM_DIALOG,
    OVERLAY,
    UNKNOWN;

    companion object {
        fun classify(packageName: String?, className: String? = null, windowType: Int? = null): WindowClassification {
            if (packageName == null || packageName == "UNKNOWN") return UNKNOWN
            if (packageName == "com.agent.android") return LOCAL_AGENT
            if (packageName == "com.android.settings") return SETTINGS
            if (packageName == "com.android.systemui") {
                if (className != null) {
                    if (className.contains("recents", ignoreCase = true)) return RECENTS
                    if (className.contains("notification", ignoreCase = true) || className.contains("shade", ignoreCase = true)) return NOTIFICATION_SURFACE
                    if (className.contains("qs", ignoreCase = true) || className.contains("quicksettings", ignoreCase = true)) return QUICK_SETTINGS
                    if (className.contains("dialog", ignoreCase = true)) return SYSTEM_DIALOG
                }
                return SYSTEM_UI
            }
            if (packageName.contains("launcher", ignoreCase = true) || packageName.contains("home", ignoreCase = true)) return LAUNCHER
            if (packageName.contains("recents", ignoreCase = true)) return RECENTS

            return APPLICATION
        }
    }
}
