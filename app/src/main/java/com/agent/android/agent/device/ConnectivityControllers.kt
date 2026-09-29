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
                SkillResult("WIFI_STATUS", SkillStatus.UNAVAILABLE, "WifiManager unavailable", System.currentTimeMillis() - start)
            } else {
                val state = if (wifi.isWifiEnabled) "ENABLED" else "DISABLED"
                SkillResult("WIFI_STATUS", SkillStatus.SUCCESS, "Wi-Fi is $state", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            SkillResult("WIFI_STATUS", SkillStatus.FAILED, "Error querying Wi-Fi: ${e.message}", System.currentTimeMillis() - start)
        }
    }

    fun setWifi(enable: Boolean): SkillResult {
        val start = System.currentTimeMillis()
        return try {
            val wifi = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            if (wifi == null) {
                return SkillResult("WIFI_SET", SkillStatus.UNAVAILABLE, "WifiManager unavailable", System.currentTimeMillis() - start)
            }
            @Suppress("DEPRECATION")
            val success = wifi.setWifiEnabled(enable)
            if (success) {
                SkillResult("WIFI_SET", SkillStatus.SUCCESS, "Wi-Fi set to ${if (enable) "ON" else "OFF"}", System.currentTimeMillis() - start)
            } else {
                SkillResult("WIFI_SET", SkillStatus.UNSUPPORTED, "Direct Wi-Fi control restricted on this device API", System.currentTimeMillis() - start, "UNSUPPORTED_DIRECT_CONTROL")
            }
        } catch (e: SecurityException) {
            SkillResult("WIFI_SET", SkillStatus.PERMISSION_REQUIRED, "Change Wi-Fi permission required", System.currentTimeMillis() - start, "PERMISSION_DENIED")
        } catch (e: Exception) {
            SkillResult("WIFI_SET", SkillStatus.FAILED, "Wi-Fi toggle error: ${e.message}", System.currentTimeMillis() - start)
        }
    }

    fun getBluetoothStatus(): SkillResult {
        val start = System.currentTimeMillis()
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                SkillResult("BT_STATUS", SkillStatus.UNSUPPORTED, "Bluetooth hardware unavailable", System.currentTimeMillis() - start)
            } else {
                val state = if (adapter.isEnabled) "ENABLED" else "DISABLED"
                SkillResult("BT_STATUS", SkillStatus.SUCCESS, "Bluetooth is $state", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            SkillResult("BT_STATUS", SkillStatus.FAILED, "Bluetooth query error: ${e.message}", System.currentTimeMillis() - start)
        }
    }

    fun setBluetooth(enable: Boolean): SkillResult {
        val start = System.currentTimeMillis()
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                return SkillResult("BT_SET", SkillStatus.UNSUPPORTED, "Bluetooth hardware unavailable", System.currentTimeMillis() - start)
            }
            val success = if (enable) {
                @Suppress("DEPRECATION") adapter.enable()
            } else {
                @Suppress("DEPRECATION") adapter.disable()
            }
            if (success) {
                SkillResult("BT_SET", SkillStatus.SUCCESS, "Bluetooth set to ${if (enable) "ON" else "OFF"}", System.currentTimeMillis() - start)
            } else {
                SkillResult("BT_SET", SkillStatus.UNSUPPORTED, "Direct Bluetooth control restricted", System.currentTimeMillis() - start, "UNSUPPORTED_DIRECT_CONTROL")
            }
        } catch (e: SecurityException) {
            SkillResult("BT_SET", SkillStatus.PERMISSION_REQUIRED, "Bluetooth admin permission required", System.currentTimeMillis() - start, "PERMISSION_DENIED")
        } catch (e: Exception) {
            SkillResult("BT_SET", SkillStatus.FAILED, "Bluetooth toggle error: ${e.message}", System.currentTimeMillis() - start)
        }
    }
}
