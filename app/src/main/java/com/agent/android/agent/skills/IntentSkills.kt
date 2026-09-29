package com.agent.android.agent.skills

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

class IntentSkills(private val context: Context) {

    fun setTimer(minutes: Int, label: String = "LocalAgent Timer"): SkillResult {
        val startTime = System.currentTimeMillis()
        if (minutes <= 0) {
            return SkillResult("SET_TIMER", SkillStatus.FAILED, "Duration must be positive", System.currentTimeMillis() - startTime, "INVALID_DURATION")
        }
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            SkillResult("SET_TIMER", SkillStatus.SUCCESS, "Timer for $minutes min launched", System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            SkillResult("SET_TIMER", SkillStatus.UNAVAILABLE, "Timer activity unavailable: ${e.message}", System.currentTimeMillis() - startTime, "ACTIVITY_NOT_FOUND")
        }
    }

    fun setAlarm(hour: Int, minute: Int, label: String = "LocalAgent Alarm"): SkillResult {
        val startTime = System.currentTimeMillis()
        if (hour !in 0..23 || minute !in 0..59) {
            return SkillResult("SET_ALARM", SkillStatus.FAILED, "Invalid time format", System.currentTimeMillis() - startTime, "INVALID_TIME")
        }
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            SkillResult("SET_ALARM", SkillStatus.SUCCESS, "Alarm for $hour:$minute launched", System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            SkillResult("SET_ALARM", SkillStatus.UNAVAILABLE, "Alarm activity unavailable: ${e.message}", System.currentTimeMillis() - startTime, "ACTIVITY_NOT_FOUND")
        }
    }

    fun webSearch(query: String): SkillResult {
        val startTime = System.currentTimeMillis()
        if (query.isBlank()) {
            return SkillResult("WEB_SEARCH", SkillStatus.FAILED, "Search query is empty", System.currentTimeMillis() - startTime, "EMPTY_QUERY")
        }
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            SkillResult("WEB_SEARCH", SkillStatus.SUCCESS, "Web search launched for: $query", System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            SkillResult("WEB_SEARCH", SkillStatus.UNAVAILABLE, "Search handler unavailable: ${e.message}", System.currentTimeMillis() - startTime, "ACTIVITY_NOT_FOUND")
        }
    }
}
