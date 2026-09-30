package com.agent.android.agent.device

import android.content.Context
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class InputStateController(private val context: Context?) {

    fun getKeyboardStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("INPUT_STATE", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val msg = "Keyboard State: HIDDEN / IDLE [Lightweight Observation]"
        return SkillResult("INPUT_STATE", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }

    fun getInputStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("INPUT_STATE", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val msg = "Input Method Capability: AVAILABLE"
        return SkillResult("INPUT_STATE", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }
}
