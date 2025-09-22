package com.rbtsoft.tankfactory

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MirageTankMainViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedImage1Uri = MutableStateFlow<Uri?>(null)
    val selectedImage1Uri: StateFlow<Uri?> = _selectedImage1Uri

    private val _selectedImage2Uri = MutableStateFlow<Uri?>(null)
    val selectedImage2Uri: StateFlow<Uri?> = _selectedImage2Uri

    private val _encodedImage = MutableStateFlow<Bitmap?>(null)
    val encodedImage: StateFlow<Bitmap?> = _encodedImage

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    fun setImage1Uri(uri: Uri) {
        _selectedImage1Uri.value = uri
    }

    fun setImage2Uri(uri: Uri) {
        _selectedImage2Uri.value = uri
    }
    fun onMakerScreenEntered() {
        _selectedImage1Uri.value = null
        _selectedImage2Uri.value = null
        _encodedImage.value = null
    }
    fun onPressMakerButton(){
        _encodedImage.value = null
    }
    fun generateMirageTank(photo1K: Float, photo2K: Float, threshold: Int) {
        val uri1 = _selectedImage1Uri.value ?: return
        val uri2 = _selectedImage2Uri.value ?: return
        _isGenerating.value=true
        viewModelScope.launch {
            _encodedImage.value = withContext(Dispatchers.IO) {
                val photo1 = getApplication<Application>().contentResolver.openInputStream(uri1)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: return@withContext null

                val photo2 = getApplication<Application>().contentResolver.openInputStream(uri2)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: return@withContext null

                MirageTankEncoder.encode(photo1, photo2, photo1K, photo2K, threshold)
            }
            _isGenerating.value=false
        }
    }
    fun saveImageToDownloads(bitmap: Bitmap) {
        _isSaving.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val filename = "MirageTank_${System.currentTimeMillis()}.png"

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
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val image = File(imagesDir, filename)
                FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                withContext(Dispatchers.Main) {
                _isSaving.value=false

                Toast.makeText(app, "图片已保存到Downloads", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}