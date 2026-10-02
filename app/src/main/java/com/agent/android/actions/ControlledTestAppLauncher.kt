package com.agent.android.actions

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.agent.android.agent.skills.app.AppLauncherImpl
import com.agent.android.observation.AccessibilityObservationEngine
import com.agent.android.observation.GuidedTestApp
import com.agent.android.observation.ObservationState

enum class LaunchStatus {
    SUCCESS,
    TARGET_NOT_INSTALLED,
    LAUNCH_FAILED,
    FOREGROUND_TIMEOUT,
    WRONG_FOREGROUND_PACKAGE,
    CANCELLED
}

data class LaunchResult(
    val status: LaunchStatus,
    val targetAppName: String,
    val expectedPackage: String?,
    val actualPackage: String?,
    val actualActivity: String?,
    val durationMs: Long,
    val message: String
)

class ControlledTestAppLauncher(private val context: Context?) {

    companion object {
        const val WAIT_TIMEOUT_MS = 15000L
        const val POLL_INTERVAL_MS = 500L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null

    fun resolveTargetPackage(targetApp: GuidedTestApp): String? {
        if (context == null) return targetApp.staticPackage
        val pm = context.packageManager
        if (targetApp.staticPackage != null) {
            val intent = pm.getLaunchIntentForPackage(targetApp.staticPackage)
            if (intent != null) return targetApp.staticPackage
            return try {
                pm.getPackageInfo(targetApp.staticPackage, 0)
                targetApp.staticPackage
            } catch (e: Exception) {
                null
            }
        } else if (targetApp == GuidedTestApp.CALCULATOR) {
            val appLauncher = AppLauncherImpl(context)
            val valRes = appLauncher.validateApp("Calculator")
            if (valRes.status.name.contains("RESOLVED") && valRes.resolvedPackage != null) {
                return valRes.resolvedPackage
            }
            val installed = try {
                pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            } catch (e: Exception) {
                emptyList()
            }
            for (info in installed) {
                if (info.packageName.contains("calculator", ignoreCase = true) || info.packageName.contains("calc", ignoreCase = true)) {
                    if (pm.getLaunchIntentForPackage(info.packageName) != null) {
                        return info.packageName
                    }
                }
            }
        }
        return null
    }

    fun launchAndWaitForForeground(
        targetApp: GuidedTestApp,
        observationEngine: AccessibilityObservationEngine?,
        onComplete: (LaunchResult) -> Unit
    ) {
        cancel()
        val start = System.currentTimeMillis()
        val resolvedPkg = resolveTargetPackage(targetApp)

        if (resolvedPkg == null) {
            onComplete(
                LaunchResult(
                    status = LaunchStatus.TARGET_NOT_INSTALLED,
                    targetAppName = targetApp.label,
                    expectedPackage = targetApp.staticPackage ?: "Calculator",
                    actualPackage = null,
                    actualActivity = null,
                    durationMs = System.currentTimeMillis() - start,
                    message = "TARGET_NOT_INSTALLED: Target application '${targetApp.label}' is not installed or resolvable."
                )
            )
            return
        }

        if (context == null) {
            onComplete(
                LaunchResult(
                    status = LaunchStatus.LAUNCH_FAILED,
                    targetAppName = targetApp.label,
                    expectedPackage = resolvedPkg,
                    actualPackage = null,
                    actualActivity = null,
                    durationMs = System.currentTimeMillis() - start,
                    message = "LAUNCH_FAILED: Context unavailable for application launch."
                )
            )
            return
        }

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(resolvedPkg)
        if (launchIntent == null) {
            onComplete(
                LaunchResult(
                    status = LaunchStatus.LAUNCH_FAILED,
                    targetAppName = targetApp.label,
                    expectedPackage = resolvedPkg,
                    actualPackage = null,
                    actualActivity = null,
                    durationMs = System.currentTimeMillis() - start,
                    message = "LAUNCH_FAILED: No launch intent available for package '$resolvedPkg'."
                )
            )
            return
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            onComplete(
                LaunchResult(
                    status = LaunchStatus.LAUNCH_FAILED,
                    targetAppName = targetApp.label,
                    expectedPackage = resolvedPkg,
                    actualPackage = null,
                    actualActivity = null,
                    durationMs = System.currentTimeMillis() - start,
                    message = "LAUNCH_FAILED: Exception starting activity: ${e.message}"
                )
            )
            return
        }

        pollRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - start
                val snap = observationEngine?.captureCurrentScreen()
                val currentPkg = snap?.packageName

                val isRunOwnedSnapshot = snap != null && snap.timestampMs >= start

                if (currentPkg == resolvedPkg && snap?.state == ObservationState.SUCCESS && isRunOwnedSnapshot) {
                    onComplete(
                        LaunchResult(
                            status = LaunchStatus.SUCCESS,
                            targetAppName = targetApp.label,
                            expectedPackage = resolvedPkg,
                            actualPackage = currentPkg,
                            actualActivity = snap.activityName,
                            durationMs = elapsed,
                            message = "SUCCESS: Target application '$resolvedPkg' confirmed in foreground."
                        )
                    )
                    return
                }

                if (elapsed >= WAIT_TIMEOUT_MS) {
                    onComplete(
                        LaunchResult(
                            status = LaunchStatus.FOREGROUND_TIMEOUT,
                            targetAppName = targetApp.label,
                            expectedPackage = resolvedPkg,
                            actualPackage = currentPkg,
                            actualActivity = snap?.activityName,
                            durationMs = elapsed,
                            message = "FOREGROUND_TIMEOUT: Target '$resolvedPkg' did not become foreground within ${WAIT_TIMEOUT_MS}ms. Last detected: '$currentPkg'."
                        )
                    )
                    return
                }

                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }

        handler.postDelayed(pollRunnable!!, POLL_INTERVAL_MS)
    }

    fun cancel() {
        pollRunnable?.let { handler.removeCallbacks(it) }
        pollRunnable = null
    }
}
