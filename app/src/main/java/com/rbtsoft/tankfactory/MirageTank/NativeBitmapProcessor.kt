package com.rbtsoft.tankfactory.miragetank

import android.graphics.Bitmap

object MiragetankCoder {

    init {
        System.loadLibrary("tankfactory")
    }

    external fun encodeNative(
        bitmap1: Bitmap,
        bitmap2: Bitmap,
        outputBitmap: Bitmap,
        photo1K: Float,
        photo2K: Float,
        threshold: Int
    )
}
