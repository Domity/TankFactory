package com.rbtsoft.tankfactory.MirageTank

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
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
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.max

class MirageTankMainViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedImage1Uri = MutableStateFlow<Uri?>(null)
    val selectedImage1Uri: StateFlow<Uri?> = _selectedImage1Uri

    private val _selectedImage2Uri = MutableStateFlow<Uri?>(null)
    val selectedImage2Uri: StateFlow<Uri?> = _selectedImage2Uri

    private var originalResultBitmap: Bitmap? = null

    private val _displayBitmap = MutableStateFlow<Bitmap?>(null)
    val displayBitmap: StateFlow<Bitmap?> = _displayBitmap

    private val _isResultTooLarge = MutableStateFlow(false)
    val isResultTooLarge: StateFlow<Boolean> = _isResultTooLarge

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val MAX_SAFE_PIXELS = 30000000

    private val MAX_DISPLAY_DIMENSION = 2500

    fun setImage1Uri(uri: Uri) { _selectedImage1Uri.value = uri }
    fun setImage2Uri(uri: Uri) { _selectedImage2Uri.value = uri }

    fun onMakerScreenEntered() {
        _selectedImage1Uri.value = null
        _selectedImage2Uri.value = null
        originalResultBitmap = null
        _displayBitmap.value = null
        _isResultTooLarge.value = false
    }
    fun onPressMakerButton(){
        originalResultBitmap = null
        _displayBitmap.value = null
        _isResultTooLarge.value = false
    }

    fun generateMirageTank(photo1K: Float, photo2K: Float, threshold: Int) {
        val uri1 = _selectedImage1Uri.value ?: return
        val uri2 = _selectedImage2Uri.value ?: return

        originalResultBitmap = null
        _displayBitmap.value = null
        _isResultTooLarge.value = false
        _isGenerating.value = true

        viewModelScope.launch {
            val app = getApplication<Application>()

            val largeBitmap: Bitmap? = withContext(Dispatchers.Default) {
                val photo1 = app.contentResolver.openInputStream(uri1)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: return@withContext null

                val photo2 = app.contentResolver.openInputStream(uri2)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: return@withContext null

                MirageTankEncoder.encode(photo1, photo2, photo1K, photo2K, threshold)
            }

            _isGenerating.value = false

            if (largeBitmap == null) return@launch
            originalResultBitmap = largeBitmap
            val pixelCount = largeBitmap.width * largeBitmap.height
            if (pixelCount > MAX_SAFE_PIXELS) {
                _isResultTooLarge.value = true
                saveImageToDownloads()
            } else {
                val scaledBitmap = scaleForDisplay(largeBitmap)
                _displayBitmap.value = scaledBitmap
            }
        }
    }

    private fun scaleForDisplay(bitmap: Bitmap): Bitmap {
        val currentWidth = bitmap.width
        val currentHeight = bitmap.height
        val maxDimension = max(currentWidth, currentHeight)

        if (maxDimension <= MAX_DISPLAY_DIMENSION) {
            return bitmap.scale(currentWidth, currentHeight)
        }

        val scaleFactor = MAX_DISPLAY_DIMENSION.toFloat() / maxDimension
        val newWidth = (currentWidth * scaleFactor).toInt()
        val newHeight = (currentHeight * scaleFactor).toInt()

        return bitmap.scale(newWidth, newHeight)
    }

    fun saveImageToDownloads() {
        val bitmapToSave = originalResultBitmap
        if (bitmapToSave == null) {
            return
        }

        _isSaving.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val app = getApplication<Application>()
            val filename = "MirageTank_${System.currentTimeMillis()}.png"
            var outputStream: OutputStream? = null
            var success = false

            try {
                outputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let {
                        app.contentResolver.openOutputStream(it)
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    downloadsDir.mkdirs()
                    val imageFile = File(downloadsDir, filename)
                    FileOutputStream(imageFile)
                }

                outputStream?.use { os ->
                    val bufferedStream = BufferedOutputStream(os, 8192)
                    bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, os)
                    bufferedStream.flush()
                    success = true
                }

                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(
                            app,
                            app.getString(R.string.mirage_tank_main_view_model_image_saved),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        app,
                        app.getString(R.string.mirage_tank_main_view_model_save_failed, e.message),
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