package com.rbtsoft.tankfactory

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.*
import kotlin.math.max
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

object MirageTankEncoder {

    suspend fun encode(photo1: Bitmap, photo2: Bitmap, photo1K: Float, photo2K: Float, threshold: Int): Bitmap = withContext(Dispatchers.Default) {

        val width = max(photo1.width, photo2.width)
        val height = max(photo1.height, photo2.height)

        val scaledPhoto1 = scaleBitmap(photo1, width, height)
        val scaledPhoto2 = scaleBitmap(photo2, width, height)

        val grayPhoto1 = toGrayscale(scaledPhoto1)
        val grayPhoto2 = toGrayscale(scaledPhoto2)

        val size = width * height
        val pixels1 = IntArray(size).apply {
            grayPhoto1.getPixels(this, 0, width, 0, 0, width, height)
        }
        val pixels2 = IntArray(size).apply {
            grayPhoto2.getPixels(this, 0, width, 0, 0, width, height)
        }
        val outputPixels = IntArray(size)

        val numCores = Runtime.getRuntime().availableProcessors()
        val totalPixels = width * height

        coroutineScope {
            val jobs = mutableListOf<Deferred<Unit>>()

            val chunkSize = totalPixels / numCores

            for (i in 0 until numCores) {
                val start = i * chunkSize
                // 最后一个块处理剩余的所有像素
                val end = if (i == numCores - 1) totalPixels else (i + 1) * chunkSize

                // 启动一个异步任务
                val job = async {
                    // 遍历此块中的所有像素索引
                    for (index in start until end) {

                        val pixel1 = pixels1[index]
                        val pixel2 = pixels2[index]

                        val grayValue1 = (Color.red(pixel1) * photo1K).toInt().coerceIn(threshold + 1, 254)
                        val grayValue2 = (Color.red(pixel2) * photo2K).toInt().coerceIn(1, threshold)

                        val alpha = 255 - (grayValue1 - grayValue2)
                        val safeAlpha = if (alpha == 0) 1 else alpha
                        val gray = (255f * grayValue2 / safeAlpha).toInt().coerceIn(0, 255)

                        outputPixels[index] = Color.argb(alpha, gray, gray, gray)

                    }
                }
                jobs.add(job)
            }

            jobs.awaitAll()
        }

        val outputBitmap = createBitmap(width, height)
        outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)

        return@withContext outputBitmap
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayBitmap = createBitmap(width, height)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val gray = (r * 0.299f + g * 0.587f + b * 0.114f).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(pixel), gray, gray, gray)
        }
        grayBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return grayBitmap
    }

    private fun scaleBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return bitmap.scale(newWidth, newHeight)
    }
}