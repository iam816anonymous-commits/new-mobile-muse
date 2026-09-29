package com.agent.android.agent.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Vibrator
import android.provider.AlarmClock
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

data class SensorDiagnosticInfo(
    val type: Int,
    val name: String,
    val vendor: String,
    val version: Int,
    val power: Float,
    val resolution: Float,
    val maxRange: Float,
    val isAvailable: Boolean
)

data class FullDeviceReport(
    val androidVersion: String,
    val deviceModel: String,
    val cameraTorchAvailable: Boolean,
    val vibratorAvailable: Boolean,
    val musicVolumeMax: Int,
    val musicVolumeCurrent: Int,
    val timerIntentAvailable: Boolean,
    val alarmIntentAvailable: Boolean,
    val webSearchIntentAvailable: Boolean,
    val sensors: List<SensorDiagnosticInfo>
)

class HardwareObservationControllers(private val context: Context?) {

    fun getBatteryStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("BATTERY", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1

            SkillResult("BATTERY", SkillStatus.SUCCESS, "Battery: $pct%, Charging: $isCharging", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("BATTERY", SkillStatus.FAILED, "Error reading battery: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun sampleSensor(sensorType: Int, sensorName: String, timeoutMs: Long = 1000L): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("SENSOR", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return SkillResult("SENSOR", SkillStatus.UNAVAILABLE, "SensorManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

        val sensor = sensorManager.getDefaultSensor(sensorType)
            ?: return SkillResult("SENSOR", SkillStatus.UNSUPPORTED, "$sensorName unavailable on device", System.currentTimeMillis() - start, "NO_SENSOR")

        val deferredResult = CompletableDeferred<String>()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && !deferredResult.isCompleted) {
                    val values = event.values.take(3).joinToString(", ") { "%.2f".format(it) }
                    deferredResult.complete(values)
                }
            }

            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }

        var sensorThread: HandlerThread? = null
        return try {
            sensorThread = HandlerThread("SensorSamplingThread").apply { start() }
            val handler = Handler(sensorThread.looper)

            val registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
            if (!registered) {
                return SkillResult("SENSOR", SkillStatus.FAILED, "Failed to register listener for $sensorName", System.currentTimeMillis() - start, "REGISTRATION_FAILED")
            }
            val sample = runBlocking {
                withTimeoutOrNull(timeoutMs) {
                    deferredResult.await()
                }
            }
            if (sample != null) {
                SkillResult("SENSOR", SkillStatus.SUCCESS, "$sensorName sample: [$sample]", System.currentTimeMillis() - start)
            } else {
                SkillResult("SENSOR", SkillStatus.FAILED, "$sensorName timeout waiting for sample", System.currentTimeMillis() - start, "SENSOR_TIMEOUT")
            }
        } catch (e: Exception) {
            SkillResult("SENSOR", SkillStatus.FAILED, "Sensor error: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        } finally {
            sensorManager.unregisterListener(listener)
            sensorThread?.quitSafely()
        }
    }

    fun runDeviceDiagnostics(): FullDeviceReport {
        val sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensorTypes = listOf(
            Sensor.TYPE_ACCELEROMETER to "Accelerometer",
            Sensor.TYPE_GYROSCOPE to "Gyroscope",
            Sensor.TYPE_PROXIMITY to "Proximity",
            Sensor.TYPE_LIGHT to "Ambient Light",
            Sensor.TYPE_MAGNETIC_FIELD to "Magnetometer"
        )

        val sensorInfos = sensorTypes.map { (type, defaultName) ->
            val s = sensorManager?.getDefaultSensor(type)
            if (s != null) {
                SensorDiagnosticInfo(
                    type = type,
                    name = s.name,
                    vendor = s.vendor,
                    version = s.version,
                    power = s.power,
                    resolution = s.resolution,
                    maxRange = s.maximumRange,
                    isAvailable = true
                )
            } else {
                SensorDiagnosticInfo(
                    type = type,
                    name = defaultName,
                    vendor = "N/A",
                    version = 0,
                    power = 0f,
                    resolution = 0f,
                    maxRange = 0f,
                    isAvailable = false
                )
            }
        }

        val cameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
        val torchAvail = (cameraManager?.cameraIdList?.size ?: 0) > 0

        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        val vibAvail = vibrator?.hasVibrator() == true

        val audio = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val musicMax = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
        val musicCur = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0

        val pm = context?.packageManager
        val timerIntent = Intent(AlarmClock.ACTION_SET_TIMER)
        val timerAvail = pm?.resolveActivity(timerIntent, 0) != null

        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM)
        val alarmAvail = pm?.resolveActivity(alarmIntent, 0) != null

        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH)
        val searchAvail = pm?.resolveActivity(searchIntent, 0) != null

        return FullDeviceReport(
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            cameraTorchAvailable = torchAvail,
            vibratorAvailable = vibAvail,
            musicVolumeMax = musicMax,
            musicVolumeCurrent = musicCur,
            timerIntentAvailable = timerAvail,
            alarmIntentAvailable = alarmAvail,
            webSearchIntentAvailable = searchAvail,
            sensors = sensorInfos
        )
    }
}
