package com.fmanager.plugins.routers

import com.fmanager.plugins.schemas.ResponseFile
import com.fmanager.utils.DatabaseFactory
import com.fmanager.utils.DatabaseFactory.AccessService
import com.fmanager.utils.DatabaseFactory.FileService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File


fun Application.configureFileRouting() {
    routing {
        var fileAccess = ""
        var fileName = ""
        authenticate {
            // Загрузка файла на сервер (в папку files)
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

                                FileService.create(ResponseFile(fileName, fileAccess.toInt()))
                            }

                            else -> {}
                        }
                        part.dispose()
                    }

                    call.respondText("$fileName is uploaded to 'files/$fileName' with access level $fileAccess")
                    call.response.status(HttpStatusCode.Created)
                } else {
                    call.respondText("Недостаточно прав")
                }
            }

            // Скачивание файла с сервера (из папки files)
            get("/download") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()

                val name = (call.request.queryParameters["name"] ?: throw IllegalArgumentException("Invalid file name"))

                val fileInfo = FileService.read(name)

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

            // Получение списка файлов, доступных для взаимодействия (из папки files)
            get("/files") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()

                call.respond(FileService.readAll(role))
                call.respond(HttpStatusCode.OK)

            }

            // Изменеие файла на сервере (в папке files)
            put("/files") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()

                val file = (call.request.queryParameters["file"] ?: throw IllegalArgumentException("Invalid file name"))

                val multipartData = call.receiveMultipart()
                val expodsedFile = FileService.read(file)

                if (role > 1 && expodsedFile != null && role >= expodsedFile.access) {
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

                                if (fileName == file) {
                                    File("files/$file").delete()
                                    File("files/$fileName").writeBytes(fileBytes)
                                } else {
                                    File("files/$fileName").writeBytes(fileBytes)
                                    File("files/$file").delete()
                                }

                                with(DatabaseFactory) {
                                    this.FileService.update(file, ResponseFile(fileName, fileAccess.toInt()))
                                }
                            }

                            else -> {}
                        }
                        part.dispose()
                    }
                    call.respondText("$file is update to 'files/$fileName' with access level $fileAccess")
                    call.response.status(HttpStatusCode.OK)
                } else {
                    call.respondText("Недостаточно прав или файл не найден!")
                }

            }

            // Удаление файла на сервере (в папке files)
            delete("/delete"){
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()
                if (role > 2) {
                    val name =
                        (call.request.queryParameters["name"] ?: throw IllegalArgumentException("Invalid file name"))

                    val file = File("files/$name")
                    file.delete()
                    FileService.delete(name)
                    call.respond(HttpStatusCode.OK)

                }
                else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }

        // Получение списка уровней доступа
        get("/access") {
            call.respond(AccessService.readAll())
            call.respond(HttpStatusCode.OK)

        }
    }
}