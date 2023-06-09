package com.fmanager.green

import com.fmanager.dao.implementation.DAOFilesImpl
import com.fmanager.dao.implementation.DAOUsersImpl
import com.fmanager.dao.interfaces.DAOFiles
import com.fmanager.dao.interfaces.DAOUsers
import com.fmanager.plugins.DatabaseFactory
import com.fmanager.plugins.schemas.ResponseFile
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ApplicationTestFilesRouting {
    private val userService: DAOUsers = DAOUsersImpl()
    private val fileService: DAOFiles = DAOFilesImpl()
    init {
        DatabaseFactory
        userService
    }
    private suspend fun auth(password: String, app: ApplicationTestBuilder): HttpResponse =
        with(app) {
            client.post("/login") {
                parameter("login", "admin")
                parameter("password", password)
            }
        }
    @Serializable
    data class AuthToken(val token: String)

    // Загрузка файла на сервер
    @Test
    fun testRootFilePost() = testApplication {
        // Setup
        val loginResponse = auth("root", this)

        // Test
        val response = client.post("/upload") {
            header(HttpHeaders.Authorization, "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}")
            contentType(ContentType.Application.Json)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("access", 1)
                        append("image", File("test/cross.png").readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"cross.png\"")
                        })
                    }
                )
            )
        }
        assertEquals("cross.png is uploaded to 'files/cross.png' with access level 1", response.bodyAsText())
        assertEquals(HttpStatusCode.Created, response.status)

        // TearDown
        File("files/cross.png").delete()
        fileService.deleteFile("cross.png")
    }

    // Скачивание файла с сервера
    @Test
    fun testRootFileGet() = testApplication {
        // Setup
        val loginResponse = auth("root", this)

        if (fileService.file("cross.png") == null) {
            val file = File("test/cross.png")
            File("files/cross.png").writeBytes(file.readBytes())

            fileService.addNewFile(ResponseFile("cross.png", 1))
        }

        // Test
        val response = client.get("/download") {
            header(
                HttpHeaders.Authorization,
                "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}"
            )
            parameter("name", "cross.png")

        }
        assertEquals(
            "attachment; filename=cross.png",
            response.headers[HttpHeaders.ContentDisposition]
        )

        // TearDown
        File("files/cross.png").delete()
        fileService.deleteFile("cross.png")

    }

    // Удаление файла с сервера
    @Test
    fun testRootFileDelete() = testApplication {
        // Setup
        val loginResponse = auth("root", this)

            if (fileService.file("cross.png") == null) {
                val file = File("test/cross.png")
                File("files/cross.png").writeBytes(file.readBytes())

                fileService.addNewFile(ResponseFile("cross.png", 1))
            }
        assertEquals(true, File("files/cross.png").exists())


        // Test
        val response = client.delete("/delete") {
            header(HttpHeaders.Authorization, "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}")
            parameter("name", "cross.png")
        }
        assertEquals(false, File("files/cross.png").delete())
        assertEquals(HttpStatusCode.OK, response.status)
    }




    // Изменение файла на сервере
    @Test
    fun testRootFileUpdate() = testApplication {
        // Setup
        val loginResponse = auth("root", this)

        if (fileService.file("cross.png") == null) {
            val file = File("test/cross.png")
            File("files/cross.png").writeBytes(file.readBytes())

            fileService.addNewFile(ResponseFile("cross.png", 1))


        }

        // Test
        val response = client.put("/files") {
            header(
                HttpHeaders.Authorization,
                "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}"
            )
            contentType(ContentType.Application.Json)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("access", 2)
                        append("image", File("test/index.html").readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "text/html")
                            append(HttpHeaders.ContentDisposition, "filename=\"index.html\"")
                        })
                    }
                )
            )
            parameter("file", "cross.png")

        }
        assertEquals(HttpStatusCode.OK, response.status)

        // TearDown
        File("files/index.html").delete()
        fileService.deleteFile("index.html")
    }

    // Получение списка файлов, доступных для взаимодействия
    @Test
    fun testRootFileListGet() = testApplication {
        // Setup
        val loginResponse = auth("root", this)


        // Test
        val response = client.get("/files") {
            header(HttpHeaders.Authorization, "Bearer ${Json.decodeFromString<AuthToken>(loginResponse.bodyAsText()).token}")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // Получение списка уровней доступа
    @Test
    fun testRootAccessGet() = testApplication {
        // Test
        val response = client.get("/access") {}
        assertEquals(HttpStatusCode.OK, response.status)
    }
}