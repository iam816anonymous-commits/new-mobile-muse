package com.agent.android.test.evidence

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import com.agent.android.test.model.TestCase
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EvidenceRecord(
    val testId: String,
    val timestamp: Long,
    val resultJsonPath: String?,
    val imagePaths: List<String>,
    val evidenceStatus: String
)

class EvidenceManager(private val context: Context) {

    private val baseDir: File
        get() = File(context.filesDir, "evidence")

    companion object {
        const val EVIDENCE_UNAVAILABLE = "EVIDENCE_UNAVAILABLE"
        private const val MAX_EVIDENCE_DIRECTORIES = 20
    }

    init {
        cleanupOldEvidence()
    }

    private fun getTestDirectory(testId: String): File {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val phaseSubdir = if (testId.startsWith("P3.1-XAPP")) "phase3.1/xapp" else if (testId.startsWith("P3.1")) "phase3.1" else if (testId.startsWith("2.5")) "phase2.5" else "phase2"
        val testDir = File(baseDir, "$phaseSubdir/$today/TEST-$testId")
        if (!testDir.exists()) {
            testDir.mkdirs()
        }
        return testDir
    }

    fun captureViewScreenshot(
        activity: Activity?,
        testId: String,
        label: String,
        onComplete: (String) -> Unit
    ) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onComplete(EVIDENCE_UNAVAILABLE)
            return
        }

        val testDir = getTestDirectory(testId)
        val imageFile = File(testDir, "$label.png")

        try {
            val window = activity.window
            val view = window.decorView.rootView

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val bitmap = Bitmap.createBitmap(
                    view.width.coerceAtLeast(1),
                    view.height.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                PixelCopy.request(
                    window,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            val saved = saveBitmapToFile(bitmap, imageFile)
                            bitmap.recycle()
                            onComplete(if (saved) imageFile.absolutePath else EVIDENCE_UNAVAILABLE)
                        } else {
                            bitmap.recycle()
                            fallbackViewDraw(view, imageFile, onComplete)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } else {
                fallbackViewDraw(view, imageFile, onComplete)
            }
        } catch (e: Exception) {
            onComplete(EVIDENCE_UNAVAILABLE)
        }
    }

    private fun fallbackViewDraw(view: View, file: File, onComplete: (String) -> Unit) {
        try {
            val bitmap = Bitmap.createBitmap(
                view.width.coerceAtLeast(1),
                view.height.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            val saved = saveBitmapToFile(bitmap, file)
            bitmap.recycle()
            onComplete(if (saved) file.absolutePath else EVIDENCE_UNAVAILABLE)
        } catch (e: Exception) {
            onComplete(EVIDENCE_UNAVAILABLE)
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun saveTestResultJson(testCase: TestCase): String {
        val testDir = getTestDirectory(testCase.id)
        val resultFile = File(testDir, "result.json")
        try {
            val json = """
                {
                  "id": "${testCase.id}",
                  "phase": "${testCase.phase}",
                  "category": "${testCase.category}",
                  "name": "${escapeJson(testCase.name)}",
                  "command": "${escapeJson(testCase.command ?: "")}",
                  "status": "${testCase.status.name}",
                  "observedResult": "${escapeJson(testCase.observedResult ?: "")}",
                  "error": "${escapeJson(testCase.error ?: "")}",
                  "duration": ${testCase.duration ?: 0L},
                  "timestamp": ${testCase.timestamp ?: System.currentTimeMillis()}
                }
            """.trimIndent()
            resultFile.writeText(json)
            return resultFile.absolutePath
        } catch (e: Exception) {
            return EVIDENCE_UNAVAILABLE
        }
    }

    fun cleanupOldEvidence() {
        try {
            if (!baseDir.exists()) return
            val dateDirs = baseDir.listFiles() ?: return
            if (dateDirs.size > MAX_EVIDENCE_DIRECTORIES) {
                dateDirs.sortBy { it.lastModified() }
                val toDelete = dateDirs.size - MAX_EVIDENCE_DIRECTORIES
                for (i in 0 until toDelete) {
                    dateDirs[i].deleteRecursively()
                }
            }
        } catch (ignored: Exception) {}
    }

    fun clearAllEvidence() {
        try {
            if (baseDir.exists()) {
                baseDir.deleteRecursively()
            }
        } catch (ignored: Exception) {}
    }

    private fun escapeJson(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}
