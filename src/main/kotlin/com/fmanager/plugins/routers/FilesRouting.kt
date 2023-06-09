package com.fmanager.plugins.routers

import com.fmanager.plugins.schemas.FileService
import com.fmanager.plugins.schemas.ResponseFile
import com.fmanager.utils.DatabaseFactory
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

// TODO post запрос в папку files/admin

fun Application.configureFileRouting() {
    routing {
        var fileDescription = ""
        var fileName = ""
        authenticate{
            post("/upload") {
                // Взятие информации с JWT токена
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()

                val multipartData = call.receiveMultipart()

                if (role > 1) {
                    multipartData.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                fileDescription = part.value
                            }

                            is PartData.FileItem -> {
                                fileName = part.originalFileName as String
                                val fileBytes = part.streamProvider().readBytes()
                                File("files/$fileName").writeBytes(fileBytes)

                                with(DatabaseFactory) {
                                    this.FileService.create(ResponseFile(fileName, fileDescription.toInt()))
                                }
                            }

                            else -> {}
                        }
                        part.dispose()
                    }

                    call.respondText("$fileDescription is uploaded to 'files/$fileName'")
                }
                else {
                    call.respondText("Недостаточно прав")
                }
            }
        }

        post("/upload/admin") {
            // Взятие информации с JWT токена
            val principal = call.principal<JWTPrincipal>()
            val role = principal!!.payload.getClaim("role").asInt()

            val multipartData = call.receiveMultipart()

            if (role == 3) {
                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            fileDescription = part.value
                        }

                        is PartData.FileItem -> {
                            fileName = part.originalFileName as String
                            val fileBytes = part.streamProvider().readBytes()
                            File("files/admin/$fileName").writeBytes(fileBytes)


                        }

                        else -> {}
                    }
                    part.dispose()
                }

                call.respondText("$fileDescription is uploaded to 'files/$fileName'")
            }
            else {
                call.respondText("Недостаточно прав")
            }
        }


        get("/download/txt") {
            val file = File("uploads/ТЗ для турфирмы.docx")
            if (file.exists()) {
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        "ТЗ для турфирмы.docx"
                    )
                        .toString()
                )
                call.respondFile(file)
            } else call.respond(HttpStatusCode.NotFound)
        }

        get("/download/png") {
            val file = File("uploads/bubble.png")
            if (file.exists()) {
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "bubble.png")
                        .toString()
                )
                call.respondFile(file)
            } else call.respond(HttpStatusCode.NotFound)
        }
    }
}