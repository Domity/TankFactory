package com.rbtsoft.tankfactory.miragetank

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.widget.Toast
import androidx.core.graphics.createBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rbtsoft.tankfactory.R
import com.rbtsoft.tankfactory.ui.components.saveImageToDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class MirageTankMakerViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedImage1Uri = MutableStateFlow<Uri?>(null)
    val selectedImage1Uri: StateFlow<Uri?> = _selectedImage1Uri

    private val _selectedImage2Uri = MutableStateFlow<Uri?>(null)
    val selectedImage2Uri: StateFlow<Uri?> = _selectedImage2Uri

    private val previewSize = 600
    private var previewBmp1: Bitmap? = null
    private var previewBmp2: Bitmap? = null
    var previewOutputBmp: Bitmap? = null

    private val _previewTrigger = MutableStateFlow(0)
    val previewTrigger: StateFlow<Int> = _previewTrigger

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private var previewJob: Job? = null

    fun setImage1Uri(uri: Uri) {
        _selectedImage1Uri.value = uri
        preparePreviewEnvironment()
    }

    fun setImage2Uri(uri: Uri) {
        _selectedImage2Uri.value = uri
        preparePreviewEnvironment()
    }

    fun onMakerScreenEntered() {
        _selectedImage1Uri.value = null
        _selectedImage2Uri.value = null
        clearBitmaps()
    }

    private fun clearBitmaps() {
        previewBmp1?.recycle()
        previewBmp2?.recycle()
        previewOutputBmp?.recycle()
        previewBmp1 = null
        previewBmp2 = null
        previewOutputBmp = null
    }


    override fun onCleared() {
        super.onCleared()
        clearBitmaps()
    }

    private fun getOrReuseBitmap(currentBitmap: Bitmap?, width: Int, height: Int): Bitmap {
        return if (currentBitmap != null && currentBitmap.width == width && currentBitmap.height == height) {
            currentBitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            currentBitmap
        } else {
            currentBitmap?.recycle()
            createBitmap(width, height)
        }
    }

    private fun preparePreviewEnvironment() {
        val uri1 = _selectedImage1Uri.value ?: return
        val uri2 = _selectedImage2Uri.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()

            val temp1 = decodeSampledBitmapFromUri(app, uri1, previewSize, previewSize) ?: return@launch
            val temp2 = decodeSampledBitmapFromUri(app, uri2, previewSize, previewSize) ?: return@launch

            val targetW = max(temp1.width, temp2.width)
            val targetH = max(temp1.height, temp2.height)

            previewBmp1 = getOrReuseBitmap(previewBmp1, targetW, targetH).apply {
                Canvas(this).drawBitmap(temp1, 0f, 0f, null)
            }
            previewBmp2 = getOrReuseBitmap(previewBmp2, targetW, targetH).apply {
                Canvas(this).drawBitmap(temp2, 0f, 0f, null)
            }

            temp1.recycle()
            temp2.recycle()

            previewOutputBmp?.recycle()
            previewOutputBmp = createBitmap(targetW, targetH)

            updatePreview(1.0f, 1.0f, 127)
        }
    }

    fun updatePreview(photo1K: Float, photo2K: Float, threshold: Int) {
        val p1 = previewBmp1 ?: return
        val p2 = previewBmp2 ?: return
        val out = previewOutputBmp ?: return

        previewJob?.cancel()
        previewJob = viewModelScope.launch(Dispatchers.Default) {
            MirageTankCoder.encodeNative(p1, p2, out, photo1K, photo2K, threshold)
            _previewTrigger.value++
        }
    }

    private fun decodeSampledBitmapFromUri(app: Application, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        options.inMutable = true

        return app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun saveMirageTank(photo1K: Float, photo2K: Float, threshold: Int) {
        val uri1 = _selectedImage1Uri.value ?: return
        val uri2 = _selectedImage2Uri.value ?: return

        _isSaving.value = true

        viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                val largeBitmap: Bitmap? = withContext(Dispatchers.IO) {
                    val photo1 = app.contentResolver.openInputStream(uri1)?.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
                    val photo2 = app.contentResolver.openInputStream(uri2)?.use { BitmapFactory.decodeStream(it) } ?: return@withContext null

                    val targetW = max(photo1.width, photo2.width)
                    val targetH = max(photo1.height, photo2.height)
                    val out = createBitmap(targetW, targetH)

                    MirageTankCoder.encodeNative(photo1, photo2, out, photo1K, photo2K, threshold)

                    photo1.recycle()
                    photo2.recycle()
                    out
                }

                if (largeBitmap != null) {
                    saveImageToDownload(
                        context = app,
                        bitmap = largeBitmap,
                        filename = "MirageTank_${System.currentTimeMillis()}.webp"
                    )
                    largeBitmap.recycle()
                }
            } catch (_: OutOfMemoryError) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(app, app.getString(R.string.image_too_large), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
