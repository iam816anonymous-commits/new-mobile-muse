package com.agent.android.agent.device

import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

enum class TaskLifetimePolicy {
    FOREGROUND,
    SHORT_TASK,
    LONG_TASK,
    CANCELLED,
    COMPLETED,
    FAILED
}

class BackgroundExecutionPolicy {

    fun getPolicyStatus(): SkillResult {
        val start = System.currentTimeMillis()
        val msg = "Background Execution Policy: FOREGROUND_ONLY [No permanent background loops]"
        return SkillResult("BACKGROUND_POLICY", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }
}
