package com.fmanager

import com.fmanager.plugins.configureSecurity
import com.fmanager.plugins.configureSerialization
import com.fmanager.plugins.routers.configureUserRouting
import com.fmanager.plugins.schemas.ResponseUser
import com.fmanager.utils.DatabaseFactory
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplicationTestUsersRouting {

    @Serializable
    data class AuthToken(val token: String)

    // Создание профиля
    @Test
    fun testRootPostUser() = testApplication {
        application {
            configureSerialization()
            configureSecurity()
            configureUserRouting()
        }

        val response = client.post("/users") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(ResponseUser("Test", "K1G9APpTuEHpFx3dTDT8", "Brains")))
        }
        assertEquals("The 'K1G9APpTuEHpFx3dTDT8' account is created", response.bodyAsText())
        assertEquals(HttpStatusCode.Created, response.status)
    }

    // Авторизация пользователя
    @Test
    fun testRootLoginUser() = testApplication {
        application {
            configureSerialization()
            configureSecurity()
            configureUserRouting()
        }

        val response = client.post("/login") {
            parameter("login", "admin")
            parameter("password", "root")
        }

        assertEquals(true, response.bodyAsText().contains("token"))
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // Просмотр информации о конкретном пользователе (в данном случае - об администаторе)
    @Test
    fun testRootReadUser()= testApplication {
        application {
            configureSerialization()
            configureSecurity()
            configureUserRouting()
        }

        client.get("/users/admin").apply {
            with(DatabaseFactory){
                val user = this.UserService.read("admin")!!
                val admin = Json.decodeFromString<ResponseUser>(bodyAsText())

                assertEquals(user.name, admin.name)
                assertEquals(user.login, admin.login)
                assertEquals(user.password, admin.password)
                assertEquals(HttpStatusCode.OK, status)
            }
        }
    }

    // Удаление пользователя по токену
    @Test
    fun testRootDeleteUserByToken() = testApplication {
        application {
            configureSerialization()
            configureSecurity()
            configureUserRouting()
        }

        val loginResponse = client.post("/login") {
            parameter("login", "K1G9APpTuEHpFx3dTDT8")
            parameter("password", "Brains")
        }

        val response = client.delete("/users") {
            header(HttpHeaders.Authorization, "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}")
        }

        assertEquals(true, response.bodyAsText().contains("success"))
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // Удаление пользователя по логину
    @Test
    fun testRootDeleteUserByLogin() = testApplication {
        application {
            configureSerialization()
            configureSecurity()
            configureUserRouting()
        }

        val loginResponse = client.post("/login") {
            parameter("login", "admin")
            parameter("password", "root")
        }

        with(DatabaseFactory) {
            this.UserService.create(ResponseUser("Test", "V7HH6KRny3pg6WBbWqnu", "Brains"))
        }

        val response = client.delete("/users") {
            header(HttpHeaders.Authorization, "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}")
            parameter("login", "V7HH6KRny3pg6WBbWqnu")
        }


        assertEquals(true, response.bodyAsText().contains("success"))
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // Изменение профиля
    @Test
    fun testRootUserEdit() = testApplication {
        application {
            configureSerialization()
            configureSecurity()
            configureUserRouting()
        }
        suspend fun auth(password: String): HttpResponse =
            client.post("/login") {
                parameter("login", "admin")
                parameter("password", password)
            }

        var loginResponse = auth("root")

        val response = client.put("/users") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}")
            setBody(Json.encodeToString(ResponseUser("admin", "admin", "newPassword")))
        }

        loginResponse = auth("newPassword")

        client.put("/users") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}")
            setBody(Json.encodeToString(ResponseUser("admin", "admin", "root")))
        }
        assertEquals(true, response.bodyAsText().contains("success"))
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // Изменения роли пользователя
    @Test
    fun testRootUserAccess() = testApplication {
        application {
            configureSerialization()
            configureSecurity()
            configureUserRouting()
        }

        val loginResponse = client.post("/login") {
            parameter("login", "admin")
            parameter("password", "root")
        }

        with(DatabaseFactory) {
            this.UserService.create(ResponseUser("test", "AEvQpKyX2g7skEYxBHoC", "root"))

            val response = client.put("/users/access") {
                contentType(ContentType.Application.Json)
                header(
                    HttpHeaders.Authorization,
                    "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}"
                )
                parameter("login", "AEvQpKyX2g7skEYxBHoC")
                parameter("role", 2)
            }

            assertEquals(true, response.bodyAsText().contains("success"))
            assertEquals(HttpStatusCode.OK, response.status)

            this.UserService.delete("AEvQpKyX2g7skEYxBHoC")
        }
    }
}
