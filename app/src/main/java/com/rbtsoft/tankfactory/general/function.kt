package com.rbtsoft.tankfactory.general

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.rbtsoft.tankfactory.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream

suspend fun saveImageToDownload(
    context: Context,
    bitmap: Bitmap,
    filename: String,
) {
    var success = false
    val mimeType = "image/webp"

    try {
        withContext(Dispatchers.IO) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val bufferSize = 32768
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    BufferedOutputStream(os, bufferSize).use { bufferedStream ->
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, bufferedStream)
                        success = true
                    }
                }
            }
        }

        withContext(Dispatchers.Main) {
            if (success) {
                Toast.makeText(
                    context,
                    context.getString(R.string.save_success),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                context.getString(R.string.save_failed, e.message),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
