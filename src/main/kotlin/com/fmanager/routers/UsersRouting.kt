package com.fmanager.routers

import com.fmanager.dao.implementation.DAOUsersImpl
import com.fmanager.dao.interfaces.DAOUsers
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
            val user = UserService(application::generateHash).getCurrentUser(login)
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

                if (UserService(application::generateHash).changeAccess(role, newRole, login)) {
                    call.respond(hashMapOf("success" to "Роль успешно измененна!"))
                    call.respond(HttpStatusCode.OK)
                }
                else {
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
