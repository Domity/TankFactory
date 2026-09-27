package io.github.domity.tankfactory.miragetank

import android.graphics.Bitmap

object MirageTankCoder {

    init {
        System.loadLibrary("tankfactory")
    }

    external fun encodeGrayNative(
        bitmap1: Bitmap,
        bitmap2: Bitmap,
        outputBitmap: Bitmap,
        photo1K: Float,
        photo2K: Float,
        threshold: Int,
        grayWeightR: Float,
        grayWeightG: Float,
        grayWeightB: Float
    )

    external fun encodeColorNative(
        bitmap1: Bitmap,
        bitmap2: Bitmap,
        outputBitmap: Bitmap,
        scaleInner: Float,
        scaleCover: Float,
        desatInner: Float,
        desatCover: Float,
        weightInner: Float,
        grayWeightR: Float,
        grayWeightG: Float,
        grayWeightB: Float
    )
}
