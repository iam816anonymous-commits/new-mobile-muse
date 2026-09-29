package com.agent.android.agent.skills

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesSkill(private val context: Context? = null) {

    fun addNote(content: String): SkillResult {
        val startTime = System.currentTimeMillis()
        if (content.isBlank()) {
            return SkillResult("NOTE", SkillStatus.FAILED, "Empty note content", System.currentTimeMillis() - startTime, "EMPTY_NOTE")
        }

        if (context != null) {
            val permCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (permCheck != PackageManager.PERMISSION_GRANTED) {
                return SkillResult(
                    "NOTE",
                    SkillStatus.PERMISSION_REQUIRED,
                    "WRITE_EXTERNAL_STORAGE permission required to persist notes",
                    System.currentTimeMillis() - startTime,
                    "PERMISSION_REQUIRED"
                )
            }
        }

        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED != state) {
            return SkillResult("NOTE", SkillStatus.UNAVAILABLE, "External storage unavailable ($state)", System.currentTimeMillis() - startTime, "STORAGE_UNAVAILABLE")
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val formattedLine = "[$timestamp] $content\n"

        return try {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LocalAgent")
            if (!dir.exists() && !dir.mkdirs() && !dir.isDirectory) {
                return SkillResult("NOTE", SkillStatus.FAILED, "Failed to create directory Download/LocalAgent", System.currentTimeMillis() - startTime, "DIRECTORY_CREATION_FAILED")
            }
            val file = File(dir, "notes.txt")
            FileWriter(file, true).use { writer ->
                writer.write(formattedLine)
            }
            SkillResult("NOTE", SkillStatus.SUCCESS, "Note persisted to Download/LocalAgent/notes.txt", System.currentTimeMillis() - startTime)
        } catch (e: SecurityException) {
            SkillResult("NOTE", SkillStatus.PERMISSION_REQUIRED, "Storage permission denied by OS: ${e.message}", System.currentTimeMillis() - startTime, "PERMISSION_REQUIRED")
        } catch (e: Exception) {
            SkillResult("NOTE", SkillStatus.FAILED, "I/O Error writing note: ${e.message}", System.currentTimeMillis() - startTime, "IO_ERROR")
        }
    }
}
