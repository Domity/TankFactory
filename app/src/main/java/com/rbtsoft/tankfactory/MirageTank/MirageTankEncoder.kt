package com.rbtsoft.tankfactory.MirageTank

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object MirageTankEncoder {

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

            val numCores = Runtime.getRuntime().availableProcessors()
            val rowsPerThread = height / numCores

            coroutineScope {
                for (i in 0 until numCores) {
                    val startY = i * rowsPerThread
                    val endY = if (i == numCores - 1) height else (i + 1) * rowsPerThread
                    launch {
                        NativeBitmapProcessor.encodeBitmaps(
                            scaledPhoto1,
                            scaledPhoto2,
                            outputBitmap,
                            photo1K,
                            photo2K,
                            threshold,
                            startY,
                            endY
                        )
                    }
                }
            }

            if (scaledPhoto1 !== photo1) {
                scaledPhoto1.recycle()
            }
            if (scaledPhoto2 !== photo2) {
                scaledPhoto2.recycle()
            }

            return@withContext outputBitmap
        }
}
