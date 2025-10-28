package com.rbtsoft.tankfactory.miragetank

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object MirageTankEncoder {
    //  具体的大小还需研究
    private const val PIXEL_THRESHOLD = 5000 * 5000

    suspend fun encode(photo1: Bitmap, photo2: Bitmap, photo1K: Float, photo2K: Float, threshold: Int): Bitmap =
        withContext(Dispatchers.Default) {

            val width = max(photo1.width, photo2.width)
            val height = max(photo1.height, photo2.height)

            val scaledPhoto1 = if (photo1.width != width || photo1.height != height) {
                photo1.scale(width, height)
            } else {
                photo1
            }
            val scaledPhoto2 = if (photo2.width != width || photo2.height != height) {
                photo2.scale(width, height)
            } else {
                photo2
            }
            val outputBitmap = createBitmap(width, height)

            if (width * height > PIXEL_THRESHOLD) {

                //  大图片使用C++加速计算(也许)

                NativeBitmapProcessor.encodeBitmaps(
                    scaledPhoto1,
                    scaledPhoto2,
                    outputBitmap,
                    photo1K,
                    photo2K,
                    threshold
                )
            } else {

                //  小图片使用C++会有反效果

                val totalPixels = width * height
                val pixels1 = IntArray(totalPixels)
                val pixels2 = IntArray(totalPixels)
                val outputPixels = IntArray(totalPixels)

                scaledPhoto1.getPixels(pixels1, 0, width, 0, 0, width, height)
                scaledPhoto2.getPixels(pixels2, 0, width, 0, 0, width, height)

                val numCores = Runtime.getRuntime().availableProcessors()
                val pixelsPerThread = totalPixels / numCores

                coroutineScope {
                    for (i in 0 until numCores) {
                        val startIndex = i * pixelsPerThread
                        val endIndex = if (i == numCores - 1) totalPixels else (i + 1) * pixelsPerThread
                        launch {
                            processInSinglePass(
                                pixels1,
                                pixels2,
                                outputPixels,
                                photo1K,
                                photo2K,
                                threshold,
                                startIndex,
                                endIndex
                            )
                        }
                    }
                }
                outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)
            }

            if (scaledPhoto1 !== photo1) {
                scaledPhoto1.recycle()
            }
            if (scaledPhoto2 !== photo2) {
                scaledPhoto2.recycle()
            }

            return@withContext outputBitmap
        }

    private fun toGray(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r * 0.299f + g * 0.587f + b * 0.114f).toInt()
    }

    private fun processInSinglePass(
        pixels1: IntArray,
        pixels2: IntArray,
        outputPixels: IntArray,
        photo1K: Float,
        photo2K: Float,
        threshold: Int,
        startIndex: Int,
        endIndex: Int
    ) {
        for (i in startIndex until endIndex) {
            val gray1 = toGray(pixels1[i])
            val gray2 = toGray(pixels2[i])

            val v1 = min(max((gray1 * photo1K).toInt(), threshold), 255)
            val v2 = min(max((gray2 * photo2K).toInt(), 0), threshold)
            val alpha = 255 - (v1 - v2)
            val safeAlpha = if (alpha == 0) 1 else alpha
            val gray = min(max((255.0f * v2 / safeAlpha).toInt(), 0), 255)

            outputPixels[i] = (alpha shl 24) or (gray shl 16) or (gray shl 8) or gray
        }
    }
}
