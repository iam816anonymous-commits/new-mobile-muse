package com.agent.android.agent.device

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Vibrator
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.agent.android.speech.SpeechToTextEngine

enum class CapabilityStatus {
    AVAILABLE,
    UNAVAILABLE,
    UNSUPPORTED,
    PERMISSION_REQUIRED
}

data class DetailedCapabilityInfo(
    val capabilityName: String,
    val capabilityExists: Boolean,
    val capabilityPermitted: Boolean,
    val capabilityUsable: Boolean,
    val status: CapabilityStatus,
    val reason: String
)

data class CapabilityInfo(
    val capabilityName: String,
    val status: CapabilityStatus,
    val reason: String
)

class CapabilityRegistry(private val context: Context?) {

    fun checkDetailedCapabilities(): Map<String, DetailedCapabilityInfo> {
        val map = mutableMapOf<String, DetailedCapabilityInfo>()

        // Torch / Flashlight
        val torchDiag = if (context != null) FlashlightController(context).getTorchDiagnostic() else null
        val torchExists = torchDiag?.capabilityExists ?: false
        val cameraPermitted = torchDiag?.capabilityPermitted ?: false
        val torchUsable = torchDiag?.capabilityUsable ?: false
        map["FLASHLIGHT"] = DetailedCapabilityInfo(
            "FLASHLIGHT",
            torchExists,
            cameraPermitted,
            torchUsable,
            if (!torchExists) CapabilityStatus.UNSUPPORTED else if (cameraPermitted) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED,
            if (!torchExists) "No camera torch hardware" else if (cameraPermitted) torchDiag?.summaryText ?: "Torch available & permitted" else "CAMERA permission required"
        )

        // Vibration
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        val vibExists = vibrator?.hasVibrator() == true
        map["VIBRATION"] = DetailedCapabilityInfo(
            "VIBRATION",
            vibExists,
            true,
            vibExists,
            if (vibExists) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED,
            if (vibExists) "Vibrator present" else "No vibrator hardware"
        )

        // Audio Streams / Volume
        val audio = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val audioExists = audio != null
        map["VOLUME"] = DetailedCapabilityInfo(
            "VOLUME",
            audioExists,
            true,
            audioExists,
            if (audioExists) CapabilityStatus.AVAILABLE else CapabilityStatus.UNAVAILABLE,
            if (audioExists) "AudioManager present" else "No AudioManager"
        )

        // Wi-Fi
        val wifiExists = context?.packageManager?.hasSystemFeature(PackageManager.FEATURE_WIFI) ?: true
        map["WIFI"] = DetailedCapabilityInfo(
            "WIFI",
            wifiExists,
            true,
            wifiExists,
            if (wifiExists) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED,
            if (wifiExists) "Wi-Fi hardware present" else "No Wi-Fi hardware"
        )

        // Bluetooth
        val btExists = context?.packageManager?.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) ?: true
        map["BLUETOOTH"] = DetailedCapabilityInfo(
            "BLUETOOTH",
            btExists,
            true,
            btExists,
            if (btExists) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED,
            if (btExists) "Bluetooth hardware present" else "No Bluetooth hardware"
        )

        // Battery
        map["BATTERY"] = DetailedCapabilityInfo(
            "BATTERY",
            true,
            true,
            true,
            CapabilityStatus.AVAILABLE,
            "Battery manager accessible"
        )

        // Sensors
        val sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensorTypes = listOf(
            "ACCELEROMETER" to Sensor.TYPE_ACCELEROMETER,
            "GYROSCOPE" to Sensor.TYPE_GYROSCOPE,
            "PROXIMITY" to Sensor.TYPE_PROXIMITY,
            "LIGHT" to Sensor.TYPE_LIGHT
        )
        for ((name, type) in sensorTypes) {
            val sensor = sensorManager?.getDefaultSensor(type)
            val exists = sensor != null
            map[name] = DetailedCapabilityInfo(
                name,
                exists,
                true,
                exists,
                if (exists) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED,
                if (exists) "Sensor ${sensor?.name} present" else "Sensor not present on device"
            )
        }

