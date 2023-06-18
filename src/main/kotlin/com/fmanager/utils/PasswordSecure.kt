package com.fmanager.utils

import io.ktor.server.application.*
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordSecure {
    fun generateRandomSalt(): ByteArray {
        val random = SecureRandom.getInstance("SHA1PRNG")
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }
}
fun Application.generateHash(password: String, salt: ByteArray): ByteArray {
    val ALGORITHM = environment.config.property("secure.ALGORITHM").getString()
    val ITERATIONS = environment.config.property("secure.ITERATIONS").getString().toInt()
    val KEY_LENGTH = environment.config.property("secure.KEY_LENGTH").getString().toInt()
    val SECRET = environment.config.property("secure.SECRET").getString()

    val combinedSalt = "${salt.decodeToString()}$SECRET".toByteArray()

    val factory: SecretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM)
    val spec: KeySpec = PBEKeySpec(password.toCharArray(), combinedSalt, ITERATIONS, KEY_LENGTH)
    val key: SecretKey = factory.generateSecret(spec)

    return key.encoded
}