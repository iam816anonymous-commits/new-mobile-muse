package com.agent.android.agent.device

import android.content.Context
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.agent.skills.app.AppLauncherImpl

class AppDiscoveryController(private val context: Context?) {

    private val appLauncher = AppLauncherImpl(context)

    fun listApps(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("APP_DISCOVERY", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val apps = appLauncher.listInstalledLaunchableApps()
        val names = apps.take(10).joinToString(", ") { it.applicationLabel }
        val msg = "Installed Launchable Apps (${apps.size} total): $names..."
        return SkillResult("APP_DISCOVERY", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }

    fun findApp(query: String): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("APP_DISCOVERY", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val matches = appLauncher.findApp(query)
        val msg = "Find '$query': ${matches.size} matches -> ${matches.joinToString(", ") { "${it.applicationLabel} [${it.packageName}]" }}"
        return SkillResult("APP_DISCOVERY", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }

    fun getAppInfo(packageName: String): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("APP_DISCOVERY", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(packageName, 0)
            val msg = "App Info: ${info.packageName} (Ver: ${info.versionName}, Code: ${info.versionCode})"
            SkillResult("APP_DISCOVERY", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("APP_DISCOVERY", SkillStatus.FAILED, "App info not found for '$packageName': ${e.message}", System.currentTimeMillis() - start, "APP_NOT_FOUND")
        }
    }
}
