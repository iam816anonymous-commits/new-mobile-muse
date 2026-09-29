package com.agent.android.agent.device

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class ConnectivityControllers(private val context: Context?) {

    fun getWifiStatus(): SkillResult {
        val start = System.currentTimeMillis()
        return try {
            val wifi = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            if (wifi == null) {
                SkillResult("WIFI_STATUS", SkillStatus.UNAVAILABLE, "WifiManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
            } else {
                val state = if (wifi.isWifiEnabled) "ENABLED" else "DISABLED"
                SkillResult("WIFI_STATUS", SkillStatus.SUCCESS, "Wi-Fi is $state", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            SkillResult("WIFI_STATUS", SkillStatus.FAILED, "Error querying Wi-Fi: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun setWifi(enable: Boolean): SkillResult {
        val start = System.currentTimeMillis()
        return try {
            val wifi = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            if (wifi == null) {
                return SkillResult("WIFI_SET", SkillStatus.UNAVAILABLE, "WifiManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
            }
            @Suppress("DEPRECATION")
            val success = wifi.setWifiEnabled(enable)
            if (success) {
                SkillResult("WIFI_SET", SkillStatus.SUCCESS, "Wi-Fi request sent (${if (enable) "ON" else "OFF"})", System.currentTimeMillis() - start)
            } else {
                SkillResult("WIFI_SET", SkillStatus.UNSUPPORTED, "Direct Wi-Fi toggle restricted on this device API", System.currentTimeMillis() - start, "UNSUPPORTED_DIRECT_CONTROL")
            }
        } catch (e: SecurityException) {
            SkillResult("WIFI_SET", SkillStatus.PERMISSION_REQUIRED, "CHANGE_WIFI_STATE permission required", System.currentTimeMillis() - start, "PERMISSION_REQUIRED")
        } catch (e: Exception) {
            SkillResult("WIFI_SET", SkillStatus.FAILED, "Wi-Fi toggle error: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun getBluetoothStatus(): SkillResult {
        val start = System.currentTimeMillis()
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                SkillResult("BT_STATUS", SkillStatus.UNSUPPORTED, "Bluetooth hardware unavailable", System.currentTimeMillis() - start, "NO_BLUETOOTH")
            } else {
                val state = if (adapter.isEnabled) "ENABLED" else "DISABLED"
                SkillResult("BT_STATUS", SkillStatus.SUCCESS, "Bluetooth is $state", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            SkillResult("BT_STATUS", SkillStatus.FAILED, "Bluetooth query error: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun setBluetooth(enable: Boolean): SkillResult {
        val start = System.currentTimeMillis()
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                return SkillResult("BT_SET", SkillStatus.UNSUPPORTED, "Bluetooth hardware unavailable", System.currentTimeMillis() - start, "NO_BLUETOOTH")
            }

            val isCurrentlyEnabled = adapter.isEnabled
            if (enable && isCurrentlyEnabled) {
                return SkillResult("BT_SET", SkillStatus.SUCCESS, "Bluetooth is ALREADY_ON", System.currentTimeMillis() - start)
            }
            if (!enable && !isCurrentlyEnabled) {
                return SkillResult("BT_SET", SkillStatus.SUCCESS, "Bluetooth is ALREADY_OFF", System.currentTimeMillis() - start)
            }

            @Suppress("DEPRECATION")
            val requested = if (enable) adapter.enable() else adapter.disable()
            if (!requested) {
                return SkillResult("BT_SET", SkillStatus.UNSUPPORTED, "Direct Bluetooth toggle restricted on target Android configuration. User action required via Settings.", System.currentTimeMillis() - start, "UNSUPPORTED_DIRECT_CONTROL")
            }

            // Async verification loop (up to 2000ms timeout)
            val startTimeMs = System.currentTimeMillis()
            var verified = false
            while (System.currentTimeMillis() - startTimeMs < 2000L) {
                if (adapter.isEnabled == enable) {
                    verified = true
                    break
                }
                Thread.sleep(100)
            }

            if (verified) {
                SkillResult("BT_SET", SkillStatus.SUCCESS, "Bluetooth set to ${if (enable) "ON" else "OFF"} [Verified]", System.currentTimeMillis() - start)
            } else {
                SkillResult("BT_SET", SkillStatus.FAILED, "Bluetooth state change timed out or rejected by OS", System.currentTimeMillis() - start, "TIMEOUT")
            }
        } catch (e: SecurityException) {
            SkillResult("BT_SET", SkillStatus.PERMISSION_REQUIRED, "BLUETOOTH_ADMIN permission required", System.currentTimeMillis() - start, "PERMISSION_REQUIRED")
        } catch (e: Exception) {
            SkillResult("BT_SET", SkillStatus.FAILED, "Bluetooth toggle error: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }
}
