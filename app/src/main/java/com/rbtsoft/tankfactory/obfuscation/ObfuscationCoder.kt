package com.rbtsoft.tankfactory.obfuscation

import java.security.SecureRandom
import java.util.Arrays

object obfuscator {

    init {
        System.loadLibrary("tankfactory")
    }

    private external fun nativeProcess(data: ByteArray, password: String, rounds: Int)

    fun encrypt(originalData: ByteArray, password: String, rounds: Int = 12): ByteArray {
        val resultData = ByteArray(originalData.size + 12)
        val nonce = ByteArray(12)
        SecureRandom().nextBytes(nonce)
        System.arraycopy(nonce, 0, resultData, 0, 12)
        System.arraycopy(originalData, 0, resultData, 12, originalData.size)
        nativeProcess(resultData, password, rounds)

        return resultData
    }
    fun decrypt(encryptedData: ByteArray, password: String, rounds: Int = 12): ByteArray {
        if (encryptedData.size <= 12) {
            return ByteArray(0)
        }
        val workingData = encryptedData.copyOf()
        nativeProcess(workingData, password, rounds)
        return Arrays.copyOfRange(workingData, 12, workingData.size)
    }
}