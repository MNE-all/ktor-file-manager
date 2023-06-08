package com.fmanager.utils

import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.text.toCharArray

object PasswordSecure {
    private const val ALGORITHM = "PBKDF2WithHmacSHA512"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val SECRET = "SomeRandomSecret"

    fun generateRandomSalt(): ByteArray {
        val random = SecureRandom.getInstance("SHA1PRNG")
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    fun generateHash(password: String, salt: ByteArray): ByteArray {
        val combinedSalt = "${salt.decodeToString()}$SECRET".toByteArray()

        val factory: SecretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM)
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), combinedSalt, ITERATIONS, KEY_LENGTH)
        val key: SecretKey = factory.generateSecret(spec)

        return key.encoded
    }
}