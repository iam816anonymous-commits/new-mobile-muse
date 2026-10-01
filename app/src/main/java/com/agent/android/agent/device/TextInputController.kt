package com.agent.android.agent.device

import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class TextInputController {

    fun executeTextContract(action: String, content: String? = null): SkillResult {
        val start = System.currentTimeMillis()
        val validActions = listOf("settext", "appendtext", "cleartext", "paste")

        if (!validActions.contains(action.lowercase())) {
            return SkillResult("TEXT_INPUT", SkillStatus.FAILED, "Unsupported text input action '$action'", System.currentTimeMillis() - start, "INVALID_ACTION")
        }

        val msg = "Text Contract Defined: '$action' (Content: '${content ?: ""}') [Phase 3 Foundation Ready]"
        return SkillResult("TEXT_INPUT", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }
}
