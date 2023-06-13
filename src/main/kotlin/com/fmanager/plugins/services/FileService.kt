package com.fmanager.plugins.services

import com.fmanager.dao.implementation.DAOFilesImpl
import com.fmanager.dao.interfaces.DAOFiles
import com.fmanager.plugins.schemas.ResponseFile
import io.ktor.http.content.*
import java.io.File

object FileService {
    private val fileServiceDatabase: DAOFiles = DAOFilesImpl().apply {}

    suspend fun uploadFile(multipartData: MultiPartData): String {
        var fileAccess = ""
        var fileName = ""

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

                    fileServiceDatabase.addNewFile(ResponseFile(fileName, fileAccess.toInt()))
                }

                else -> {}
            }
            part.dispose()
        }

        return "$fileName is uploaded to 'files/$fileName' with access level $fileAccess"
    }

    suspend fun downloadFile(name: String, role: Int): File?{
        val fileInfo = fileServiceDatabase.file(name)

        if (fileInfo != null && fileInfo.access <= role){
            val file = File("files/$name")
            if (file.exists()) {
                return file
            }
        }
        return null
    }

    suspend fun fileList(role: Int): List<com.fmanager.plugins.schemas.File>{
        return fileServiceDatabase.allFiles(role)
    }

    suspend fun editFile(name: String, role: Int, multipartData: MultiPartData): String? {
        var fileAccess = ""
        var fileName = ""

        val exposedFile = fileServiceDatabase.file(name)

        if (role > 1 && exposedFile != null && role >= exposedFile.access) {
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

                        if (fileName == name) {
                            File("files/$name").delete()
                            File("files/$fileName").writeBytes(fileBytes)
                        } else {
                            File("files/$fileName").writeBytes(fileBytes)
                            File("files/$name").delete()
                        }

                        fileServiceDatabase.editFile(name, ResponseFile(fileName, fileAccess.toInt()))
                    }
                    else -> {}
                }
                part.dispose()
            }
            return "$name is update to 'files/$fileName' with access level $fileAccess"
        }
        else {
            return null
        }
    }

    suspend fun fileDelete(name: String, role: Int): Boolean {
        if (role > 2) {
            val file = File("files/$name")
            if (file.delete()) {
                fileServiceDatabase.deleteFile(name)
                return true
            }

        }
        return false
    }
}