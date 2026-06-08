package com.rbtsoft.tankfactory.obfuscation

object ObfuscationCoder {
    fun generatePassword(): String = TankCipher.generateSecurePassword()
    fun encrypt(data: ByteArray, password: String): ByteArray = TankCipher.encryptData(data, password)
    fun decrypt(data: ByteArray, password: String): ByteArray? = TankCipher.decryptData(data, password)
}
