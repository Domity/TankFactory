package io.github.domity.tankfactory.encrypt

import java.security.MessageDigest
import java.security.SecureRandom

object TankCipher {

    init { System.loadLibrary("tankfactory") }

    private external fun nativeChaCha20Process(data: ByteArray, key32: ByteArray)
    private val secureRandom = SecureRandom()

    fun generateSecurePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return buildString(20) {repeat(20){append(chars[secureRandom.nextInt(chars.length)])} }
    }

    private fun deriveKey32(password: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(password.toByteArray())

    fun encryptData(rawData: ByteArray, password: String): ByteArray {
        val key32 = deriveKey32(password)
        val nonce = ByteArray(12).apply(secureRandom::nextBytes)
        return (nonce + rawData).apply{nativeChaCha20Process(this,key32)}
    }

    fun decryptData(encryptedPayload: ByteArray, password: String): ByteArray? {
        if (encryptedPayload.size <= 12) return null
        val key32 = deriveKey32(password)
        return encryptedPayload.clone().apply {
            nativeChaCha20Process(this,key32)
        }.let{ workingCopy -> workingCopy.copyOfRange(12, workingCopy.size)}
    }
}