package com.fmanager.routers

import com.fmanager.dao.implementation.DAOUsersImpl
import com.fmanager.dao.interfaces.DAOUsers
import com.fmanager.plugins.schemas.ResponseUser
import com.fmanager.utils.generateHash
import com.fmanager.utils.generateToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking


fun Application.configureUserRouting() {
    routing {
        val userService: DAOUsers = DAOUsersImpl().apply {
            runBlocking {
                if(allUsers().isEmpty()) {
                    init("admin", "admin", "root", application::generateHash)
                }
            }
        }
        get("/users") {
            call.respond(userService.allUsers())
        }

        // Create user
        post("/users") {
            val user = call.receive<ResponseUser>()
            try {
                call.respond(HttpStatusCode.Created,
                    "The '${userService.addNewUser(user.name, user.login, user.password, application::generateHash)!!.login}' account is created")
            }
            catch (e: Exception){
                call.respond(HttpStatusCode.BadRequest, "login is not unique")
            }
        }
        // Login
        post("/login") {
            val users = userService.allUsers()
            val login = (call.request.queryParameters["login"] ?: throw IllegalArgumentException("Invalid login"))
            val password = (call.request.queryParameters["password"] ?: throw IllegalArgumentException("Invalid password"))

            users.forEach{ user ->
                if (user.login == login) {
                    val hash = application.generateHash(password, user.salt).decodeToString()
                    val trueHase = user.password.decodeToString()

                    if (trueHase == hash) {
                        // Generate JWT
                        call.respond(hashMapOf("token" to application.generateToken(user.login, user.role)))
                        return@post
                    }
                }
            }
            call.respond(hashMapOf("error" to "Ошибка при авторизации!"))
        }
        // Read user
        get("/users/{login}") {
            val login = (call.parameters["login"] ?: throw IllegalArgumentException("Invalid login"))
            val user = userService.user(login)
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

                if (login != "admin" || user.login == "admin") {
                    userService.editUser(login, user.login, user.name, user.password, application::generateHash)
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

                if (role > 2) {
                    val login =
                        (call.request.queryParameters["login"] ?: throw IllegalArgumentException("Invalid login"))
                    val newRole: Int =
                        (call.request.queryParameters["role"]
                            ?: throw IllegalArgumentException("Invalid role")).toInt()

                    if (userService.user(login)?.login != "admin" || newRole > 2) {
                        userService.changeRole(login, newRole)
                        call.respond(hashMapOf("success" to "Роль успешно измененна!"))
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(hashMapOf("error" to "Невозможно понижение роли данного пользователя!"))
                        call.respond(HttpStatusCode.BadRequest)
                    }

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

                if (login != "admin") {
                    val startAmount = userService.allUsers().count()
                    userService.deleteUser(login!!)
                    if (startAmount - 1 == userService.allUsers().count()) {
                        call.respond(hashMapOf("success" to "Пользователь '$login' успешно удалён!"))
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(hashMapOf("error" to "Данного пользователя не существует!"))
                        call.respond(HttpStatusCode.BadRequest)
                    }
                } else {
                    call.respond(hashMapOf("error" to "Невозможно удаление данного пользователя!"))
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}