        // Brightness / WRITE_SETTINGS
        val canWriteSettings = context != null && Settings.System.canWrite(context)
        map["BRIGHTNESS"] = DetailedCapabilityInfo(
            "BRIGHTNESS",
            true,
            canWriteSettings,
            canWriteSettings,
            if (canWriteSettings) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED,
            if (canWriteSettings) "WRITE_SETTINGS granted" else "WRITE_SETTINGS permission required"
        )

        map["SCREEN_TIMEOUT"] = DetailedCapabilityInfo(
            "SCREEN_TIMEOUT",
            true,
            canWriteSettings,
            canWriteSettings,
            if (canWriteSettings) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED,
            if (canWriteSettings) "WRITE_SETTINGS granted" else "WRITE_SETTINGS permission required"
        )

        // Ringer / Notification Policy Access
        val notifPolicyGranted = if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.isNotificationPolicyAccessGranted == true
        } else {
            true
        }
        map["RINGER"] = DetailedCapabilityInfo(
            "RINGER",
            true,
            notifPolicyGranted,
            notifPolicyGranted,
            if (notifPolicyGranted) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED,
            if (notifPolicyGranted) "Ringer mode write permitted" else "Notification Policy Access required for SILENT mode"
        )

        map["MEDIA"] = DetailedCapabilityInfo(
            "MEDIA",
            true,
            true,
            true,
            CapabilityStatus.AVAILABLE,
            "Media controls available"
        )

        map["LOCATION_STATUS"] = DetailedCapabilityInfo(
            "LOCATION_STATUS",
            true,
            true,
            true,
            CapabilityStatus.AVAILABLE,
            "Location status available"
        )

        map["APP_LAUNCH"] = DetailedCapabilityInfo(
            "APP_LAUNCH",
            true,
            true,
            true,
            CapabilityStatus.AVAILABLE,
            "App launcher available"
        )

        map["SYSTEM_SETTINGS_INTENTS"] = DetailedCapabilityInfo(
            "SYSTEM_SETTINGS_INTENTS",
            true,
            true,
            true,
            CapabilityStatus.AVAILABLE,
            "Settings intents available"
        )

        // STT
        val sttEngine = if (context != null) SpeechToTextEngine(context) else null
        val sttExists = sttEngine?.isAvailable() ?: false
        val sttPermitted = sttEngine?.hasRecordAudioPermission() ?: false
        map["STT"] = DetailedCapabilityInfo(
            "STT",
            sttExists,
            sttPermitted,
            sttExists && sttPermitted,
            if (!sttExists) CapabilityStatus.UNSUPPORTED else if (sttPermitted) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED,
            if (!sttExists) "SpeechRecognizer unavailable" else if (sttPermitted) "STT available & permitted" else "RECORD_AUDIO permission required"
        )

        // TTS
        map["TTS"] = DetailedCapabilityInfo(
            "TTS",
            true,
            true,
            true,
            CapabilityStatus.AVAILABLE,
            "TextToSpeech API available"
        )

        // Accessibility
        val accEnabled = if (context != null) isAccessibilityServiceEnabled(context) else false
        map["ACCESSIBILITY"] = DetailedCapabilityInfo(
            "ACCESSIBILITY",
            true,
            accEnabled,
            accEnabled,
            if (accEnabled) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED,
            if (accEnabled) "LocalAgentAccessibilityService enabled" else "Accessibility service not enabled in Android Settings"
        )

        return map
    }

    fun checkAllCapabilities(): Map<String, CapabilityInfo> {
        val detailed = checkDetailedCapabilities()
        return detailed.mapValues {
            CapabilityInfo(it.value.capabilityName, it.value.status, it.value.reason)
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = "${context.packageName}/com.agent.android.service.LocalAgentAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
