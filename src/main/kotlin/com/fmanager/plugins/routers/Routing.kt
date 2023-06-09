package com.fmanager.plugins.routers

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Welcome to file manager API!")
        }

        authenticate{
            get("/hello") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("login").asString()
                val role = principal.payload.getClaim("role").asInt()
                val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
                call.respondText("Hello, $username! Role $role. Token is expired at $expiresAt ms.")
            }
        }
    }
}
