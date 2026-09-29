package com.agent.android.agent.device

import android.content.Context
import android.media.AudioManager
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class VolumeController(private val context: Context?) {

    fun getVolume(streamType: Int = AudioManager.STREAM_MUSIC): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start)
        return try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "AudioManager unavailable", System.currentTimeMillis() - start)
            val current = audio.getStreamVolume(streamType)
            val max = audio.getStreamMaxVolume(streamType)
            SkillResult("VOLUME", SkillStatus.SUCCESS, "Volume: $current / $max", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("VOLUME", SkillStatus.FAILED, "Error querying volume: ${e.message}", System.currentTimeMillis() - start)
        }
    }

    fun setVolume(index: Int, streamType: Int = AudioManager.STREAM_MUSIC): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start)
        return try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return SkillResult("VOLUME", SkillStatus.UNAVAILABLE, "AudioManager unavailable", System.currentTimeMillis() - start)
            val max = audio.getStreamMaxVolume(streamType)
            val safeIndex = index.coerceIn(0, max)
            audio.setStreamVolume(streamType, safeIndex, 0)
            SkillResult("VOLUME", SkillStatus.SUCCESS, "Volume set to $safeIndex / $max", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("VOLUME", SkillStatus.FAILED, "Error setting volume: ${e.message}", System.currentTimeMillis() - start)
        }
    }
}
