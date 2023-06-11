package com.fmanager.green

import com.fmanager.dao.users.DAOUsers
import com.fmanager.dao.users.DAOUsersImpl
import com.fmanager.module
import com.fmanager.plugins.configureSecurity
import com.fmanager.plugins.configureSerialization
import com.fmanager.plugins.schemas.ResponseUser
import com.fmanager.routers.configureUserRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplicationTestUsersRouting {
    private val dao: DAOUsers = DAOUsersImpl().apply {
        runBlocking {
            if(allUsers().isEmpty()) {
                addNewUser("admin", "admin", "root")
            }
        }
    }

    @Serializable
    data class AuthToken(val token: String)


    // Создание профиля
    @Test
    fun testRootPostUser() = testApplication {
        // Setup
        application {
            configureSerialization()
            configureSecurity()
            configureUserRouting()
        }

        // Test
        val response = client.post("/users") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(ResponseUser("Test", "K1G9APpTuEHpFx3dTDT8", "Brains")))
        }
        assertEquals("The 'K1G9APpTuEHpFx3dTDT8' account is created", response.bodyAsText())
        assertEquals(HttpStatusCode.Created, response.status)

        // Tears down
        dao.deleteUser("K1G9APpTuEHpFx3dTDT8")

    }

    // Авторизация пользователя
    @Test
    fun testRootLoginUser() = testApplication {
        // Setup
        application {
            module()
        }

        // Test
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
        // Setup
        application {
            configureSerialization()
            configureSecurity()
            configureUserRouting()
        }

        // Test
        client.get("/users/admin").apply {
            val user = dao.user("admin")!!
            val admin = Json.decodeFromString<ResponseUser>(bodyAsText())

            assertEquals(user.name, admin.name)
            assertEquals(user.login, admin.login)
            assertEquals(user.password.decodeToString(), admin.password)
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    // Удаление пользователя по токену
    @Test
    fun testRootDeleteUserByToken() = testApplication {
        // Setup
        application {
            configureSerialization()
            configureSecurity()
            configureUserRouting()
        }

        dao.addNewUser("name", "K1G9APpTuEHpFx3dTDT8", "Brains")


        val loginResponse = client.post("/login") {
            parameter("login", "K1G9APpTuEHpFx3dTDT8")
            parameter("password", "Brains")
        }

        // Test
        val response = client.delete("/users") {
            header(HttpHeaders.Authorization, "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}")
        }

        assertEquals(true, response.bodyAsText().contains("success"))
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // Удаление пользователя по логину
    @Test
    fun testRootDeleteUserByLogin() = testApplication {
        // Setup
        application {
            configureSerialization()
            configureSecurity()
            configureUserRouting()
        }

        val loginResponse = client.post("/login") {
            parameter("login", "admin")
            parameter("password", "root")
        }

        dao.addNewUser("Test", "V7HH6KRny3pg6WBbWqnu", "Brains")


        // Test
        val response = client.delete("/users") {
            header(HttpHeaders.Authorization, "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}")
            parameter("login", "V7HH6KRny3pg6WBbWqnu")
        }


        assertEquals(true, response.bodyAsText().contains("success"))
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // Изменение профиля
    @Test
    fun testRootUserUpdate() = testApplication {
        // Setup
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

        // Test
        val response = client.put("/users") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}")
            setBody(Json.encodeToString(ResponseUser("admin", "admin", "newPassword")))
        }
        assertEquals(true, response.bodyAsText().contains("success"))
        assertEquals(HttpStatusCode.OK, response.status)

        // Tears down
        loginResponse = auth("newPassword")

        client.put("/users") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}")
            setBody(Json.encodeToString(ResponseUser("admin", "admin", "root")))
        }
    }

    // Изменения роли пользователя
    @Test
    fun testRootUserAccess() = testApplication {
        // Setup
        application {
            configureSerialization()
            configureSecurity()
            configureUserRouting()
        }

        val loginResponse = client.post("/login") {
            parameter("login", "admin")
            parameter("password", "root")
        }

        dao.addNewUser("test", "AEvQpKyX2g7skEYxBHoC", "root")


        // Test
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

        // Tears down
        dao.deleteUser("AEvQpKyX2g7skEYxBHoC")

    }
}
