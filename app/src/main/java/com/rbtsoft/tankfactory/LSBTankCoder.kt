package com.rbtsoft.tankfactory

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

object LsbTankEncoder {

    @SuppressLint("UseKtx")
    fun encode(surPic: Bitmap, insPic: Bitmap, info: String, compress: Int): Bitmap? {
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
        var fifo: Long=0L  //  *关键
        var fifoCount = 0

        for (i in 3 until tankByteArray.size) {
            if (fifoCount < compress) {
                val byteToHide = if (count < dataToHide.size) {
                    dataToHide[count++]
                } else {
                    signature[snCount++ % signature.size]
                }
                val unsignedByteLong = (byteToHide.toInt() and 0xFF).toLong()

                // 使用这个无符号 Long 进行左移
                fifo = fifo or (unsignedByteLong shl (24 - fifoCount))

                fifoCount += 8
            }
            val lsbBits = ((fifo ushr (32 - compress)) and lsbMask[compress - 1].toLong()).toInt()

            tankByteArray[i] = (tankByteArray[i] and lsbMask[compress - 1].inv()) or lsbBits
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


    fun decode(tankPic: Bitmap): Bitmap? {
        try {
            //  验证签名
            val lsbMask = intArrayOf(0x1, 0x3, 0x7, 0xF, 0x1F, 0x3F, 0x7F)

            val width = tankPic.width
            val height = tankPic.height
            val pixels = IntArray(width * height)
            tankPic.getPixels(pixels, 0, width, 0, 0, width, height)
            // 读取所有像素的RGB字节
            val tankByteArray = ByteArray(width * height * 3)
            for (i in pixels.indices) {
                tankByteArray[i * 3 + 0] = Color.red(pixels[i]).toByte()
                tankByteArray[i * 3 + 1] = Color.green(pixels[i]).toByte()
                tankByteArray[i * 3 + 2] = Color.blue(pixels[i]).toByte()
            }
            //  检查文件签名和压缩度
            val byte0 = tankByteArray[0].toInt() and 0xFF
            val byte1 = tankByteArray[1].toInt() and 0xFF
            val byte2 = tankByteArray[2].toInt() and 0xFF

            if ((byte0 and 0x7) != 0x0 ||
                (byte1 and 0x7) != 0x3 ||
                (byte2 and 0x7) == 0 ||
                (byte2 and 0x7) > 7
            ) {
                return null
            }
            val lsbCompress = byte2 and 0x7

            //  抽取 LSB 数据
            var fifo:Long=0L  //  *关键,保证高压缩度图片能正确解析
            var fifoCount = 0
            val lsbByteList = ByteArrayOutputStream()
            val currentLsbMask = lsbMask[lsbCompress - 1]
            val lsbCompressLong = lsbCompress.toLong()

            for (i in 2 until tankByteArray.size) {
                val currentByte = tankByteArray[i].toInt() and 0xFF
                val newLsb = currentByte and currentLsbMask

                // 将新的 LSB 位存入 FIFO 的低位
                // 确保 newLsb 的值在 OR 之前是干净的 Long
                fifo = fifo or (newLsb.toLong())

                // 检查是否积累了至少一个字节 (8 位)
                if (fifoCount >= 8) {
                    // 取出高位字节
                    val shiftAmount = fifoCount - 8

                    // 使用 Long 的无符号右移  *关键
                    val decodedByteInt = ((fifo ushr shiftAmount) and 0xFF).toInt()
                    lsbByteList.write(decodedByteInt)

                    fifoCount -= 8
                }

                // 将 FIFO 左移 LsbCompress 位
                fifo = (fifo.toInt().toLong()) shl lsbCompress.toInt()

                // 增加计数器
                fifoCount += lsbCompress

            }
            val lsbByteArrayFull = lsbByteList.toByteArray()
            if (lsbByteArrayFull.size < 256) {
                return null
            }

            //  解析文件头信息
            var offset = 0
            val sLsbCountBuilder = StringBuilder()
            val lsbFileNameList = ByteArrayOutputStream()
            val lsbFileMimeBuilder = StringBuilder()

            //  提取文件大小（字符串）
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

            //  提取文件名
            while (offset < lsbByteArrayFull.size && offset < 0xFF && lsbByteArrayFull[offset].toInt() != 0x01) {
                lsbFileNameList.write(lsbByteArrayFull[offset].toInt())
                offset++
            }
            if (offset == lsbByteArrayFull.size || offset == 0xFF) return null
            offset++
            //  提取 MIME 类型
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
            // 检查解码出的数据是否足够长
            if (lsbByteArrayFull.size < offset + LsbCount) {
                return null
            }
            //  提取隐藏的图片数据
            val lsbData = lsbByteArrayFull.sliceArray(offset until offset + LsbCount)
            return BitmapFactory.decodeByteArray(lsbData, 0, lsbData.size)

        } catch (e: Exception) {
            return null
        }
    }
}
