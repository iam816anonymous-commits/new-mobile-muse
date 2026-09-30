package com.agent.android.agent.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class NetworkController(private val context: Context?) {

    fun getNetworkStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("NETWORK", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return SkillResult("NETWORK", SkillStatus.UNAVAILABLE, "ConnectivityManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            @Suppress("DEPRECATION")
            val active: NetworkInfo? = cm.activeNetworkInfo
            if (active != null && active.isConnected) {
                val typeName = active.typeName
                SkillResult("NETWORK", SkillStatus.SUCCESS, "Network CONNECTED via $typeName", System.currentTimeMillis() - start)
            } else {
                SkillResult("NETWORK", SkillStatus.SUCCESS, "Network DISCONNECTED", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            SkillResult("NETWORK", SkillStatus.FAILED, "Error querying network: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }
}
