package com.rbtsoft.tankfactory.lsbtank

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rbtsoft.tankfactory.general.saveImageToDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max


class LSBTankMakerViewModel(application: Application) : AndroidViewModel(application) {
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

    //  大于此值则不进行预览直接保存
    private val maxSafePixels = 50000000

    //  大于此值则加载较小的位图副本到预览界面
    private val maxDisplayDimension = 2500

    fun setImage1Uri(uri: Uri) {
        _selectedImage1Uri.value = uri
    }
    fun setImage2Uri(uri: Uri) {
        _selectedImage2Uri.value = uri
    }
    fun onMakerScreenEntered() {
        _selectedImage1Uri.value = null
        _selectedImage2Uri.value = null
        _displayBitmap.value = null
        originalResultBitmap = null
        _isResultTooLarge.value = false
    }
    fun onPressMakerButton(){
        _displayBitmap.value = null
        originalResultBitmap = null
        _isResultTooLarge.value = false
    }
    @SuppressLint("StringFormatInvalid")
    fun saveImageToDownload() {
        val bitmapToSave = originalResultBitmap ?: return
        _isSaving.value = true
        viewModelScope.launch {
            saveImageToDownload(
                context = getApplication(),
                bitmap = bitmapToSave,
                filename = "LSBTank_${System.currentTimeMillis()}.webp"
            )
            _isSaving.value = false
        }
    }

    fun generateLSBTank(compress: Int) {
        val uri1 = _selectedImage1Uri.value ?: return
        val uri2 = _selectedImage2Uri.value ?: return
        _isGenerating.value=true
        originalResultBitmap = null
        _displayBitmap.value = null
        _isResultTooLarge.value = false
        viewModelScope.launch {
            val app = getApplication<Application>()
            val largeBitmap = withContext(Dispatchers.IO) {
                val photo1 =
                    app.contentResolver.openInputStream(uri1)?.use {
                        BitmapFactory.decodeStream(it)
                    } ?: return@withContext null
                val photo2 =
                    app.contentResolver.openInputStream(uri2)?.use {
                        BitmapFactory.decodeStream(it)
                    } ?: return@withContext null
                val lsbTank = LsbTankCoder.encode(photo1, photo2, compress)
                photo1.recycle()
                photo2.recycle()
                lsbTank
            }
            _isGenerating.value=false

            if (largeBitmap == null) return@launch
            originalResultBitmap = largeBitmap

            val pixelCount = largeBitmap.width * largeBitmap.height

            if (pixelCount > maxSafePixels) {
                _isResultTooLarge.value = true
                _displayBitmap.value = null
                saveImageToDownload()
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
}
