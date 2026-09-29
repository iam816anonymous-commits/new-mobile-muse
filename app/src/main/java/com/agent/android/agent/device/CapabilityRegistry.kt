package com.agent.android.agent.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Vibrator
import android.provider.Settings

enum class CapabilityStatus {
    AVAILABLE,
    UNAVAILABLE,
    UNSUPPORTED,
    PERMISSION_REQUIRED
}

data class CapabilityInfo(
    val capabilityName: String,
    val status: CapabilityStatus,
    val reason: String
)

class CapabilityRegistry(private val context: Context?) {

    fun checkAllCapabilities(): Map<String, CapabilityInfo> {
        val map = mutableMapOf<String, CapabilityInfo>()

        // Torch / Flashlight
        val cameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val torchOk = (cameraManager?.cameraIdList?.size ?: 0) > 0
        map["FLASHLIGHT"] = CapabilityInfo("FLASHLIGHT", if (torchOk) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED, if (torchOk) "Camera torch present" else "No camera torch")

        // Vibration
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        val vibOk = vibrator?.hasVibrator() == true
        map["VIBRATION"] = CapabilityInfo("VIBRATION", if (vibOk) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED, if (vibOk) "Vibrator present" else "No vibrator hardware")

        // Volume / Audio
        val audio = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val audioOk = audio != null
        map["VOLUME"] = CapabilityInfo("VOLUME", if (audioOk) CapabilityStatus.AVAILABLE else CapabilityStatus.UNAVAILABLE, if (audioOk) "AudioManager present" else "No AudioManager")

        // Wi-Fi & Bluetooth
        map["WIFI"] = CapabilityInfo("WIFI", CapabilityStatus.AVAILABLE, "Wi-Fi manager accessible")
        map["BLUETOOTH"] = CapabilityInfo("BLUETOOTH", CapabilityStatus.AVAILABLE, "Bluetooth adapter accessible")

        // Sensors
        val sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensorTypes = listOf(
            "ACCELEROMETER" to Sensor.TYPE_ACCELEROMETER,
            "GYROSCOPE" to Sensor.TYPE_GYROSCOPE,
            "PROXIMITY" to Sensor.TYPE_PROXIMITY,
            "LIGHT" to Sensor.TYPE_LIGHT,
            "MAGNETOMETER" to Sensor.TYPE_MAGNETIC_FIELD
        )
        for ((name, type) in sensorTypes) {
            val sensor = sensorManager?.getDefaultSensor(type)
            if (sensor != null) {
                map[name] = CapabilityInfo(name, CapabilityStatus.AVAILABLE, "Sensor ${sensor.name} present")
            } else {
                map[name] = CapabilityInfo(name, CapabilityStatus.UNSUPPORTED, "Hardware sensor not present on device")
            }
        }

        // Brightness
        val canWriteSettings = context != null && Settings.System.canWrite(context)
        map["BRIGHTNESS"] = CapabilityInfo(
            "BRIGHTNESS",
            if (canWriteSettings) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED,
            if (canWriteSettings) "System brightness control available" else "WRITE_SETTINGS special permission required"
        )

        map["SCREEN_TIMEOUT"] = CapabilityInfo("SCREEN_TIMEOUT", if (canWriteSettings) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED, if (canWriteSettings) "Screen timeout setting available" else "WRITE_SETTINGS special permission required")
        map["RINGER"] = CapabilityInfo("RINGER", CapabilityStatus.AVAILABLE, "Ringer mode read/write available")
        map["MEDIA"] = CapabilityInfo("MEDIA", CapabilityStatus.AVAILABLE, "Media key dispatch available")
        map["LOCATION_STATUS"] = CapabilityInfo("LOCATION_STATUS", CapabilityStatus.AVAILABLE, "Read-only location provider status available")
        map["APP_LAUNCH"] = CapabilityInfo("APP_LAUNCH", CapabilityStatus.AVAILABLE, "PackageManager app discovery & launch available")
        map["SYSTEM_SETTINGS_INTENTS"] = CapabilityInfo("SYSTEM_SETTINGS_INTENTS", CapabilityStatus.AVAILABLE, "System Settings intents available")

        return map
    }
}
