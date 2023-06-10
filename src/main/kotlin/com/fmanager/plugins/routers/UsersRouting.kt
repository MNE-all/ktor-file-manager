package com.fmanager.plugins.routers

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fmanager.plugins.schemas.ResponseUser
import com.fmanager.utils.DatabaseFactory
import com.fmanager.utils.JWTPrefs
import com.fmanager.utils.PasswordSecure
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Application.configureUserRouting() {
    routing {
        // Create user
        post("/users") {
            with(DatabaseFactory) {
                val user = call.receive<ResponseUser>()
                val login = this.UserService.create(user)
                call.respond(HttpStatusCode.Created, "The '$login' account is created")
            }
        }
        // Login
        post("/login") {
            val db = DatabaseFactory
            val users = db.UserService.readAll()
            val login = (call.request.queryParameters["login"] ?: throw IllegalArgumentException("Invalid login"))
            val password = (call.request.queryParameters["password"] ?: throw IllegalArgumentException("Invalid password"))


            users.forEach{ user ->
                val hash = PasswordSecure.generateHash(password, user.salt).decodeToString()
                val trueHase = user.password.decodeToString()

                if (user.login == login && trueHase == hash) {
                    // Generate JWT
                    with(JWTPrefs) {
                        val token = JWT.create()
                            .withAudience(jwtAudience)
                            .withIssuer(jwtDomain)
                            .withClaim("login", user.login)
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
        // Read user
        get("/users/{login}") {
            val db = DatabaseFactory
            with(db) {
                val login = (call.parameters["login"]  ?: throw IllegalArgumentException("Invalid login"))
                val user = this.UserService.read(login)
                if (user != null) {
                    call.respond(HttpStatusCode.OK, user)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        authenticate {
            // Update user
            put("/users") {
                with(DatabaseFactory) {
                    val principal = call.principal<JWTPrincipal>()
                    val login = principal!!.payload.getClaim("login").asString()
                    val user = call.receive<ResponseUser>()

                    if (login != "admin" || user.login == "admin"){
                        this.UserService.update(login, user)
                        call.respond(hashMapOf("success" to "Пользователь '${user.login}' успешно изменен!"))
                        call.respond(HttpStatusCode.OK)
                    }
                    else
                    {
                        call.respond(hashMapOf("error" to "Невозможно изменить логин пользователя!"))
                        call.respond(HttpStatusCode.BadRequest)
                    }

                }
            }


            // Update user role
            /*
            * 1. user
            * 2. writer
            * 3. admin
             */
            put("/users/access") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()

                if (role > 2) {
                    with(DatabaseFactory) {
                        val login = (call.request.queryParameters["login"] ?: throw IllegalArgumentException("Invalid login"))
                        val newRole: Int =
                            (call.request.queryParameters["role"]
                                ?: throw IllegalArgumentException("Invalid role")).toInt()

                        if (this.UserService.read(login)?.login != "admin" || newRole > 2) {
                            this.UserService.changeRole(login, newRole)
                            call.respond(hashMapOf("success" to "Роль успешно измененна!"))
                            call.respond(HttpStatusCode.OK)
                        }
                        else {
                            call.respond(hashMapOf("error" to "Невозможно понижение роли данного пользователя!"))
                            call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                }
            }

            // Delete user
            delete("/users") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()

                val login = if (call.request.queryParameters["login"] != null && role > 2) {
                    call.request.queryParameters["login"]
                }
                else {
                    principal.payload.getClaim("login").asString()
                }

                with(DatabaseFactory) {
                    if (login != "admin") {
                        val startAmount = this.UserService.readAll().count()
                        this.UserService.delete(login!!)
                        if (startAmount - 1 == this.UserService.readAll().count()) {
                            call.respond(hashMapOf("success" to "Пользователь '$login' успешно удалён!"))
                            call.respond(HttpStatusCode.OK)
                        }
                        else {
                            call.respond(hashMapOf("error" to "Данного пользователя не существует!"))
                            call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                    else {
                        call.respond(hashMapOf("error" to "Невозможно удаление данного пользователя!"))
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}
