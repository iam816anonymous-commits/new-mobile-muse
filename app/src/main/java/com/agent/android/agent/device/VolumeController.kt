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

    fun getStreamName(streamType: Int): String {
        return when (streamType) {
            AudioManager.STREAM_RING -> "RING"
            AudioManager.STREAM_ALARM -> "ALARM"
            AudioManager.STREAM_NOTIFICATION -> "NOTIFICATION"
            AudioManager.STREAM_SYSTEM -> "SYSTEM"
            AudioManager.STREAM_VOICE_CALL -> "VOICE"
            else -> "MUSIC"
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
            val percent = if (max > 0) Math.round((current.toDouble() / max.toDouble()) * 100.0).toInt() else 0
            val streamName = getStreamName(streamType)
            val message = "STREAM: $streamName | CURRENT_INDEX: $current | MAX_INDEX: $max | PERCENTAGE: $percent%"
            SkillResult("VOLUME", SkillStatus.SUCCESS, message, System.currentTimeMillis() - start)
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
            val streamName = getStreamName(streamType)

            if (verifiedIndex == targetIndex) {
                val actualPercent = if (max > 0) Math.round((verifiedIndex.toDouble() / max.toDouble()) * 100.0).toInt() else 0
                val message = "STREAM: $streamName | CURRENT_INDEX: $verifiedIndex | MAX_INDEX: $max | PERCENTAGE: $actualPercent% [Verified]"
                SkillResult("VOLUME", SkillStatus.SUCCESS, message, System.currentTimeMillis() - start)
            } else if (verifiedIndex == initialIndex && targetIndex != initialIndex) {
                SkillResult("VOLUME", SkillStatus.UNSUPPORTED, "Volume stream $streamName is fixed or locked by device OS policy", System.currentTimeMillis() - start, "UNSUPPORTED_FIXED_VOLUME")
            } else {
                val actualPercent = if (max > 0) Math.round((verifiedIndex.toDouble() / max.toDouble()) * 100.0).toInt() else 0
                val message = "STREAM: $streamName | CURRENT_INDEX: $verifiedIndex | MAX_INDEX: $max | PERCENTAGE: $actualPercent%"
                SkillResult("VOLUME", SkillStatus.SUCCESS, message, System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            SkillResult("VOLUME", SkillStatus.FAILED, "Error setting volume: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }
}
