package com.agent.android.agent.device

import android.content.Context
import android.media.AudioManager
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class VolumeController(private val context: Context?) {

    fun getVolume(streamType: Int = AudioManager.STREAM_MUSIC): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")
        return try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "AudioManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
            val current = audio.getStreamVolume(streamType)
            val max = audio.getStreamMaxVolume(streamType)
            SkillResult("VOLUME", SkillStatus.SUCCESS, "Stream $streamType Volume: $current / $max", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("VOLUME", SkillStatus.FAILED, "Error querying volume: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun setVolumePercentage(percent: Int, streamType: Int = AudioManager.STREAM_MUSIC): SkillResult {
        val start = System.currentTimeMillis()
        if (percent !in 0..100) {
            return SkillResult("VOLUME", SkillStatus.FAILED, "Volume percent must be 0..100. Received $percent", System.currentTimeMillis() - start, "INVALID_ARGUMENT")
        }
        if (context == null) return SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "AudioManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            val max = audio.getStreamMaxVolume(streamType)
            val targetIndex = ((percent / 100.0) * max).toInt().coerceIn(0, max)

            audio.setStreamVolume(streamType, targetIndex, 0)
            val verifiedIndex = audio.getStreamVolume(streamType)

            if (verifiedIndex == targetIndex) {
                SkillResult("VOLUME", SkillStatus.SUCCESS, "Stream $streamType set to $targetIndex / $max ($percent%) [Verified]", System.currentTimeMillis() - start)
            } else {
                SkillResult("VOLUME", SkillStatus.FAILED, "Volume mismatch. Expected $targetIndex, Actual $verifiedIndex / $max", System.currentTimeMillis() - start, "VERIFICATION_FAILED")
            }
        } catch (e: Exception) {
            SkillResult("VOLUME", SkillStatus.FAILED, "Error setting volume: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }
}
