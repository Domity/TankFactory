package com.rbtsoft.tankfactory

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

object LsbTankEncoder {

    fun encode(surPic: Bitmap, insPic: Bitmap, info: String, compress: Int): Bitmap? {
        if (compress == 0 || compress >= 8) return null

        val lsbMask = intArrayOf(0x1, 0x3, 0x7, 0xF, 0x1F, 0x3F, 0x7F)
        val signature = "/By:f_Endman".toByteArray()

        //  为尊重原作者成果，并未对签名进行删除，这也意味着可以使用原作者的工具查看图片

        val insPicByteArray = bitmapToByteArray(insPic) ?: return null
        val insPicLength = insPicByteArray.size.toLong()

        val byteForLSB = (insPicLength * 8 / compress)
        val currentSurPicByte = (surPic.width * surPic.height * 3).toLong()
        val zoom = byteForLSB.toDouble() / currentSurPicByte.toDouble() * (if (compress >= 6) 1.05 else 1.01)
        val squareRootZoom = sqrt(zoom)
        val scaledWidth = (surPic.width * squareRootZoom).toInt()
        val scaledHeight = (surPic.height * squareRootZoom).toInt()

        val tankPic = Bitmap.createScaledBitmap(surPic, scaledWidth, scaledHeight, true)

        val header = mutableListOf<Byte>()
        header.addAll(insPicLength.toString().toByteArray().toList())
        header.add(0x01)

        header.addAll("hidden.png".toByteArray().toList())
        header.add(0x01)
        header.addAll("image/png".toByteArray().toList())
        header.add(0x00)

        val dataToHide = header.plus(insPicByteArray.toList()).toByteArray()

        val tankByteArray = IntArray(tankPic.width * tankPic.height * 3)
        var pixelIndex = 0
        for (y in 0 until tankPic.height) {
            for (x in 0 until tankPic.width) {
                val pixel = tankPic.getPixel(x, y)
                tankByteArray[pixelIndex++] = Color.red(pixel)
                tankByteArray[pixelIndex++] = Color.green(pixel)
                tankByteArray[pixelIndex++] = Color.blue(pixel)
            }
        }

        tankByteArray[0] = (tankByteArray[0] and 0xF8) or 0x00
        tankByteArray[1] = (tankByteArray[1] and 0xF8) or 0x03
        tankByteArray[2] = (tankByteArray[2] and 0xF8) or (compress and 0x7)

        var count = 0
        var snCount = 0
        var fifo = 0
        var fifoCount = 0

        for (i in 3 until tankByteArray.size) {
            if (fifoCount < compress) {
                val byteToHide = if (count < dataToHide.size) {
                    dataToHide[count++]
                } else {
                    signature[snCount++ % signature.size]
                }
                fifo = fifo or (byteToHide.toInt() shl (24 - fifoCount))
                fifoCount += 8
            }
            tankByteArray[i] = (tankByteArray[i] and lsbMask[compress - 1].inv()) or ((fifo shr (32 - compress)) and lsbMask[compress - 1])
            fifo = fifo shl compress
            fifoCount -= compress
        }

        val outputBitmap = Bitmap.createBitmap(tankPic.width, tankPic.height, Bitmap.Config.ARGB_8888)
        var outputIndex = 0
        for (y in 0 until tankPic.height) {
            for (x in 0 until tankPic.width) {
                val red = tankByteArray[outputIndex++]
                val green = tankByteArray[outputIndex++]
                val blue = tankByteArray[outputIndex++]
                outputBitmap.setPixel(x, y, Color.rgb(red, green, blue))
            }
        }

        tankPic.recycle()
        return outputBitmap
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray? {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}