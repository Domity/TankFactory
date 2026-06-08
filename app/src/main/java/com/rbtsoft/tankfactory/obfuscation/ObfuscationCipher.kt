package com.rbtsoft.tankfactory.obfuscation

import java.security.MessageDigest
import java.security.SecureRandom

object TankCipher {

    init {
        System.loadLibrary("tankfactory")
    }

    private external fun nativeChaCha20Process(data: ByteArray, key32: ByteArray)
    private val secureRandom = SecureRandom()

    fun generateSecurePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..20).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
    }

    private fun deriveKey32(password: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray(Charsets.UTF_8))
    }

    fun encryptData(rawData: ByteArray, password: String): ByteArray {
        val key32 = deriveKey32(password)
        val nonce = ByteArray(12)
        secureRandom.nextBytes(nonce)
        val payload = ByteArray(12 + rawData.size)
        System.arraycopy(nonce, 0, payload, 0, 12)
        System.arraycopy(rawData, 0, payload, 12, rawData.size)
        nativeChaCha20Process(payload, key32)
        return payload
    }

    fun decryptData(encryptedPayload: ByteArray, password: String): ByteArray? {
        if (encryptedPayload.size <= 12) return null
        val key32 = deriveKey32(password)
        val workingCopy = encryptedPayload.clone()
        nativeChaCha20Process(workingCopy, key32)
        val rawDataSize = workingCopy.size - 12
        val rawData = ByteArray(rawDataSize)
        System.arraycopy(workingCopy, 12, rawData, 0, rawDataSize)
        return rawData
    }
}