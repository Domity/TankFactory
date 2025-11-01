package com.rbtsoft.tankfactory.lsbtank

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.graphics.scale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rbtsoft.tankfactory.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
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

    //  大于此值则不进行预览直接保存
    private val maxSafePixels = 50000000

    //  大于此值则加载较小的位图副本到预览界面
    private val maxDisplayDimension = 2500

    fun setImageUri(uri: Uri) {
        _selectedImageUri.value = uri
        _displayBitmap.value = null
        originalResultBitmap = null
        _isResultTooLarge.value = false
    }

    fun onPressDecoderButton(){
        _isDecoding.value = true
        _displayBitmap.value = null
        originalResultBitmap = null
        _isResultTooLarge.value = false
    }

    @SuppressLint("Recycle")
    fun decodeLSBTank() {
        val uri = _selectedImageUri.value ?: return
        _isDecoding.value = true
        originalResultBitmap = null
        _displayBitmap.value = null
        _isResultTooLarge.value = false

        viewModelScope.launch {
            val app = getApplication<Application>()
            val largeBitmap: Bitmap? = withContext(Dispatchers.Default) {
                val tankBitmap = try {
                    app.contentResolver.openInputStream(uri)
                        ?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }
                } catch (_: Exception) {
                    null
                }
                if (tankBitmap != null) {
                    LsbTankCoder.decode(tankBitmap)
                } else {
                    null
                }
            }
            _isDecoding.value = false

            if (largeBitmap == null) return@launch
            originalResultBitmap = largeBitmap

            val pixelCount = largeBitmap.width * largeBitmap.height

            if (pixelCount > maxSafePixels) {
                _isResultTooLarge.value = true
                _displayBitmap.value = null
                saveImageToDownloads()
            } else {
                _isResultTooLarge.value = false
                val scaledBitmap = scaleForDisplay(largeBitmap)
                _displayBitmap.value = scaledBitmap
            }
        }
    }

    private fun scaleForDisplay(bitmap: Bitmap): Bitmap {
        val currentWidth = bitmap.width
        val currentHeight = bitmap.height
        val maxDimension = max(currentWidth, currentHeight)

        if (maxDimension <= maxDisplayDimension) {
            return bitmap
        }

        val scaleFactor = maxDisplayDimension.toFloat() / maxDimension
        val newWidth = (currentWidth * scaleFactor).toInt()
        val newHeight = (currentHeight * scaleFactor).toInt()

        return bitmap.scale(newWidth, newHeight)
    }

    @SuppressLint("StringFormatInvalid")
    fun saveImageToDownloads() {
        val bitmapToSave = originalResultBitmap ?: return
        _isSaving.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val app = getApplication<Application>()
            val filename = "LSB_Decoded_${System.currentTimeMillis()}.webp"
            var success = false

            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/webp")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                    app.contentResolver.openOutputStream(uri)?.use { os ->
                        BufferedOutputStream(os).use { bufferedStream ->
                            bitmapToSave.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, bufferedStream)
                            success = true
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(
                            app,
                            app.getString(R.string.lsb_tank_viewer_view_model_image_saved),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        app,
                        app.getString(R.string.lsb_tank_viewer_view_model_save_failed, e.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isSaving.value = false
                }
            }
        }
    }
}
