package com.fmanager.plugins.schemas

import AccessService
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedFile(val id: Int, val name: String, val access: Int)
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

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(file: ResponseFile): String = dbQuery {
        Files.insert {
            it[name] = file.name
            it[access] = file.access
        }[Files.name]
    }

    suspend fun read(name: String): ExposedFile? {
        return dbQuery {
            Files.select { Files.name eq name }
                .map { ExposedFile(it[Files.id].value, it[Files.name], it[Files.access].value) }
                .singleOrNull()
        }
    }

    suspend fun readAll(role: Int): List<ExposedFile> = dbQuery {
        val list: MutableList<ExposedFile> = mutableListOf()
        val query = Files.selectAll()
        query.forEach {
            if (it[Files.access].value <= role){
                list.add(
                    ExposedFile(
                        it[Files.id].value,
                        it[Files.name],
                        it[Files.access].value,
                    )
                )
            }
        }
        list
    }

    suspend fun update(fileName: String, file: ResponseFile) {
        dbQuery {
            Files.update({ Files.name eq fileName}) {
                it[name] = file.name
                it[access] = file.access
            }
        }
    }

    suspend fun delete(name: String) {
        dbQuery {
            Files.deleteWhere { Files.name.eq(name) }
        }
    }
}