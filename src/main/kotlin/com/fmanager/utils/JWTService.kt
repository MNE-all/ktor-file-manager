package com.fmanager.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JWTService {
    const val jwtAudience = "file-manager.com"
    const val jwtDomain = "http://0.0.0.0:8080/" // "http://file-manager.com"
    const val jwtRealm = "ktor file manager app"
    const val jwtSecret = "secret"

    fun generateToken(login: String, role: Int): String{
        with(JWTService) {
             return JWT.create()
                .withAudience(jwtAudience)
                .withIssuer(jwtDomain)
                .withClaim("login", login)
                .withClaim("role", role)
                .withExpiresAt(Date(System.currentTimeMillis() + 60000))
                .sign(Algorithm.HMAC256(jwtSecret))
        }
    }
}