package com.agent.android.agent.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class FileAccessController(private val context: Context?) {

    fun getFileAccessStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("FILE_ACCESS", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        val writeGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val readGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        val msg = "Storage Access: READ=${if (readGranted) "GRANTED" else "DENIED"}, WRITE=${if (writeGranted) "GRANTED" else "DENIED"}"
        return SkillResult("FILE_ACCESS", SkillStatus.SUCCESS, msg, System.currentTimeMillis() - start)
    }
}
