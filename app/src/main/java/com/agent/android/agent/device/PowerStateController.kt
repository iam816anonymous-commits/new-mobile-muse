package com.agent.android.agent.device

import android.content.Context
import android.os.PowerManager
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class PowerStateController(private val context: Context?) {

    fun getPowerStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("POWER", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                ?: return SkillResult("POWER", SkillStatus.UNAVAILABLE, "PowerManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            val isInteractive = pm.isInteractive
            val msg = "Screen State: ${if (isInteractive) "INTERACTIVE / ON" else "OFF"}"
            SkillResult("POWER", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("POWER", SkillStatus.FAILED, "Error querying power status: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }
}
