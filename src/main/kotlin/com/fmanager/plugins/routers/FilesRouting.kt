package com.fmanager.plugins.routers

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
        var fileAccess = ""
        var fileName = ""
        authenticate {
            post("/upload") {
                // Взятие информации с JWT токена
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()

                val multipartData = call.receiveMultipart()

                if (role > 1) {
                    multipartData.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                if (part.name == "access") {
                                    fileAccess = part.value
                                }
                            }

                            is PartData.FileItem -> {
                                fileName = part.originalFileName as String
                                val fileBytes = part.streamProvider().readBytes()
                                File("files/$fileName").writeBytes(fileBytes)

                                with(DatabaseFactory) {
                                    var x = fileAccess.toInt()
                                    this.FileService.create(ResponseFile(fileName, x))
                                }
                            }

                            else -> {}
                        }
                        part.dispose()
                    }

                    call.respondText("$fileName is uploaded to 'files/$fileName' with access level $fileAccess")
                } else {
                    call.respondText("Недостаточно прав")
                }
            }


            get("/download") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()

                val name = (call.request.queryParameters["name"] ?: throw IllegalArgumentException("Invalid file name"))

                with(DatabaseFactory) {
                    val fileInfo = this.FileService.read(name)

                    if (fileInfo != null && fileInfo.access <= role) {
                        val file = File("files/$name")
                        if (file.exists()) {
                            call.response.header(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Attachment.withParameter(
                                    ContentDisposition.Parameters.FileName,
                                    name
                                )
                                    .toString()
                            )
                            call.respondFile(file)
                        } else call.respond(HttpStatusCode.NotFound)
                    } else {
                        call.respond(hashMapOf("error" to "Недостаточно прав или файл не найден!"))
                        call.respond(HttpStatusCode.NotFound)
                    }
                }


            }
        }

        get("/files") {
            with(DatabaseFactory) {
                call.respond(this.FileService.readAll())
                call.respond(HttpStatusCode.OK)
            }
        }

        get("/access") {
            with(DatabaseFactory) {
                call.respond(this.AccessService.readAll())
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}