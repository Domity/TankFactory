package io.github.domity.tankfactory.encrypt

import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object TankCipher {

    init { System.loadLibrary("tankfactory") }

    private external fun nativeChaCha20Poly1305Process(
        data: ByteArray,
        key32: ByteArray,
        nonce: ByteArray,
        isEncrypt: Boolean,
        tag: ByteArray
    ): Int

    private val secureRandom = SecureRandom()

    fun generateSecurePassword(): CharArray {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#%^&*()-_=+[]{}|;:,.<>?/~"
        return CharArray(20) { chars[secureRandom.nextInt(chars.length)] }
    }

    private fun deriveKey32(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, 600000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derived = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return derived
    }

    fun encryptData(rawData: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(16).apply(secureRandom::nextBytes)
        val nonce = ByteArray(12).apply(secureRandom::nextBytes)
        val key32 = deriveKey32(password, salt)
        val ciphertext = rawData.clone()
        val tag = ByteArray(16)

        try {
            val res = nativeChaCha20Poly1305Process(ciphertext, key32, nonce, true, tag)
            if (res != 0) throw IllegalStateException("Native encryption failed code=$res")
            return salt + nonce + tag + ciphertext
        } finally {
            Arrays.fill(key32, 0.toByte())
        }
    }

    fun decryptData(encryptedPayload: ByteArray, password: CharArray): ByteArray? {
        if (encryptedPayload.size <= 44) return null
        val salt = encryptedPayload.copyOfRange(0, 16)
        val nonce = encryptedPayload.copyOfRange(16, 28)
        val tag = encryptedPayload.copyOfRange(28, 44)
        val ciphertext = encryptedPayload.copyOfRange(44, encryptedPayload.size)
        val key32 = deriveKey32(password, salt)

        try {
            val res = nativeChaCha20Poly1305Process(ciphertext, key32, nonce, false, tag)
            if (res != 0) return null
            return ciphertext
        } finally {
            Arrays.fill(key32, 0.toByte())
        }
    }
}