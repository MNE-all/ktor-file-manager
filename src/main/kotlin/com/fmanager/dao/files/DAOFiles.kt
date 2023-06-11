package com.fmanager.dao.files

import com.fmanager.plugins.schemas.File
import com.fmanager.plugins.schemas.ResponseFile

interface DAOFiles {
        suspend fun allFiles(role: Int): List<File>
        suspend fun file(name: String): File?
        suspend fun addNewFile(file: ResponseFile): String?
        suspend fun editFile(fileName: String, file: ResponseFile): Boolean
        suspend fun deleteFile(name: String): Boolean
}