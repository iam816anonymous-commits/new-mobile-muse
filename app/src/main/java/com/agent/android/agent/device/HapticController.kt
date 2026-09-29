package com.agent.android.agent.device

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class HapticController(private val context: Context?) {

    fun vibrate(durationMs: Long = 300L): SkillResult {
        val start = System.currentTimeMillis()
        val safeDuration = durationMs.coerceIn(50L, 2000L)
        if (context == null) {
            return SkillResult("VIBRATE", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")
        }
        return try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator == null || !vibrator.hasVibrator()) {
                return SkillResult("VIBRATE", SkillStatus.UNSUPPORTED, "Vibrator hardware unavailable", System.currentTimeMillis() - start, "NO_VIBRATOR")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(safeDuration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(safeDuration)
            }
            SkillResult("VIBRATE", SkillStatus.SUCCESS, "Vibrated for ${safeDuration}ms", System.currentTimeMillis() - start)
        } catch (e: SecurityException) {
            SkillResult("VIBRATE", SkillStatus.PERMISSION_REQUIRED, "Vibrate permission required", System.currentTimeMillis() - start, "PERMISSION_DENIED")
        } catch (e: Exception) {
            SkillResult("VIBRATE", SkillStatus.FAILED, "Vibration failed: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_ERROR")
        }
    }
}
