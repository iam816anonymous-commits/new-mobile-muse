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

enum class SensorCapabilityState {
    SENSOR_EXISTS,
    SENSOR_REGISTERABLE,
    SENSOR_USABLE,
    SENSOR_DATA_AVAILABLE,
    SENSOR_RESTRICTED,
    SENSOR_UNAVAILABLE
}

enum class SensorResultClassification {
    AVAILABLE_AND_USABLE,
    AVAILABLE_NO_DATA,
    REGISTRATION_FAILED,
    UNAVAILABLE,
    RESTRICTED,
    NOT_APPLICABLE,
    ERROR
}

data class DiscoveredSensorMetadata(
    val type: Int,
    val name: String,
    val vendor: String,
    val version: Int,
    val maximumRange: Float,
    val resolution: Float,
    val power: Float,
    val minDelay: Int,
    val reportingMode: Int,
    val isWakeUpSensor: Boolean,
    val handle: Int,
    val capabilityState: SensorCapabilityState,
    val registrationSuccess: Boolean = false,
    val dataReceived: Boolean = false,
    val sampleValues: List<Float> = emptyList(),
    val classification: SensorResultClassification = SensorResultClassification.UNAVAILABLE,
    val durationMs: Long = 0L,
    val errorReason: String? = null
)

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
    val totalSensorsDiscovered: Int,
    val usableSensorsCount: Int,
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

    fun discoverAllSensors(): List<DiscoveredSensorMetadata> {
        val sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return emptyList()

        val allSensors = try {
            sensorManager.getSensorList(Sensor.TYPE_ALL) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return allSensors.map { s ->
            val reportingMode = try { s.reportingMode } catch (e: Exception) { -1 }
            val isWakeUp = try { s.isWakeUpSensor } catch (e: Exception) { false }
            val handle = try { s.id } catch (e: Exception) { -1 }

            DiscoveredSensorMetadata(
                type = s.type,
                name = s.name ?: "Unknown Sensor",
                vendor = s.vendor ?: "Unknown Vendor",
                version = s.version,
                maximumRange = s.maximumRange,
                resolution = s.resolution,
                power = s.power,
                minDelay = s.minDelay,
                reportingMode = reportingMode,
                isWakeUpSensor = isWakeUp,
                handle = handle,
                capabilityState = SensorCapabilityState.SENSOR_EXISTS,
                registrationSuccess = false,
                dataReceived = false,
                sampleValues = emptyList(),
                classification = SensorResultClassification.UNAVAILABLE
            )
        }
    }

    fun validateSingleSensor(sensorType: Int, timeoutMs: Long = 500L): DiscoveredSensorMetadata {
        val start = System.currentTimeMillis()
        val sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            return DiscoveredSensorMetadata(
                type = sensorType,
                name = "Sensor $sensorType",
                vendor = "N/A",
                version = 0,
                maximumRange = 0f,
                resolution = 0f,
                power = 0f,
                minDelay = 0,
                reportingMode = -1,
                isWakeUpSensor = false,
                handle = -1,
                capabilityState = SensorCapabilityState.SENSOR_UNAVAILABLE,
                classification = SensorResultClassification.UNAVAILABLE,
                durationMs = System.currentTimeMillis() - start,
                errorReason = "SensorManager unavailable"
            )
        }

        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor == null) {
            return DiscoveredSensorMetadata(
                type = sensorType,
                name = "Sensor $sensorType",
                vendor = "N/A",
                version = 0,
                maximumRange = 0f,
                resolution = 0f,
                power = 0f,
                minDelay = 0,
                reportingMode = -1,
                isWakeUpSensor = false,
                handle = -1,
                capabilityState = SensorCapabilityState.SENSOR_UNAVAILABLE,
                classification = SensorResultClassification.UNAVAILABLE,
                durationMs = System.currentTimeMillis() - start,
                errorReason = "Sensor type $sensorType not present on device"
            )
        }

        val deferredResult = CompletableDeferred<List<Float>>()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.values != null && !deferredResult.isCompleted) {
                    val rawVals = event.values.toList()
                    val isValid = rawVals.all { !it.isNaN() && !it.isInfinite() }
                    if (isValid) {
                        deferredResult.complete(rawVals)
                    }
                }
            }

            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }

        var thread: HandlerThread? = null
        return try {
            thread = HandlerThread("SensorValidationThread-${sensor.type}").apply { start() }
            val handler = Handler(thread.looper)

            val registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
            val durMs = System.currentTimeMillis() - start

            if (!registered) {
                DiscoveredSensorMetadata(
                    type = sensor.type,
                    name = sensor.name,
                    vendor = sensor.vendor,
                    version = sensor.version,
                    maximumRange = sensor.maximumRange,
                    resolution = sensor.resolution,
                    power = sensor.power,
                    minDelay = sensor.minDelay,
                    reportingMode = try { sensor.reportingMode } catch (e: Exception) { -1 },
                    isWakeUpSensor = try { sensor.isWakeUpSensor } catch (e: Exception) { false },
                    handle = try { sensor.id } catch (e: Exception) { -1 },
                    capabilityState = SensorCapabilityState.SENSOR_EXISTS,
                    registrationSuccess = false,
                    dataReceived = false,
                    classification = SensorResultClassification.REGISTRATION_FAILED,
                    durationMs = durMs,
                    errorReason = "Registration failed"
                )
            } else {
                val values = runBlocking {
                    withTimeoutOrNull(timeoutMs) {
                        deferredResult.await()
                    }
                }
                val totalDur = System.currentTimeMillis() - start
                if (values != null && values.isNotEmpty()) {
                    DiscoveredSensorMetadata(
                        type = sensor.type,
                        name = sensor.name,
                        vendor = sensor.vendor,
                        version = sensor.version,
                        maximumRange = sensor.maximumRange,
                        resolution = sensor.resolution,
                        power = sensor.power,
                        minDelay = sensor.minDelay,
                        reportingMode = try { sensor.reportingMode } catch (e: Exception) { -1 },
                        isWakeUpSensor = try { sensor.isWakeUpSensor } catch (e: Exception) { false },
                        handle = try { sensor.id } catch (e: Exception) { -1 },
                        capabilityState = SensorCapabilityState.SENSOR_USABLE,
                        registrationSuccess = true,
                        dataReceived = true,
                        sampleValues = values,
                        classification = SensorResultClassification.AVAILABLE_AND_USABLE,
                        durationMs = totalDur
                    )
                } else {
                    DiscoveredSensorMetadata(
                        type = sensor.type,
                        name = sensor.name,
                        vendor = sensor.vendor,
                        version = sensor.version,
                        maximumRange = sensor.maximumRange,
                        resolution = sensor.resolution,
                        power = sensor.power,
                        minDelay = sensor.minDelay,
                        reportingMode = try { sensor.reportingMode } catch (e: Exception) { -1 },
                        isWakeUpSensor = try { sensor.isWakeUpSensor } catch (e: Exception) { false },
                        handle = try { sensor.id } catch (e: Exception) { -1 },
                        capabilityState = SensorCapabilityState.SENSOR_REGISTERABLE,
                        registrationSuccess = true,
                        dataReceived = false,
                        sampleValues = emptyList(),
                        classification = SensorResultClassification.AVAILABLE_NO_DATA,
                        durationMs = totalDur,
                        errorReason = "No event received within timeout"
                    )
                }
            }
        } catch (e: Exception) {
            DiscoveredSensorMetadata(
                type = sensor.type,
                name = sensor.name,
                vendor = sensor.vendor,
                version = sensor.version,
                maximumRange = sensor.maximumRange,
                resolution = sensor.resolution,
                power = sensor.power,
                minDelay = sensor.minDelay,
                reportingMode = -1,
                isWakeUpSensor = false,
                handle = -1,
                capabilityState = SensorCapabilityState.SENSOR_EXISTS,
                registrationSuccess = false,
                dataReceived = false,
                classification = SensorResultClassification.ERROR,
                durationMs = System.currentTimeMillis() - start,
                errorReason = e.message
            )
        } finally {
            sensorManager.unregisterListener(listener)
            thread?.quitSafely()
        }
    }

    fun discoverAndValidateAllSensors(timeoutPerSensorMs: Long = 300L): List<DiscoveredSensorMetadata> {
        val sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return emptyList()

        val allSensors = try {
            sensorManager.getSensorList(Sensor.TYPE_ALL) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val results = mutableListOf<DiscoveredSensorMetadata>()
        for (s in allSensors) {
            val res = validateSingleSensor(s.type, timeoutPerSensorMs)
            results.add(res)
        }
        return results
    }

    fun getSensorList(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("SENSOR_LIST", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val discovered = discoverAllSensors()
        if (discovered.isEmpty()) {
            return SkillResult("SENSOR_LIST", SkillStatus.SUCCESS, "No hardware sensors detected on device", System.currentTimeMillis() - start)
        }

        val sb = StringBuilder("Sensors Discovered (${discovered.size} total):\n")
        for ((idx, s) in discovered.withIndex()) {
            sb.append("${idx + 1}. ${s.name} [Type: ${s.type}, Vendor: ${s.vendor}, Ver: ${s.version}, Range: ${s.maximumRange}, Res: ${s.resolution}]\n")
        }
        return SkillResult("SENSOR_LIST", SkillStatus.SUCCESS, sb.toString().trim(), System.currentTimeMillis() - start)
    }

    fun sampleSensor(sensorType: Int, sensorName: String, timeoutMs: Long = 1000L): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) {
            return SkillResult("SENSOR", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")
        }

        val validated = validateSingleSensor(sensorType, timeoutMs)

        return when (validated.classification) {
            SensorResultClassification.AVAILABLE_AND_USABLE -> {
                val vals = validated.sampleValues.take(3).joinToString(", ") { "%.2f".format(it) }
                val meta = "Vendor: ${validated.vendor}, MaxRange: ${validated.maximumRange}, Res: ${validated.resolution}"
                SkillResult("SENSOR", SkillStatus.SUCCESS, "$sensorName sample: [$vals] ($meta)", System.currentTimeMillis() - start)
            }
            SensorResultClassification.AVAILABLE_NO_DATA -> {
                SkillResult("SENSOR", SkillStatus.SUCCESS, "$sensorName registered successfully (no active event received within timeout)", System.currentTimeMillis() - start)
            }
            SensorResultClassification.UNAVAILABLE -> {
                SkillResult("SENSOR", SkillStatus.UNSUPPORTED, "$sensorName unavailable on device", System.currentTimeMillis() - start, "NO_SENSOR")
            }
            else -> {
                SkillResult("SENSOR", SkillStatus.FAILED, "$sensorName error: ${validated.errorReason ?: "Unknown error"}", System.currentTimeMillis() - start, "SENSOR_ERROR")
            }
        }
    }

    fun runDeviceDiagnostics(): FullDeviceReport {
        val sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val coreSensorTypes = listOf(
            Sensor.TYPE_ACCELEROMETER to "Accelerometer",
            Sensor.TYPE_GYROSCOPE to "Gyroscope",
            Sensor.TYPE_PROXIMITY to "Proximity",
            Sensor.TYPE_LIGHT to "Ambient Light",
            Sensor.TYPE_MAGNETIC_FIELD to "Magnetometer"
        )

        val sensorInfos = coreSensorTypes.map { (type, defaultName) ->
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

        val discovered = discoverAllSensors()
        val usableCount = discovered.count { it.capabilityState == SensorCapabilityState.SENSOR_USABLE }

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
            totalSensorsDiscovered = discovered.size,
            usableSensorsCount = usableCount,
            sensors = sensorInfos
        )
    }
}
