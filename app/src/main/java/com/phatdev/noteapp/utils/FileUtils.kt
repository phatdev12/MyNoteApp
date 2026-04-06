package com.phatdev.noteapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {

    fun getOutputMediaFile(context: Context): File? {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, "MyNoteApp").apply { mkdirs() }
        } ?: return null

        return File(mediaDir, "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")
    }

    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    fun getDownloadDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "Downloads").apply {
            if (!exists()) mkdirs()
        }
        return dir
    }
}
