package com.agent.android.agent.device

import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class InteractionVisualizer {

    fun showIndicator(x: Float, y: Float, actionLabel: String): SkillResult {
        val start = System.currentTimeMillis()
        val msg = "Interaction Overlay Indicator Defined at ($x, $y) for '$actionLabel'"
        return SkillResult("VISUALIZER", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }

    fun dismissIndicator(): SkillResult {
        val start = System.currentTimeMillis()
        return SkillResult("VISUALIZER", SkillStatus.SUCCESS, "Interaction Overlay Indicator Dismissed", System.currentTimeMillis() - start)
    }
}
