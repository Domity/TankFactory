package com.rbtsoft.tankfactory.lsbtank

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.core.graphics.scale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rbtsoft.tankfactory.R
import com.rbtsoft.tankfactory.ui.components.saveImageToDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class LSBTankViewerViewModel(application: Application) : AndroidViewModel(application) {
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    private var originalResultBitmap: Bitmap? = null

    private val _displayBitmap = MutableStateFlow<Bitmap?>(null)
    val displayBitmap: StateFlow<Bitmap?> = _displayBitmap

    private val _isResultTooLarge = MutableStateFlow(false)
    val isResultTooLarge: StateFlow<Boolean> = _isResultTooLarge

    private val _isDecoding = MutableStateFlow(false)
    val isDecoding: StateFlow<Boolean> = _isDecoding

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val maxSafePixels = 16_000_000
    private val maxDisplayDimension = 1080

    fun setImageUri(uri: Uri) {
        _selectedImageUri.value = uri
        clearBitmaps()
    }

    fun onPressDecoderButton(){
        clearBitmaps()
    }

    private fun clearBitmaps() {
        _displayBitmap.value = null
        originalResultBitmap?.recycle()
        originalResultBitmap = null
        _isResultTooLarge.value = false
    }

    fun decodeLSBTank() {
        val uri = _selectedImageUri.value ?: return
        clearBitmaps()
        _isDecoding.value = true

        viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                val largeBitmap: Bitmap? = withContext(Dispatchers.Default) {
                    val tankBitmap = app.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                    if (tankBitmap != null) {
                        val decoded = LsbTankCoder.decode(tankBitmap)
                        tankBitmap.recycle()
                        decoded
                    } else null
                }

                if (largeBitmap != null) {
                    originalResultBitmap = largeBitmap
                    val pixelCount = largeBitmap.width * largeBitmap.height

                    if (pixelCount > maxSafePixels) {
                        _isResultTooLarge.value = true
                    } else {
                        val scaledBitmap = withContext(Dispatchers.Default) {
                            scaleForDisplay(largeBitmap)
                        }
                        _displayBitmap.value = scaledBitmap
                    }
                }
            } catch (_: OutOfMemoryError) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(app, app.getString(R.string.image_too_large), Toast.LENGTH_LONG).show()
                }
            } finally {
                _isDecoding.value = false
            }
        }
    }

    private fun scaleForDisplay(bitmap: Bitmap): Bitmap {
        val currentWidth = bitmap.width
        val currentHeight = bitmap.height
        val maxDimension = max(currentWidth, currentHeight)

        if (maxDimension <= maxDisplayDimension) return bitmap

        val scaleFactor = maxDisplayDimension.toFloat() / maxDimension
        val newWidth = (currentWidth * scaleFactor).toInt()
        val newHeight = (currentHeight * scaleFactor).toInt()

        return bitmap.scale(newWidth, newHeight)
    }

    fun saveImageToDownload() {
        val bitmapToSave = originalResultBitmap ?: return
        _isSaving.value = true
        viewModelScope.launch {
            saveImageToDownload(
                context = getApplication(),
                bitmap = bitmapToSave,
                filename = "LSB_Decoded_${System.currentTimeMillis()}.webp"
            )
            _isSaving.value = false
        }
    }
}
