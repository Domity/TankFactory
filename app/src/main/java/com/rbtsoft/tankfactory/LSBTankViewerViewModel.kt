package com.rbtsoft.tankfactory

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
class LSBTankViewerViewModel(application: Application) : AndroidViewModel(application) {
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    private val _decodedImage = MutableStateFlow<Bitmap?>(null)
    val decodedImage: StateFlow<Bitmap?> = _decodedImage

    private val _isDecoding = MutableStateFlow(false)
    val isDecoding: StateFlow<Boolean> = _isDecoding

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    fun setImageUri(uri: Uri) {
        _selectedImageUri.value = uri
        _decodedImage.value = null
    }

    fun onPressDecoderButton(){
        _isDecoding.value = true
        _decodedImage.value = null
    }

    @SuppressLint("Recycle")
    fun decodeLSBTank() {
        val uri = _selectedImageUri.value ?: return

        _isDecoding.value = true
        _decodedImage.value = null
        viewModelScope.launch {
            _decodedImage.value = withContext(Dispatchers.Default) {
                val tankBitmap = try {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                } catch (_: Exception) {
                    null
                }
                if (tankBitmap != null) {
                    LsbTankEncoder.decode(tankBitmap)
                } else {
                    null
                }
            }
            _isDecoding.value = false
        }
    }

    fun saveImageToDownloads(bitmap: Bitmap) {
        _isSaving.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val app = getApplication<Application>()
            val filename = "LSB_Decoded_${System.currentTimeMillis()}.png"
            val fos: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let {
                    app.contentResolver.openOutputStream(it)
                }
            } else {
                @Suppress("DEPRECATION")
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val image = File(imagesDir, filename)
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                FileOutputStream(image)
            }
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                withContext(Dispatchers.Main) {
                    _isSaving.value = false
                    Toast.makeText(app, app.getString(R.string.lsb_tank_viewer_view_model_image_saved), Toast.LENGTH_SHORT).show()
                }
            } ?: withContext(Dispatchers.Main) {
                _isSaving.value = false
                Toast.makeText(app, app.getString(R.string.lsb_tank_viewer_view_model_save_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
}