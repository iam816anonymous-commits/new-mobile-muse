package com.agent.android.agent.skills

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

class IntentSkills(private val context: Context) {

    fun setTimer(minutes: Int, label: String = "LocalAgent Timer"): SkillResult {
        val startTime = System.currentTimeMillis()
        if (minutes <= 0) {
            return SkillResult("SET_TIMER", SkillStatus.FAILED, "Duration must be positive. Received $minutes", System.currentTimeMillis() - startTime, "INVALID_ARGUMENT")
        }
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        if (resolveInfo == null) {
            return SkillResult("SET_TIMER", SkillStatus.UNSUPPORTED, "No compatible AlarmClock Timer activity resolved on device", System.currentTimeMillis() - startTime, "ACTIVITY_NOT_FOUND")
        }

        return try {
            context.startActivity(intent)
            SkillResult("SET_TIMER", SkillStatus.SUCCESS, "Timer request accepted for $minutes min [Handler: ${resolveInfo.activityInfo.packageName}]", System.currentTimeMillis() - startTime)
        } catch (e: SecurityException) {
            SkillResult("SET_TIMER", SkillStatus.PERMISSION_REQUIRED, "SET_ALARM permission required for Timer: ${e.message}", System.currentTimeMillis() - startTime, "PERMISSION_REQUIRED")
        } catch (e: Exception) {
            SkillResult("SET_TIMER", SkillStatus.UNAVAILABLE, "Timer activity launch failed: ${e.message}", System.currentTimeMillis() - startTime, "UNAVAILABLE")
        }
    }

    fun setAlarm(hour: Int, minute: Int, label: String = "LocalAgent Alarm"): SkillResult {
        val startTime = System.currentTimeMillis()
        if (hour !in 0..23 || minute !in 0..59) {
            return SkillResult("SET_ALARM", SkillStatus.FAILED, "Invalid time format ($hour:$minute). Hour must be 0..23, Minute 0..59", System.currentTimeMillis() - startTime, "INVALID_ARGUMENT")
        }
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        if (resolveInfo == null) {
            return SkillResult("SET_ALARM", SkillStatus.UNSUPPORTED, "No compatible AlarmClock Alarm activity resolved on device", System.currentTimeMillis() - startTime, "ACTIVITY_NOT_FOUND")
        }

        return try {
            context.startActivity(intent)
            SkillResult("SET_ALARM", SkillStatus.SUCCESS, "Alarm request accepted for $hour:$minute [Handler: ${resolveInfo.activityInfo.packageName}]", System.currentTimeMillis() - startTime)
        } catch (e: SecurityException) {
            SkillResult("SET_ALARM", SkillStatus.PERMISSION_REQUIRED, "SET_ALARM permission required for Alarm: ${e.message}", System.currentTimeMillis() - startTime, "PERMISSION_REQUIRED")
        } catch (e: Exception) {
            SkillResult("SET_ALARM", SkillStatus.UNAVAILABLE, "Alarm activity launch failed: ${e.message}", System.currentTimeMillis() - startTime, "UNAVAILABLE")
        }
    }

    fun webSearch(query: String): SkillResult {
        val startTime = System.currentTimeMillis()
        if (query.isBlank()) {
            return SkillResult("WEB_SEARCH", SkillStatus.FAILED, "Search query is empty", System.currentTimeMillis() - startTime, "INVALID_ARGUMENT")
        }
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        if (resolveInfo == null) {
            return SkillResult("WEB_SEARCH", SkillStatus.UNSUPPORTED, "No compatible Search Activity resolved", System.currentTimeMillis() - startTime, "ACTIVITY_NOT_FOUND")
        }

        return try {
            context.startActivity(intent)
            SkillResult("WEB_SEARCH", SkillStatus.SUCCESS, "Web search launched for: $query [Handler: ${resolveInfo.activityInfo.packageName}]", System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            SkillResult("WEB_SEARCH", SkillStatus.UNAVAILABLE, "Search handler launch failed: ${e.message}", System.currentTimeMillis() - startTime, "UNAVAILABLE")
        }
    }
}
