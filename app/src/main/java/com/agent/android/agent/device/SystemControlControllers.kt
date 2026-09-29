package com.agent.android.agent.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class SystemControlControllers(private val context: Context?) {

    fun setBrightness(percent: Int): SkillResult {
        val start = System.currentTimeMillis()
        if (percent !in 0..100) {
            return SkillResult("BRIGHTNESS", SkillStatus.FAILED, "Percent must be 0..100. Received $percent", System.currentTimeMillis() - start, "INVALID_ARGUMENT")
        }
        if (context == null) return SkillResult("BRIGHTNESS", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val value = Math.round((percent / 100.0) * 255).toInt().coerceIn(0, 255)
            val canWrite = Settings.System.canWrite(context)
            if (!canWrite) {
                return SkillResult("BRIGHTNESS", SkillStatus.PERMISSION_REQUIRED, "WRITE_SETTINGS permission required to modify system brightness", System.currentTimeMillis() - start, "PERMISSION_REQUIRED")
            }
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            val verified = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, -1)
            val verifiedPct = if (verified >= 0) Math.round((verified / 255.0) * 100).toInt() else -1
            SkillResult("BRIGHTNESS", SkillStatus.SUCCESS, "System brightness set to $verified / 255 ($verifiedPct%) [Verified]", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("BRIGHTNESS", SkillStatus.FAILED, "Error setting brightness: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun setScreenTimeout(seconds: Int): SkillResult {
        val start = System.currentTimeMillis()
        val validSeconds = listOf(15, 30, 60, 120, 300, 600, 1800)
        if (seconds !in validSeconds) {
            return SkillResult("SCREEN_TIMEOUT", SkillStatus.FAILED, "Timeout must be one of $validSeconds seconds. Received $seconds", System.currentTimeMillis() - start, "INVALID_ARGUMENT")
        }
        if (context == null) return SkillResult("SCREEN_TIMEOUT", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val canWrite = Settings.System.canWrite(context)
            if (!canWrite) {
                return SkillResult("SCREEN_TIMEOUT", SkillStatus.PERMISSION_REQUIRED, "WRITE_SETTINGS permission required to modify screen timeout", System.currentTimeMillis() - start, "PERMISSION_REQUIRED")
            }
            val ms = seconds * 1000
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, ms)
            val verifiedMs = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, -1)
            val verifiedSec = verifiedMs / 1000
            SkillResult("SCREEN_TIMEOUT", SkillStatus.SUCCESS, "Screen timeout set to $verifiedSec s [Verified]", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("SCREEN_TIMEOUT", SkillStatus.FAILED, "Error setting screen timeout: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun setRingerMode(modeStr: String): SkillResult {
        val start = System.currentTimeMillis()
        val targetMode = when (modeStr.lowercase()) {
            "normal" -> AudioManager.RINGER_MODE_NORMAL
            "silent" -> AudioManager.RINGER_MODE_SILENT
            "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
            else -> return SkillResult("RINGER", SkillStatus.FAILED, "Mode must be normal, silent, or vibrate. Received '$modeStr'", System.currentTimeMillis() - start, "INVALID_ARGUMENT")
        }

        if (context == null) return SkillResult("RINGER", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return SkillResult("RINGER", SkillStatus.UNAVAILABLE, "AudioManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            audio.ringerMode = targetMode
            val verifiedMode = audio.ringerMode
            val verifiedName = when (verifiedMode) {
                AudioManager.RINGER_MODE_NORMAL -> "NORMAL"
                AudioManager.RINGER_MODE_SILENT -> "SILENT"
                AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
                else -> "UNKNOWN"
            }
            SkillResult("RINGER", SkillStatus.SUCCESS, "Ringer mode set to $verifiedName [Verified]", System.currentTimeMillis() - start)
        } catch (e: SecurityException) {
            SkillResult("RINGER", SkillStatus.PERMISSION_REQUIRED, "Notification Policy Access required to change ringer mode", System.currentTimeMillis() - start, "PERMISSION_REQUIRED")
        } catch (e: Exception) {
            SkillResult("RINGER", SkillStatus.FAILED, "Error setting ringer mode: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun dispatchMediaKey(action: String): SkillResult {
        val start = System.currentTimeMillis()
        val keyCode = when (action.lowercase()) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> return SkillResult("MEDIA", SkillStatus.FAILED, "Media action must be play, pause, stop, next, or previous. Received '$action'", System.currentTimeMillis() - start, "INVALID_ARGUMENT")
        }

        if (context == null) return SkillResult("MEDIA", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return SkillResult("MEDIA", SkillStatus.UNAVAILABLE, "AudioManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            val downEvent = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keyCode, 0)
            val upEvent = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0)

            audio.dispatchMediaKeyEvent(downEvent)
            audio.dispatchMediaKeyEvent(upEvent)

            SkillResult("MEDIA", SkillStatus.SUCCESS, "Media key '$action' dispatched", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("MEDIA", SkillStatus.FAILED, "Error dispatching media key: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun getDeviceInfo(): SkillResult {
        val start = System.currentTimeMillis()
        val storageInfoStr = try {
            val dataDir = Environment.getDataDirectory()
            if (dataDir != null) {
                val stat = StatFs(dataDir.path)
                val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
                val totalBytes = stat.blockCountLong * stat.blockSizeLong
                val availMb = availableBytes / (1024 * 1024)
                val totalMb = totalBytes / (1024 * 1024)
                "Storage: ${availMb}MB free / ${totalMb}MB total"
            } else {
                "Storage: N/A"
            }
        } catch (e: Exception) {
            "Storage: N/A"
        }

        val infoStr = "Device: ${Build.MANUFACTURER} ${Build.MODEL} | OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) | $storageInfoStr"
        return SkillResult("DEVICE_INFO", SkillStatus.SUCCESS, infoStr, System.currentTimeMillis() - start)
    }

    fun getNetworkStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("NETWORK_STATUS", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            @Suppress("DEPRECATION")
            val activeNet: NetworkInfo? = cm?.activeNetworkInfo
            if (activeNet != null && activeNet.isConnected) {
                val typeName = activeNet.typeName
                SkillResult("NETWORK_STATUS", SkillStatus.SUCCESS, "Connected via $typeName (${activeNet.state})", System.currentTimeMillis() - start)
            } else {
                SkillResult("NETWORK_STATUS", SkillStatus.SUCCESS, "Network DISCONNECTED", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            SkillResult("NETWORK_STATUS", SkillStatus.FAILED, "Error reading network status: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun getLocationStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("LOCATION_STATUS", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsOk = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val netOk = lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

            SkillResult("LOCATION_STATUS", SkillStatus.SUCCESS, "GPS Provider: ${if (gpsOk) "ENABLED" else "DISABLED"} | Network Provider: ${if (netOk) "ENABLED" else "DISABLED"}", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("LOCATION_STATUS", SkillStatus.FAILED, "Error querying location status: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun openSystemSettings(target: String): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("SYSTEM_SETTINGS", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val intentAction = when (target.lowercase()) {
            "wifi", "wifi settings" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth", "bluetooth settings" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "battery", "battery settings" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "accessibility", "accessibility settings" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        val intent = Intent(intentAction).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        if (resolveInfo == null) {
            return SkillResult("SYSTEM_SETTINGS", SkillStatus.UNSUPPORTED, "No activity resolved for $target settings", System.currentTimeMillis() - start, "ACTIVITY_NOT_FOUND")
        }

        return try {
            context.startActivity(intent)
            SkillResult("SYSTEM_SETTINGS", SkillStatus.SUCCESS, "Opened $target settings [Handler: ${resolveInfo.activityInfo.packageName}]", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("SYSTEM_SETTINGS", SkillStatus.UNAVAILABLE, "Failed to launch $target settings: ${e.message}", System.currentTimeMillis() - start, "UNAVAILABLE")
        }
    }
}
