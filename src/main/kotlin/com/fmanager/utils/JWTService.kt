package com.fmanager.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import java.util.*
fun Application.generateToken(login: String, role: Int): String {
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val secret = environment.config.property("jwt.secret").getString()
    return JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("login", login)
        .withClaim("role", role)
        .withExpiresAt(Date(System.currentTimeMillis() + 60000))
        .sign(Algorithm.HMAC256(secret))
}

