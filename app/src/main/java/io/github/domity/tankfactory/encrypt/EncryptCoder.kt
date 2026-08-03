package io.github.domity.tankfactory.encrypt

object EncryptCoder {
    fun generatePassword(): CharArray = TankCipher.generateSecurePassword()
    fun encrypt(data: ByteArray, password: CharArray): ByteArray = TankCipher.encryptData(data, password)
    fun decrypt(data: ByteArray, password: CharArray): ByteArray? = TankCipher.decryptData(data, password)
}