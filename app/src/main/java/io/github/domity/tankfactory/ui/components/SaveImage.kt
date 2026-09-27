package io.github.domity.tankfactory.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import io.github.domity.tankfactory.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream

enum class ImageFormat {
    PNG, WEBP
}

private data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

suspend fun saveImageToDownload(
    context: Context,
    bitmap: Bitmap,
    filename: String,
    format: ImageFormat = ImageFormat.WEBP
) {
    var success = false
    val (mimeType, compressFormat, extension, quality) = when (format) {
        ImageFormat.PNG -> Quadruple("image/png", Bitmap.CompressFormat.PNG, ".png", 0)
        ImageFormat.WEBP -> Quadruple("image/webp", Bitmap.CompressFormat.WEBP_LOSSLESS, ".webp", 0)
    }
    val finalFilename = if (filename.endsWith(".png") || filename.endsWith(".webp")) {
        filename.substringBeforeLast('.') + extension
    } else {
        filename + extension
    }

    try {
        withContext(Dispatchers.IO) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalFilename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val bufferSize = 65536
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    BufferedOutputStream(os, bufferSize).use { bufferedStream ->
                        bitmap.compress(compressFormat, quality, bufferedStream)
                        bufferedStream.flush()
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
