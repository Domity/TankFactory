package com.rbtsoft.tankfactory.lsbtank

import android.graphics.Bitmap

object LsbTankCoder {

    init {
        System.loadLibrary("tankfactory")
    }

    private external fun encodeNative(surPic: Bitmap, insPic: Bitmap, compress: Int): Bitmap?
    private external fun decodeNative(tankPic: Bitmap): Bitmap?

    fun encode(surPic: Bitmap, insPic: Bitmap, compress: Int): Bitmap? {
        return encodeNative(surPic, insPic, compress)
    }

    fun decode(tankPic: Bitmap): Bitmap? {
        return decodeNative(tankPic)
    }
}
