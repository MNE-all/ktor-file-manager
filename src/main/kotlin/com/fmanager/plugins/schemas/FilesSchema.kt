package com.fmanager.plugins.schemas

import AccessService
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class File(val id: Int, val name: String, val access: Int)
@Serializable
data class ResponseFile(val name: String, val access: Int)

class FileService(database: Database) {
    object Files : IntIdTable() {
        val name = varchar("name", length = 80).uniqueIndex()
        val access = reference("access", AccessService.Access)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Files)
        }
    }
}