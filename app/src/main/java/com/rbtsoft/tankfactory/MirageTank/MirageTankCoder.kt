package com.rbtsoft.tankfactory.miragetank

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import kotlin.math.max

object MirageTankCoder {

    init {
        System.loadLibrary("tankfactory")
    }

    fun encode(photo1: Bitmap, photo2: Bitmap, photo1K: Float, photo2K: Float, threshold: Int): Bitmap {
        val width = max(photo1.width, photo2.width)
        val height = max(photo1.height, photo2.height)
        val outputBitmap = createBitmap(width, height)

        encodeNative(
            photo1,
            photo2,
            outputBitmap,
            photo1K,
            photo2K,
            threshold
        )

        return outputBitmap
    }

    private external fun encodeNative(
        bitmap1: Bitmap,
        bitmap2: Bitmap,
        outputBitmap: Bitmap,
        photo1K: Float,
        photo2K: Float,
        threshold: Int
    )
}
