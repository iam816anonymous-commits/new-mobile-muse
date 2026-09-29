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
        if (durationMs <= 0 || durationMs > 2000) {
            return SkillResult(
                "VIBRATE",
                SkillStatus.FAILED,
                "Vibration duration must be between 1ms and 2000ms. Received $durationMs ms",
                System.currentTimeMillis() - start,
                "INVALID_ARGUMENT"
            )
        }

        if (context == null) {
            return SkillResult("VIBRATE", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")
        }

        return try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator == null || !vibrator.hasVibrator()) {
                return SkillResult("VIBRATE", SkillStatus.UNSUPPORTED, "Vibrator hardware unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
            SkillResult("VIBRATE", SkillStatus.SUCCESS, "Vibrated for ${durationMs}ms", System.currentTimeMillis() - start)
        } catch (e: SecurityException) {
            SkillResult("VIBRATE", SkillStatus.PERMISSION_REQUIRED, "Vibrate permission required", System.currentTimeMillis() - start, "PERMISSION_REQUIRED")
        } catch (e: Exception) {
            SkillResult("VIBRATE", SkillStatus.FAILED, "Vibration failed: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }
}
