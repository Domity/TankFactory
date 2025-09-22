package com.rbtsoft.tankfactory

import android.graphics.Bitmap
import android.graphics.Color

object MirageTankEncoder {

    fun encode(photo1: Bitmap, photo2: Bitmap, photo1K: Float, photo2K: Float, threshold: Int): Bitmap {
        //调整图像大小
        val width = maxOf(photo1.width, photo2.width)
        val height = maxOf(photo1.height, photo2.height)

        val scaledPhoto1 = scaleBitmap(photo1, width, height)
        val scaledPhoto2 = scaleBitmap(photo2, width, height)

        //将图片转换为灰度图
        val grayPhoto1 = toGrayscale(scaledPhoto1)
        val grayPhoto2 = toGrayscale(scaledPhoto2)

        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel1 = grayPhoto1.getPixel(x, y)
                val pixel2 = grayPhoto2.getPixel(x, y)

                val grayValue1 = (Color.red(pixel1) * photo1K).toInt().coerceIn(threshold + 1, 254)
                val grayValue2 = (Color.red(pixel2) * photo2K).toInt().coerceIn(1, threshold)

                val alpha = 255 - (grayValue1 - grayValue2)
                val gray = (255f * grayValue2 / alpha).toInt().coerceIn(0, 255)

                outputBitmap.setPixel(x, y, Color.argb(alpha, gray, gray, gray))
            }
        }
        return outputBitmap
    }

    // 将 Bitmap 转换为灰度图
    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

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

    // 调整 Bitmap 大小
    private fun scaleBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}