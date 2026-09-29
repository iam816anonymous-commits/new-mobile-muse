package com.agent.android.agent.device

import android.content.Context
import android.media.AudioManager
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class VolumeController(private val context: Context?) {

    fun parseStreamType(streamName: String): Int {
        return when (streamName.lowercase()) {
            "ring" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            "notification" -> AudioManager.STREAM_NOTIFICATION
            "system" -> AudioManager.STREAM_SYSTEM
            "voice" -> AudioManager.STREAM_VOICE_CALL
            else -> AudioManager.STREAM_MUSIC
        }
    }

    fun getVolume(streamType: Int = AudioManager.STREAM_MUSIC): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")
        return try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "AudioManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
            val current = audio.getStreamVolume(streamType)
            val max = audio.getStreamMaxVolume(streamType)
            val percent = if (max > 0) (current * 100) / max else 0
            SkillResult("VOLUME", SkillStatus.SUCCESS, "Stream $streamType Volume: $current / $max ($percent%)", System.currentTimeMillis() - start)
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

            val initialIndex = audio.getStreamVolume(streamType)
            val max = audio.getStreamMaxVolume(streamType)
            val targetIndex = Math.round((percent / 100.0) * max).toInt().coerceIn(0, max)

            audio.setStreamVolume(streamType, targetIndex, 0)
            val verifiedIndex = audio.getStreamVolume(streamType)

            if (verifiedIndex == targetIndex) {
                val actualPercent = if (max > 0) (verifiedIndex * 100) / max else 0
                SkillResult("VOLUME", SkillStatus.SUCCESS, "Stream $streamType set to $verifiedIndex / $max ($actualPercent%) [Verified]", System.currentTimeMillis() - start)
            } else if (verifiedIndex == initialIndex && targetIndex != initialIndex) {
                SkillResult("VOLUME", SkillStatus.UNSUPPORTED, "Volume stream $streamType is fixed or locked by device OS policy", System.currentTimeMillis() - start, "UNSUPPORTED_FIXED_VOLUME")
            } else {
                val actualPercent = if (max > 0) (verifiedIndex * 100) / max else 0
                SkillResult("VOLUME", SkillStatus.SUCCESS, "Stream $streamType set to nearest hardware index $verifiedIndex / $max ($actualPercent%)", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            SkillResult("VOLUME", SkillStatus.FAILED, "Error setting volume: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }
}
