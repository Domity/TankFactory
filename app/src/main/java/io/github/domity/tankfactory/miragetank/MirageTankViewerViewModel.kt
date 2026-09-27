package io.github.domity.tankfactory.miragetank

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.domity.tankfactory.ui.components.ImageFormat
import io.github.domity.tankfactory.ui.components.saveImageToDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MirageTankViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private var currentImageUri: Uri? = null
    private var cachedBitmap: Bitmap? = null

    fun setImageUri(uri: Uri) {
        currentImageUri = uri
        cachedBitmap?.recycle()
        cachedBitmap = null
    }

    fun saveImage(isDarkMode: Boolean, format: ImageFormat) {
        val uri = currentImageUri ?: return
        _isSaving.value = true

        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val result = withContext(Dispatchers.IO) {
                    val bitmap = app.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    } ?: return@withContext null

                    extractLayerWithBackground(bitmap, isDarkMode)
                }

                if (result != null) {
                    val layerName = if (isDarkMode) "Inside" else "Surface"
                    saveImageToDownload(
                        context = app,
                        bitmap = result,
                        filename = "MirageTank_${layerName}_${System.currentTimeMillis()}",
                        format = format
                    )
                    result.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun extractLayerWithBackground(bitmap: Bitmap, isDarkMode: Boolean): Bitmap {
        val output = createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val bgColor = if (isDarkMode) Color.BLACK else Color.WHITE
        canvas.drawColor(bgColor)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        bitmap.recycle()
        return output
    }

    override fun onCleared() {
        super.onCleared()
        cachedBitmap?.recycle()
    }
}
