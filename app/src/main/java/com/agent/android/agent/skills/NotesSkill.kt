package com.agent.android.agent.skills

import android.content.Context
import android.os.Environment
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

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val formattedLine = "[$timestamp] $content\n"

        return try {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LocalAgent")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, "notes.txt")
            FileWriter(file, true).use { writer ->
                writer.write(formattedLine)
            }
            SkillResult("NOTE", SkillStatus.SUCCESS, "Note persisted to ${file.name}", System.currentTimeMillis() - startTime)
        } catch (e: SecurityException) {
            SkillResult("NOTE", SkillStatus.PERMISSION_REQUIRED, "Storage permission required to write notes", System.currentTimeMillis() - startTime, "PERMISSION_DENIED")
        } catch (e: Exception) {
            SkillResult("NOTE", SkillStatus.FAILED, "I/O Error: ${e.message}", System.currentTimeMillis() - startTime, "IO_ERROR")
        }
    }
}
