package com.agent.android.agent.skills.app

data class AppDescriptor(
    val packageName: String,
    val applicationLabel: String,
    val launchable: Boolean,
    val launcherActivity: String?
)

enum class AppLaunchStatus {
    SUCCESS,
    APP_NOT_FOUND,
    APP_NOT_INSTALLED,
    APP_NOT_LAUNCHABLE,
    AMBIGUOUS_APP,
    ACTIVITY_UNAVAILABLE,
    LAUNCH_FAILED,
    INVALID_ARGUMENT,
    CANCELLED,
    UNSUPPORTED,
    UNAVAILABLE
}

data class AppLaunchResult(
    val requestedApp: String,
    val resolvedPackage: String?,
    val resolvedLabel: String?,
    val launcherActivity: String?,
    val status: AppLaunchStatus,
    val errorCode: String? = null,
    val message: String,
    val durationMs: Long
)

interface AppLauncher {
    fun listInstalledLaunchableApps(): List<AppDescriptor>
    fun findApp(query: String): List<AppDescriptor>
    fun validateApp(query: String): AppLaunchResult
    fun launchApp(query: String): AppLaunchResult
}
