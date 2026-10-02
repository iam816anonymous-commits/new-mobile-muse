package com.agent.android.agent.device

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class UsageStatsController(private val context: Context?) {

    fun hasUsageStatsPermission(): Boolean {
        if (context == null) return false
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getCurrentForegroundApp(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("USAGE_STATS", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        if (!hasUsageStatsPermission()) {
            return SkillResult("USAGE_STATS", SkillStatus.PERMISSION_REQUIRED, "Usage Stats Access required. Grant in Settings -> Usage Access.", System.currentTimeMillis() - start, "USAGE_STATS_ACCESS_REQUIRED")
        }

        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return SkillResult("USAGE_STATS", SkillStatus.UNAVAILABLE, "UsageStatsManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 1000 * 60 * 5, now)

            if (stats.isNullOrEmpty()) {
                SkillResult("USAGE_STATS", SkillStatus.SUCCESS, "No recent app usage stats returned", System.currentTimeMillis() - start)
            } else {
                val latest = stats.maxByOrNull { it.lastTimeUsed }
                val pkg = latest?.packageName ?: "UNKNOWN"
                SkillResult("USAGE_STATS", SkillStatus.SUCCESS, "Current Foreground App Package: $pkg", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            SkillResult("USAGE_STATS", SkillStatus.FAILED, "Error querying current app: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun getCurrentForegroundAppPackage(): String? {
        val res = getCurrentForegroundApp()
        if (res.status == SkillStatus.SUCCESS && res.message.contains("Package: ")) {
            return res.message.substringAfter("Package: ").trim()
        }
        return null
    }
}
