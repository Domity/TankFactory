package com.rbtsoft.tankfactory.lsbtank

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

object LsbTankEncoder {

    @SuppressLint("UseKtx")
    fun encode(surPic: Bitmap, insPic: Bitmap, compress: Int): Bitmap? {
        if (compress == 0 || compress >= 8) return null
        val lsbMask = intArrayOf(0x1, 0x3, 0x7, 0xF, 0x1F, 0x3F, 0x7F)
        val signature = "/By:f_Endman".toByteArray()

        //  为尊重原作者成果，并未对签名进行删除，这也意味着可以使用原作者的工具查看图片
        //  不至于出现"不兼容"的情况
        //  压缩度大于4的除外

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
        val pixels = IntArray(tankPic.width * tankPic.height)
        tankPic.getPixels(pixels, 0, tankPic.width, 0, 0, tankPic.width, tankPic.height)
        val tankByteArray = IntArray(tankPic.width * tankPic.height * 3)
        var pixelIndex = 0
        for (pixel in pixels) {
            tankByteArray[pixelIndex++] = Color.red(pixel)
            tankByteArray[pixelIndex++] = Color.green(pixel)
            tankByteArray[pixelIndex++] = Color.blue(pixel)
        }
        tankByteArray[0] = (tankByteArray[0] and 0xF8) or 0x00
        tankByteArray[1] = (tankByteArray[1] and 0xF8) or 0x03
        tankByteArray[2] = (tankByteArray[2] and 0xF8) or (compress and 0x7)
        var count = 0
        var snCount = 0
        var fifo=0L
        var fifoCount = 0
        for (i in 3 until tankByteArray.size) {
            if (fifoCount < compress) {
                val byteToHide = if (count < dataToHide.size) {
                    dataToHide[count++]
                } else {
                    signature[snCount++ % signature.size]
                }
                val unsignedByteLong = (byteToHide.toInt() and 0xFF).toLong()
                fifo = fifo or (unsignedByteLong shl (24 - fifoCount))

                fifoCount += 8
            }
            val lsbBits = ((fifo ushr (32 - compress)) and lsbMask[compress - 1].toLong()).toInt()
            tankByteArray[i] = (tankByteArray[i] and lsbMask[compress - 1].inv()) or lsbBits
            fifo = fifo shl compress
            fifoCount -= compress
        }
        val finalPixels = IntArray(tankPic.width * tankPic.height)
        var outputIndex = 0
        for (i in finalPixels.indices) {
            val red = tankByteArray[outputIndex++]
            val green = tankByteArray[outputIndex++]
            val blue = tankByteArray[outputIndex++]
            finalPixels[i] = Color.rgb(red, green, blue)
        }
        val outputBitmap = Bitmap.createBitmap(tankPic.width, tankPic.height, Bitmap.Config.ARGB_8888)
        outputBitmap.setPixels(finalPixels, 0, tankPic.width, 0, 0, tankPic.width, tankPic.height)
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

    fun decode(tankPic: Bitmap): Bitmap? {
            val lsbMask = intArrayOf(0x1, 0x3, 0x7, 0xF, 0x1F, 0x3F, 0x7F)
            val width = tankPic.width
            val height = tankPic.height
            val pixels = IntArray(width * height)
            tankPic.getPixels(pixels, 0, width, 0, 0, width, height)
            val tankByteArray = ByteArray(width * height * 3)
            for (i in pixels.indices) {
                tankByteArray[i * 3 + 0] = Color.red(pixels[i]).toByte()
                tankByteArray[i * 3 + 1] = Color.green(pixels[i]).toByte()
                tankByteArray[i * 3 + 2] = Color.blue(pixels[i]).toByte()
            }
            val byte0 = tankByteArray[0].toInt() and 0xFF
            val byte1 = tankByteArray[1].toInt() and 0xFF
            val byte2 = tankByteArray[2].toInt() and 0xFF

            if ((byte0 and 0x7) != 0x0 ||
                (byte1 and 0x7) != 0x3 ||
                (byte2 and 0x7) == 0// ||
               // (byte2 and 0x7) > 7
            ) {
                return null
            }
            val lsbCompress = byte2 and 0x7
            var fifo =0L
            var fifoCount = 0
            val lsbByteList = ByteArrayOutputStream()
            val currentLsbMask = lsbMask[lsbCompress - 1]
            //val lsbCompressLong = lsbCompress.toLong()
            for (i in 2 until tankByteArray.size) {
                val currentByte = tankByteArray[i].toInt() and 0xFF
                val newLsb = currentByte and currentLsbMask
                fifo = fifo or (newLsb.toLong())
                if (fifoCount >= 8) {
                    val shiftAmount = fifoCount - 8
                    val decodedByteInt = ((fifo ushr shiftAmount) and 0xFF).toInt()
                    lsbByteList.write(decodedByteInt)
                    fifoCount -= 8
                }
                fifo = (fifo.toInt().toLong()) shl lsbCompress
                fifoCount += lsbCompress
            }
            val lsbByteArrayFull = lsbByteList.toByteArray()
            if (lsbByteArrayFull.size < 256) {
                return null
            }
            var offset = 0
            val sLsbCountBuilder = StringBuilder()
            val lsbFileNameList = ByteArrayOutputStream()
            val lsbFileMimeBuilder = StringBuilder()
            while (offset < lsbByteArrayFull.size && offset < 0xFF && lsbByteArrayFull[offset].toInt() != 0x01) {
                val currentByte = lsbByteArrayFull[offset].toInt() and 0xFF
                if (currentByte in 48..57) {
                    sLsbCountBuilder.append(currentByte.toChar())
                } else {
                    return null
                }
                offset++
            }
            if (offset == lsbByteArrayFull.size || offset == 0xFF) return null
            offset++ // 跳过 0x01
            while (offset < lsbByteArrayFull.size && offset < 0xFF && lsbByteArrayFull[offset].toInt() != 0x01) {
                lsbFileNameList.write(lsbByteArrayFull[offset].toInt())
                offset++
            }
            if (offset == lsbByteArrayFull.size || offset == 0xFF) return null
            offset++
            while (offset < lsbByteArrayFull.size && offset < 0xFF && lsbByteArrayFull[offset].toInt() != 0x00) {
                lsbFileMimeBuilder.append(lsbByteArrayFull[offset].toInt().toChar())
                offset++
            }
            if (offset == lsbByteArrayFull.size || offset == 0xFF) return null
            offset++
            val sLsbCount = sLsbCountBuilder.toString()
            val LsbCount = sLsbCount.toIntOrNull() ?: run {
                return null
            }
            if (lsbByteArrayFull.size < offset + LsbCount) {
                return null
            }
            val lsbData = lsbByteArrayFull.sliceArray(offset until offset + LsbCount)
            return BitmapFactory.decodeByteArray(lsbData, 0, lsbData.size)

    }
}
