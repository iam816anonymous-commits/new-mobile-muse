package com.agent.android.agent.device

import android.content.Context
import android.location.LocationManager
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class LocationController(private val context: Context?) {

    fun getLocationProviders(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("LOCATION", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return SkillResult("LOCATION", SkillStatus.UNAVAILABLE, "LocationManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            val gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val net = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            val msg = "Location Providers: GPS=${if (gps) "ENABLED" else "DISABLED"}, NETWORK=${if (net) "ENABLED" else "DISABLED"}"
            SkillResult("LOCATION", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("LOCATION", SkillStatus.FAILED, "Error querying location providers: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }
}
