package com.agent.android.agent.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class HardwareObservationControllers(private val context: Context?) {

    fun getBatteryStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("BATTERY", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start)
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
            SkillResult("BATTERY", SkillStatus.FAILED, "Error reading battery: ${e.message}", System.currentTimeMillis() - start)
        }
    }

    fun sampleSensor(sensorType: Int, sensorName: String, timeoutMs: Long = 1000L): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("SENSOR", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start)

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return SkillResult("SENSOR", SkillStatus.UNAVAILABLE, "SensorManager unavailable", System.currentTimeMillis() - start)

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

        return try {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
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
            SkillResult("SENSOR", SkillStatus.FAILED, "Sensor error: ${e.message}", System.currentTimeMillis() - start)
        } finally {
            sensorManager.unregisterListener(listener)
        }
    }
}
