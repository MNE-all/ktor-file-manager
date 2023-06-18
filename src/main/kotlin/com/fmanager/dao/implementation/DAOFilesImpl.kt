package com.fmanager.dao.implementation

import com.fmanager.dao.interfaces.DAOFiles
import com.fmanager.plugins.DatabaseFactory.dbQuery
import com.fmanager.plugins.schemas.File
import com.fmanager.plugins.schemas.FileService
import com.fmanager.plugins.schemas.ResponseFile
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class DAOFilesImpl: DAOFiles {
    override suspend fun allFiles(role: Int): List<File> = dbQuery{
        val list: MutableList<File> = mutableListOf()
        val query = FileService.Files.selectAll()
        query.forEach {
            if (it[FileService.Files.access].value <= role){
                list.add(
                    File(
                        it[FileService.Files.id].value,
                        it[FileService.Files.name],
                        it[FileService.Files.access].value,
                    )
                )
            }
        }
        list
    }

    override suspend fun file(name: String): File? = dbQuery{
        FileService.Files.select { FileService.Files.name eq name }
            .map { File(it[FileService.Files.id].value, it[FileService.Files.name], it[FileService.Files.access].value) }
            .singleOrNull()
    }

    override suspend fun addNewFile(file: ResponseFile): String = dbQuery{
        FileService.Files.insert {
            it[name] = file.name
            it[access] = file.access
        }[FileService.Files.name]
    }

    override suspend fun editFile(fileName: String, file: ResponseFile): Boolean = dbQuery{
        FileService.Files.update({ FileService.Files.name eq fileName}) {
            it[name] = file.name
            it[access] = file.access
        } > 1
    }

    override suspend fun deleteFile(name: String): Boolean = dbQuery{
        FileService.Files.deleteWhere { FileService.Files.name.eq(name) } > 1
    }
}