package com.fmanager.routers

import com.fmanager.plugins.DatabaseFactory.AccessService
import com.fmanager.plugins.services.FileService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureFileRouting() {
    routing {
        authenticate {
            // Загрузка файла на сервер (в папку files)
            post("/upload") {
                // Взятие информации с JWT токена
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()

                val multipartData = call.receiveMultipart()

                if (role > 1) {
                    call.respondText(FileService.uploadFile(multipartData))
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

                val file = FileService.downloadFile(name, role)

                if (file != null) {
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(
                            ContentDisposition.Parameters.FileName,
                            name
                        )
                            .toString()
                    )
                    call.respondFile(file)
                    call.respond(HttpStatusCode.OK)
                }
                else {
                    call.respond(hashMapOf("error" to "Недостаточно прав или файл не найден!"))
                    call.respond(HttpStatusCode.NotFound)
                }


            }

            // Получение списка файлов, доступных для взаимодействия (из папки files)
            get("/files") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()

                call.respond(FileService.fileList(role))
                call.respond(HttpStatusCode.OK)

            }

            // Изменеие файла на сервере (в папке files)
            put("/files") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()

                val file = (call.request.queryParameters["file"] ?: throw IllegalArgumentException("Invalid file name"))

                val multipartData = call.receiveMultipart()

                val result = FileService.editFile(file, role, multipartData)
                if (result != null) {
                    call.respondText(result)
                    call.response.status(HttpStatusCode.OK)
                }
                else {
                    call.respondText("Недостаточно прав или файл не найден!")
                }

            }

            // Удаление файла на сервере (в папке files)
            delete("/delete"){
                val principal = call.principal<JWTPrincipal>()
                val role = principal!!.payload.getClaim("role").asInt()
                val name = call.request.queryParameters["name"] ?: throw IllegalArgumentException("Invalid file name")

                if (FileService.fileDelete(name, role)) {
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