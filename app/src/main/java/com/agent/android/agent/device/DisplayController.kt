package com.agent.android.agent.device

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

data class DisplaySnapshot(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val densityScale: Float,
    val rotation: Int,
    val orientation: String
)

class DisplayController(private val context: Context?) {

    fun getDisplaySnapshot(): DisplaySnapshot? {
        if (context == null) return null
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return null
            val display = wm.defaultDisplay
            val metrics = DisplayMetrics()
            display.getRealMetrics(metrics)

            val rot = display.rotation
            val orientStr = if (metrics.widthPixels < metrics.heightPixels) "PORTRAIT" else "LANDSCAPE"

            DisplaySnapshot(
                widthPx = metrics.widthPixels,
                heightPx = metrics.heightPixels,
                densityDpi = metrics.densityDpi,
                densityScale = metrics.density,
                rotation = rot,
                orientation = orientStr
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getDisplayStatus(): SkillResult {
        val start = System.currentTimeMillis()
        val snap = getDisplaySnapshot()
            ?: return SkillResult("DISPLAY", SkillStatus.UNAVAILABLE, "Context/WindowManager unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val msg = "DISPLAY: ${snap.widthPx}x${snap.heightPx} px | Density: ${snap.densityDpi} dpi (${snap.densityScale}x) | Orientation: ${snap.orientation} (Rotation: ${snap.rotation})"
        return SkillResult("DISPLAY", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }

    fun getDisplayDimensions(): SkillResult {
        val start = System.currentTimeMillis()
        val snap = getDisplaySnapshot()
            ?: return SkillResult("DISPLAY", SkillStatus.UNAVAILABLE, "Context/WindowManager unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val msg = "Dimensions: ${snap.widthPx} x ${snap.heightPx} pixels"
        return SkillResult("DISPLAY", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }

    fun getDisplayOrientation(): SkillResult {
        val start = System.currentTimeMillis()
        val snap = getDisplaySnapshot()
            ?: return SkillResult("DISPLAY", SkillStatus.UNAVAILABLE, "Context/WindowManager unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val msg = "Orientation: ${snap.orientation}"
        return SkillResult("DISPLAY", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }
}
