package com.fmanager.plugins.routers

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fmanager.utils.DatabaseFactory
import com.fmanager.plugins.schemas.ExposedUser
import com.fmanager.utils.JWTPrefs
import com.fmanager.utils.PasswordSecure
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import java.util.*

fun Application.configureUserRouting() {
    routing {
        // Create user
        post("/users") {
            with(DatabaseFactory) {
                val user = call.receive<ExposedUser>()
                val id = this.UserService.create(user)
                call.respond(HttpStatusCode.Created, id)
            }
        }
        post("/login") {
            val db = DatabaseFactory
            val users = db.UserService.readAll()
            val login = (call.request.queryParameters["login"] ?: throw IllegalArgumentException("Invalid login"))
            val password = (call.request.queryParameters["password"] ?: throw IllegalArgumentException("Invalid password"))


            users.forEach{ user ->
                val hash = PasswordSecure.generateHash(password, user.salt).decodeToString()
                var trueHase = user.password.decodeToString()

                if (user.login == login && trueHase == hash) {
                    with(JWTPrefs) {
                        val token = JWT.create()
                            .withAudience(jwtAudience)
                            .withIssuer(jwtDomain)
                            .withClaim("username", user.login)
                            .withClaim("role", user.role)
                            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
                            .sign(Algorithm.HMAC256(jwtSecret))
                        call.respond(hashMapOf("token" to token))
                    }
                    return@post
                }
            }
            call.respond(hashMapOf("error" to "Ошибка при авторизации!"))
        }

        get("/users") {
            with(DatabaseFactory) {
                call.respond(HttpStatusCode.OK, this.UserService.readAll())
            }
        }
        // Read user
        get("/users/{id}") {
            val db = DatabaseFactory
            with(db) {
                val id = (call.parameters["id"]  ?: throw IllegalArgumentException("Invalid ID"))
                val user = this.UserService.read(UUID.fromString(id))
                if (user != null) {
                    call.respond(HttpStatusCode.OK, user)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        // Update user
        put("/users/{id}") {
            with(DatabaseFactory) {
                val id = (call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID"))
                val user = call.receive<ExposedUser>()
                this.UserService.update(UUID.fromString(id), user)
                call.respond(HttpStatusCode.OK)
            }
        }
        // Delete user
        delete("/users/{id}") {
            with(DatabaseFactory) {
                val id = (call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID"))
                this.UserService.delete(UUID.fromString(id))
                call.respond(HttpStatusCode.OK)
            }
        }

        // Delete all users (for debugging)
        delete("/users") {
            with(DatabaseFactory) {
                for (user in this.UserService.readAll()) {
                    this.UserService.delete(UUID.fromString(user.uuid))
                }
                call.respond(HttpStatusCode.OK)
            }
        }


    }
}
