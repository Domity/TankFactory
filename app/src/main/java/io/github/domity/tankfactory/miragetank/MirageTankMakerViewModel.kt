package io.github.domity.tankfactory.miragetank

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.core.graphics.createBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.domity.tankfactory.R
import io.github.domity.tankfactory.ui.components.saveImageToDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import androidx.core.graphics.scale

class MirageTankMakerViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedImage1Uri = MutableStateFlow<Uri?>(null)
    val selectedImage1Uri: StateFlow<Uri?> = _selectedImage1Uri

    private val _selectedImage2Uri = MutableStateFlow<Uri?>(null)
    val selectedImage2Uri: StateFlow<Uri?> = _selectedImage2Uri

    private val previewSize = 600
    private var previewBmp1: Bitmap? = null
    private var previewBmp2: Bitmap? = null
    var previewOutputBmp: Bitmap? = null

    private var lastParams: PreviewParams? = null
    private val workMutex = Mutex()
    private var bitmapVersion = 0L
    private var latestEncodeRequest = 0L

    private data class PreviewParams(
        val photo1K: Float,
        val photo2K: Float,
        val threshold: Int,
        val colorMode: Boolean,
        val scaleInner: Float,
        val scaleCover: Float,
        val desatInner: Float,
        val desatCover: Float,
        val weightInner: Float,
        val grayR: Float,
        val grayG: Float,
        val grayB: Float
    )

    private val defaultParams = PreviewParams(
        photo1K = 1.0f, photo2K = 1.0f, threshold = 127, colorMode = false,
        scaleInner = 0.3f, scaleCover = 0.2f,
        desatInner = 0.0f, desatCover = 0.0f, weightInner = 0.7f,
        grayR = 0.299f, grayG = 0.587f, grayB = 0.114f
    )

    private val _previewTrigger = MutableStateFlow(0)
    val previewTrigger: StateFlow<Int> = _previewTrigger

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

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
        invalidatePreviews()
        viewModelScope.launch(Dispatchers.IO) {
            workMutex.withLock { recycleBitmapsNow() }
        }
    }

    private fun invalidatePreviews() {
        bitmapVersion++
        latestEncodeRequest++
    }

    private fun recycleBitmapsNow() {
        previewBmp1?.recycle()
        previewBmp2?.recycle()
        previewOutputBmp?.recycle()
        previewBmp1 = null
        previewBmp2 = null
        previewOutputBmp = null
    }

    override fun onCleared() {
        super.onCleared()
        invalidatePreviews()
        runBlocking {
            withTimeoutOrNull(2000) {
                workMutex.withLock { recycleBitmapsNow() }
            }
        }
    }

    private fun preparePreviewEnvironment() {
        val gen = ++bitmapVersion
        viewModelScope.launch(Dispatchers.IO) {
            workMutex.withLock {
                if (gen != bitmapVersion) return@withLock
                val uri1 = _selectedImage1Uri.value ?: return@withLock
                val uri2 = _selectedImage2Uri.value ?: return@withLock

                val app = getApplication<Application>()

                val temp1 = decodeSampledBitmapFromUri(app, uri1, previewSize, previewSize) ?: return@withLock
                val temp2 = decodeSampledBitmapFromUri(app, uri2, previewSize, previewSize) ?: return@withLock

                val targetW = max(temp1.width, temp2.width)
                val targetH = max(temp1.height, temp2.height)

                val newBmp1 = scaleAndCenterBitmap(temp1, targetW, targetH)
                val newBmp2 = scaleAndCenterBitmap(temp2, targetW, targetH)

                temp1.recycle()
                temp2.recycle()

                recycleBitmapsNow()

                previewBmp1 = newBmp1
                previewBmp2 = newBmp2
                previewOutputBmp = createBitmap(targetW, targetH)

                encodeWithCurrentBitmaps(lastParams ?: defaultParams)
            }
        }
    }

    private fun scaleAndCenterBitmap(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        if (source.width == targetW && source.height == targetH) {
            return source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val scaleW = targetW.toFloat() / source.width
        val scaleH = targetH.toFloat() / source.height
        val scale = minOf(scaleW, scaleH)

        val scaledW = (source.width * scale).toInt()
        val scaledH = (source.height * scale).toInt()

        val offsetX = (targetW - scaledW) / 2f
        val offsetY = (targetH - scaledH) / 2f

        val result = createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.BLACK)
        
        val scaledBitmap = source.scale(scaledW, scaledH)
        canvas.drawBitmap(scaledBitmap, offsetX, offsetY, null)
        scaledBitmap.recycle()

        return result
    }

    fun updatePreview(photo1K: Float, photo2K: Float, threshold: Int, colorMode: Boolean,
                      scaleInner: Float, scaleCover: Float, 
                      desatInner: Float, desatCover: Float, weightInner: Float,
                      grayR: Float, grayG: Float, grayB: Float) {
        val params = PreviewParams(photo1K, photo2K, threshold, colorMode,
            scaleInner, scaleCover, desatInner, desatCover, weightInner, grayR, grayG, grayB)
        lastParams = params
        updatePreviewInternal(params)
    }

    private fun updatePreviewInternal(params: PreviewParams) {
        if (previewBmp1 == null || previewBmp2 == null || previewOutputBmp == null) return

        val request = ++latestEncodeRequest
        val gen = bitmapVersion

        viewModelScope.launch(Dispatchers.Default) {
            workMutex.withLock {
                if (gen != bitmapVersion) return@withLock
                if (request != latestEncodeRequest) return@withLock
                encodeWithCurrentBitmaps(params)
            }
        }
    }

    private fun encodeWithCurrentBitmaps(params: PreviewParams) {
        val p1 = previewBmp1 ?: return
        val p2 = previewBmp2 ?: return
        val out = previewOutputBmp ?: return

        if (params.colorMode) {
            MirageTankCoder.encodeColorNative(p1, p2, out,
                params.scaleInner, params.scaleCover, params.desatInner, params.desatCover,
                params.weightInner, params.grayR, params.grayG, params.grayB)
        } else {
            MirageTankCoder.encodeGrayNative(p1, p2, out,
                params.photo1K, params.photo2K, params.threshold,
                params.grayR, params.grayG, params.grayB)
        }
        _previewTrigger.value++
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

    fun saveMirageTank(photo1K: Float, photo2K: Float, threshold: Int, colorMode: Boolean,
                       scaleInner: Float, scaleCover: Float,
                       desatInner: Float, desatCover: Float, weightInner: Float,
                       grayR: Float, grayG: Float, grayB: Float, format: io.github.domity.tankfactory.ui.components.ImageFormat) {
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

                    val scaledPhoto1 = scaleAndCenterBitmap(photo1, targetW, targetH)
                    val scaledPhoto2 = scaleAndCenterBitmap(photo2, targetW, targetH)

                    if (scaledPhoto1 !== photo1) photo1.recycle()
                    if (scaledPhoto2 !== photo2) photo2.recycle()

                    val out = createBitmap(targetW, targetH)

                    if (colorMode) {
                        MirageTankCoder.encodeColorNative(scaledPhoto1, scaledPhoto2, out,
                            scaleInner, scaleCover, desatInner, desatCover, weightInner, grayR, grayG, grayB)
                    } else {
                        MirageTankCoder.encodeGrayNative(scaledPhoto1, scaledPhoto2, out, photo1K, photo2K, threshold, grayR, grayG, grayB)
                    }

                    scaledPhoto1.recycle()
                    scaledPhoto2.recycle()
                    out
                }

                if (largeBitmap != null) {
                    saveImageToDownload(
                        context = app,
                        bitmap = largeBitmap,
                        filename = "MirageTank_${System.currentTimeMillis()}",
                        format = format
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
