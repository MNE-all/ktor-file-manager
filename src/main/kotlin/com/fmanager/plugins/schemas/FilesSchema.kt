package com.fmanager.plugins.schemas

import com.fmanager.plugins.schemas.UserService.Users.autoGenerate
import com.fmanager.plugins.schemas.UserService.Users.uniqueIndex
import com.fmanager.utils.PasswordSecure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedFile(val id: Int, val name: String, val access: Int)
@Serializable
data class ResponseFile(val name: String, val access: Int)

class FileService(private val database: Database) {
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

    suspend fun read(id: Int): ExposedFile? {
        return dbQuery {
            Files.select { Files.id eq id }
                .map { ExposedFile(it[Files.id].value, it[Files.name], it[Files.access].value) }
                .singleOrNull()
        }
    }

    suspend fun readAll(): List<ExposedFile> = dbQuery {
        var list: MutableList<ExposedFile> = mutableListOf()
        val query = Files.selectAll()
        query.forEach {
            list.add(
                ExposedFile(
                    it[Files.id].value,
                    it[Files.name],
                    it[Files.access].value,
                )
            )
        }
        list
    }

    suspend fun update(id: Int, file: ResponseFile) {
        dbQuery {
            Files.update({ Files.id eq id}) {
                it[name] = file.name
                it[access] = file.access
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Files.deleteWhere { Files.id.eq(id) }
        }
    }
}