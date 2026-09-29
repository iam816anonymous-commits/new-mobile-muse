package com.agent.android.agent.skills.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class AppLauncherImpl(private val context: Context?) : AppLauncher {

    override fun listInstalledLaunchableApps(): List<AppDescriptor> {
        if (context == null) return emptyList()
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos.map { info ->
            AppDescriptor(
                packageName = info.activityInfo.packageName,
                applicationLabel = info.loadLabel(pm).toString(),
                launchable = true,
                launcherActivity = info.activityInfo.name
            )
        }.distinctBy { it.packageName }
    }

    override fun findApp(query: String): List<AppDescriptor> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return emptyList()

        val allApps = listInstalledLaunchableApps()

        // 1. Exact package name match
        val packageMatch = allApps.filter { it.packageName.equals(cleanQuery, ignoreCase = true) }
        if (packageMatch.isNotEmpty()) return packageMatch

        // 2. Exact label match (case-insensitive)
        val exactLabelMatch = allApps.filter { it.applicationLabel.equals(cleanQuery, ignoreCase = true) }
        if (exactLabelMatch.isNotEmpty()) return exactLabelMatch

        // 3. Substring label match
        return allApps.filter { it.applicationLabel.contains(cleanQuery, ignoreCase = true) }
    }

    override fun validateApp(query: String): AppLaunchResult {
        val start = System.currentTimeMillis()
        val clean = query.trim()
        if (clean.isEmpty()) {
            return AppLaunchResult(
                requestedApp = query,
                resolvedPackage = null,
                resolvedLabel = null,
                launcherActivity = null,
                status = AppLaunchStatus.INVALID_ARGUMENT,
                errorCode = "INVALID_ARGUMENT",
                message = "Requested app query cannot be empty",
                durationMs = System.currentTimeMillis() - start
            )
        }

        if (context == null) {
            return AppLaunchResult(
                requestedApp = query,
                resolvedPackage = null,
                resolvedLabel = null,
                launcherActivity = null,
                status = AppLaunchStatus.UNAVAILABLE,
                errorCode = "NO_CONTEXT",
                message = "Context unavailable for app validation",
                durationMs = System.currentTimeMillis() - start
            )
        }

        val matches = findApp(clean)
        if (matches.isEmpty()) {
            return AppLaunchResult(
                requestedApp = query,
                resolvedPackage = null,
                resolvedLabel = null,
                launcherActivity = null,
                status = AppLaunchStatus.APP_NOT_FOUND,
                errorCode = "APP_NOT_FOUND",
                message = "No installed app matches '$query'",
                durationMs = System.currentTimeMillis() - start
            )
        }

        val exactMatches = matches.filter {
            it.packageName.equals(clean, ignoreCase = true) || it.applicationLabel.equals(clean, ignoreCase = true)
        }

        if (exactMatches.size > 1) {
            val packagesStr = exactMatches.joinToString(", ") { "${it.applicationLabel} (${it.packageName})" }
            return AppLaunchResult(
                requestedApp = query,
                resolvedPackage = null,
                resolvedLabel = null,
                launcherActivity = null,
                status = AppLaunchStatus.AMBIGUOUS_APP,
                errorCode = "AMBIGUOUS_APP",
                message = "Multiple apps match '$query': $packagesStr",
                durationMs = System.currentTimeMillis() - start
            )
        }

        val selected = if (exactMatches.size == 1) exactMatches.first() else matches.first()
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(selected.packageName)

        if (launchIntent == null) {
            return AppLaunchResult(
                requestedApp = query,
                resolvedPackage = selected.packageName,
                resolvedLabel = selected.applicationLabel,
                launcherActivity = selected.launcherActivity,
                status = AppLaunchStatus.APP_NOT_LAUNCHABLE,
                errorCode = "APP_NOT_LAUNCHABLE",
                message = "App '${selected.applicationLabel}' has no launchable activity",
                durationMs = System.currentTimeMillis() - start
            )
        }

        return AppLaunchResult(
            requestedApp = query,
            resolvedPackage = selected.packageName,
            resolvedLabel = selected.applicationLabel,
            launcherActivity = selected.launcherActivity,
            status = AppLaunchStatus.SUCCESS,
            message = "App '${selected.applicationLabel}' validated successfully",
            durationMs = System.currentTimeMillis() - start
        )
    }

    override fun launchApp(query: String): AppLaunchResult {
        val start = System.currentTimeMillis()
        val validation = validateApp(query)
        if (validation.status != AppLaunchStatus.SUCCESS) {
            return validation
        }

        if (context == null || validation.resolvedPackage == null) {
            return AppLaunchResult(
                requestedApp = query,
                resolvedPackage = validation.resolvedPackage,
                resolvedLabel = validation.resolvedLabel,
                launcherActivity = validation.launcherActivity,
                status = AppLaunchStatus.LAUNCH_FAILED,
                errorCode = "NO_CONTEXT",
                message = "Context or resolved package unavailable",
                durationMs = System.currentTimeMillis() - start
            )
        }

        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(validation.resolvedPackage)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent == null) {
                return AppLaunchResult(
                    requestedApp = query,
                    resolvedPackage = validation.resolvedPackage,
                    resolvedLabel = validation.resolvedLabel,
                    launcherActivity = validation.launcherActivity,
                    status = AppLaunchStatus.APP_NOT_LAUNCHABLE,
                    errorCode = "APP_NOT_LAUNCHABLE",
                    message = "Launch intent is null",
                    durationMs = System.currentTimeMillis() - start
                )
            }

            context.startActivity(launchIntent)
            AppLaunchResult(
                requestedApp = query,
                resolvedPackage = validation.resolvedPackage,
                resolvedLabel = validation.resolvedLabel,
                launcherActivity = validation.launcherActivity,
                status = AppLaunchStatus.SUCCESS,
                message = "App '${validation.resolvedLabel}' launch intent dispatched",
                durationMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            AppLaunchResult(
                requestedApp = query,
                resolvedPackage = validation.resolvedPackage,
                resolvedLabel = validation.resolvedLabel,
                launcherActivity = validation.launcherActivity,
                status = AppLaunchStatus.LAUNCH_FAILED,
                errorCode = "LAUNCH_FAILED",
                message = "Error launching app: ${e.message}",
                durationMs = System.currentTimeMillis() - start
            )
        }
    }
}
