package com.rbtsoft.tankfactory.MirageTank

import android.graphics.Bitmap

object NativeBitmapProcessor {

    init {
        System.loadLibrary("tankfactory")
    }

    external fun encodeBitmaps(
        bitmap1: Bitmap,
        bitmap2: Bitmap,
        outputBitmap: Bitmap,
        photo1K: Float,
        photo2K: Float,
        threshold: Int,
        startY: Int,
        endY: Int
    )
}
