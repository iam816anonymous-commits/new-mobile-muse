package com.agent.android.agent.device

import android.content.Context
import android.os.Build
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class ScreenCaptureController(private val context: Context?) {

    fun getScreenCaptureStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("SCREEN_CAPTURE", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val stateMsg = "MediaProjection API Available (API ${Build.VERSION.SDK_INT}) [USER_CONSENT_REQUIRED]"
        return SkillResult("SCREEN_CAPTURE", SkillStatus.SUCCESS, "SCREEN_CAPTURE_STATUS: $stateMsg", System.currentTimeMillis() - start)
    }

    fun requestScreenCaptureConsent(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("SCREEN_CAPTURE", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val msg = "MediaProjection Consent Flow Ready [Requires Activity Result]"
        return SkillResult("SCREEN_CAPTURE", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }
}
