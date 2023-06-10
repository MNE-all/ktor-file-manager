package com.fmanager

import com.fmanager.plugins.configureSecurity
import com.fmanager.plugins.configureSerialization
import com.fmanager.plugins.routers.configureFileRouting
import com.fmanager.plugins.schemas.ResponseUser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test

class ApplicationTestFilesRouting {
    @Serializable
    data class AuthToken(val token: String)

    // Загрузка файла на сервер
    @Test
    fun testRootPostUser() = testApplication {
        application {
            configureSerialization()
            configureSecurity()
            configureFileRouting()
        }

        val response = client.post("/users") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(ResponseUser("Test", "K1G9APpTuEHpFx3dTDT8", "Brains")))
        }
        Assert.assertEquals("The 'K1G9APpTuEHpFx3dTDT8' account is created", response.bodyAsText())
        Assert.assertEquals(HttpStatusCode.Created, response.status)
    }
}