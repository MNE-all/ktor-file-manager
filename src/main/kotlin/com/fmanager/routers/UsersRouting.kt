package com.fmanager.routers

import com.fmanager.plugins.schemas.ResponseUser
import com.fmanager.plugins.services.UserService
import com.fmanager.utils.generateHash
import com.fmanager.utils.generateToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureUserRouting() {
    routing {
        get("/users") {
            call.respond(UserService(null).allUsers())
        }

        // Create user
        post("/users") {
            val userLogin = UserService(application::generateHash).addUser(call.receive<ResponseUser>())
            if (userLogin != null) {
                call.respond(HttpStatusCode.Created, "The '$userLogin' account is created")
            } else {
                call.respond(HttpStatusCode.BadRequest, "login is not unique")
            }
        }
        // Login
        post("/login") {
            val login = (call.request.queryParameters["login"] ?: throw IllegalArgumentException("Invalid login"))
            val password =
                (call.request.queryParameters["password"] ?: throw IllegalArgumentException("Invalid password"))

            val role = UserService(application::generateHash).login(login, password)
            if (role != null) {
                // Generate JWT
                call.respond(hashMapOf("token" to application.generateToken(login, role)))
                return@post
            }
            call.respond(hashMapOf("error" to "Ошибка при авторизации!"))
        }
        // Read user
        get("/users/{login}") {
            val login = (call.parameters["login"] ?: throw IllegalArgumentException("Invalid login"))
            val user = UserService(null).getCurrentUser(login)
            if (user != null) {
                call.respond(HttpStatusCode.OK, ResponseUser(user.name, user.login, user.password.decodeToString()))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        authenticate {
            // Update user
            put("/users") {
                val principal = call.principal<JWTPrincipal>()
                val login = principal!!.payload.getClaim("login").asString()
                val user = call.receive<ResponseUser>()

                if (UserService(application::generateHash).editUser(login, user)) {
                    call.respond(hashMapOf("success" to "Пользователь '${user.login}' успешно изменен!"))
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(hashMapOf("error" to "Невозможно изменить логин пользователя!"))
                    call.respond(HttpStatusCode.BadRequest)
                }
            }


            // Update user role
            /*
            * 1. user
            * 2. writer
            * 3. admin
            *
            * Более актуальную информацию можно получть, отправив get запрос по /access
             */
            put("/users/access") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()
                val newRole: Int =
                    (call.request.queryParameters["role"] ?: throw IllegalArgumentException("Invalid role")).toInt()
                val login = call.request.queryParameters["login"] ?: throw IllegalArgumentException("Invalid login")

                if (UserService(null).changeAccess(role, newRole, login)) {
                    call.respond(hashMapOf("success" to "Роль успешно измененна!"))
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(hashMapOf("error" to "Невозможно понижение роли данного пользователя!"))
                    call.respond(HttpStatusCode.BadRequest)
                }
            }


            // Delete user
            delete("/users") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()
                val login = if (call.request.queryParameters["login"] != null && role > 2) {
                    call.request.queryParameters["login"]
                } else {
                    principal.payload.getClaim("login").asString()
                }

                val message = UserService(null).deleteUser(login!!)
                if (message.contains("success")) {
                    call.respond(message)
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(message)
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}
