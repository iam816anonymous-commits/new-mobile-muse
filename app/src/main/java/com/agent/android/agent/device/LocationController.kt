package com.agent.android.agent.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

data class LocationDiagnosticReport(
    val finePermissionGranted: Boolean,
    val coarsePermissionGranted: Boolean,
    val locationServicesEnabled: Boolean,
    val locationMode: Int,
    val locationModeName: String,
    val gpsProviderEnabled: Boolean,
    val networkProviderEnabled: Boolean,
    val networkConnected: Boolean,
    val lastKnownLocationProvider: String?,
    val lastKnownLocationAgeMs: Long?,
    val lastKnownLocationAccuracy: Float?,
    val hasLocationFix: Boolean,
    val status: LocationReadinessStatus,
    val rootCauseExplanation: String
)

enum class LocationReadinessStatus {
    READY,
    DEGRADED,
    UNAVAILABLE
}

class LocationController(private val context: Context?) {

    fun diagnoseLocation(): LocationDiagnosticReport {
        if (context == null) {
            return LocationDiagnosticReport(
                finePermissionGranted = false,
                coarsePermissionGranted = false,
                locationServicesEnabled = false,
                locationMode = -1,
                locationModeName = "UNKNOWN",
                gpsProviderEnabled = false,
                networkProviderEnabled = false,
                networkConnected = false,
                lastKnownLocationProvider = null,
                lastKnownLocationAgeMs = null,
                lastKnownLocationAccuracy = null,
                hasLocationFix = false,
                status = LocationReadinessStatus.UNAVAILABLE,
                rootCauseExplanation = "Context unavailable"
            )
        }

        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        val gpsEnabled = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        val netEnabled = lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        @Suppress("DEPRECATION")
        val locMode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (lm?.isLocationEnabled == true) Settings.Secure.LOCATION_MODE_HIGH_ACCURACY else Settings.Secure.LOCATION_MODE_OFF
            } else {
                Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
            }
        } catch (e: Exception) {
            Settings.Secure.LOCATION_MODE_OFF
        }

        val locModeName = when (locMode) {
            Settings.Secure.LOCATION_MODE_OFF -> "OFF"
            Settings.Secure.LOCATION_MODE_SENSORS_ONLY -> "SENSORS_ONLY (Device Only / GPS)"
            Settings.Secure.LOCATION_MODE_BATTERY_SAVING -> "BATTERY_SAVING (Network Only)"
            Settings.Secure.LOCATION_MODE_HIGH_ACCURACY -> "HIGH_ACCURACY (GPS + Network)"
            else -> "UNKNOWN ($locMode)"
        }

        val locationServicesEnabled = locMode != Settings.Secure.LOCATION_MODE_OFF || gpsEnabled || netEnabled

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        @Suppress("DEPRECATION")
        val activeNet = cm?.activeNetworkInfo
        val networkConnected = activeNet != null && activeNet.isConnected

        var lastLocation: Location? = null
        var lastProvider: String? = null

        if (fineGranted || coarseGranted) {
            try {
                val gpsLoc = if (gpsEnabled) lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
                val netLoc = if (netEnabled) lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
                val passLoc = lm?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

                lastLocation = listOfNotNull(gpsLoc, netLoc, passLoc).maxByOrNull { it.time }
                if (lastLocation != null) {
                    lastProvider = lastLocation.provider
                }
            } catch (e: SecurityException) {
                // Permission revoked
            } catch (e: Exception) {
                // Location query failed
            }
        }

        val now = System.currentTimeMillis()
        val locAge = lastLocation?.let { now - it.time }
        val accuracy = lastLocation?.accuracy
        val hasFix = lastLocation != null

        val rootCauseSb = StringBuilder()
        val overallStatus: LocationReadinessStatus

        if (!fineGranted && !coarseGranted) {
            overallStatus = LocationReadinessStatus.UNAVAILABLE
            rootCauseSb.append("Location permissions (FINE/COARSE) not granted. ")
        } else if (!locationServicesEnabled) {
            overallStatus = LocationReadinessStatus.UNAVAILABLE
            rootCauseSb.append("Master Location Services disabled in System Settings. ")
        } else if (!netEnabled && locMode == Settings.Secure.LOCATION_MODE_SENSORS_ONLY) {
            overallStatus = LocationReadinessStatus.DEGRADED
            rootCauseSb.append("NETWORK_PROVIDER disabled because Location Mode is 'Device Only / Sensors Only'. Switch location mode to High Accuracy in System Settings. ")
        } else if (!netEnabled) {
            overallStatus = LocationReadinessStatus.DEGRADED
            rootCauseSb.append("NETWORK_PROVIDER disabled by OS or NetworkLocationProvider package unavailable. ")
        } else if (!gpsEnabled) {
            overallStatus = LocationReadinessStatus.DEGRADED
            rootCauseSb.append("GPS_PROVIDER disabled in System Settings. ")
        } else if (!hasFix) {
            overallStatus = LocationReadinessStatus.DEGRADED
            rootCauseSb.append("Location providers enabled, but no fresh or last-known location fix obtained yet. ")
        } else {
            overallStatus = LocationReadinessStatus.READY
            rootCauseSb.append("All location systems operational. Fix acquired via $lastProvider provider.")
        }

        return LocationDiagnosticReport(
            finePermissionGranted = fineGranted,
            coarsePermissionGranted = coarseGranted,
            locationServicesEnabled = locationServicesEnabled,
            locationMode = locMode,
            locationModeName = locModeName,
            gpsProviderEnabled = gpsEnabled,
            networkProviderEnabled = netEnabled,
            networkConnected = networkConnected,
            lastKnownLocationProvider = lastProvider,
            lastKnownLocationAgeMs = locAge,
            lastKnownLocationAccuracy = accuracy,
            hasLocationFix = hasFix,
            status = overallStatus,
            rootCauseExplanation = rootCauseSb.toString().trim()
        )
    }

    fun getLocationProviders(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("LOCATION", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val diag = diagnoseLocation()
        val msg = "Location Providers: GPS=${if (diag.gpsProviderEnabled) "ENABLED" else "DISABLED"}, NETWORK=${if (diag.networkProviderEnabled) "ENABLED" else "DISABLED"} | Mode: ${diag.locationModeName} | Status: ${diag.status}"
        return SkillResult("LOCATION", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }

    fun testLocationFix(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("LOCATION_FIX", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val diag = diagnoseLocation()

        if (diag.status == LocationReadinessStatus.UNAVAILABLE) {
            return SkillResult("LOCATION_FIX", SkillStatus.PERMISSION_REQUIRED, "Location Unavailable: ${diag.rootCauseExplanation}", System.currentTimeMillis() - start, "LOCATION_UNAVAILABLE")
        }

        if (diag.hasLocationFix) {
            val ageSec = (diag.lastKnownLocationAgeMs ?: 0L) / 1000
            val msg = "Location Fix: Provider=${diag.lastKnownLocationProvider}, Accuracy=${diag.lastKnownLocationAccuracy ?: 0f}m, Age=${ageSec}s | Status: ${diag.status}"
            return SkillResult("LOCATION_FIX", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
        }

        return SkillResult("LOCATION_FIX", SkillStatus.FAILED, "No location fix available: ${diag.rootCauseExplanation}", System.currentTimeMillis() - start, "FIX_UNAVAILABLE")
    }
}
