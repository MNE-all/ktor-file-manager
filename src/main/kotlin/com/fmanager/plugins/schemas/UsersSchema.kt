package com.fmanager.plugins.schemas

import com.fmanager.utils.PasswordSecure
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import java.util.UUID


@Serializable
data class ExposedUser(val name: String, val login: String, val password: String, val role: Int)
@Serializable
data class SystemUserInfo(val uuid: String,
                          val login: String,
                          val password: ByteArray, val salt: ByteArray,
                          val role: Int)

class UserService(private val database: Database) {

    object Users : Table() {
        val id = uuid("id").autoGenerate()
        val name = varchar("name", length = 50)
        val login = varchar("login", length = 50).uniqueIndex()
        val password = binary("password", 128)
        val salt = binary("salt", 128)
        val role = integer("role")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(user: ExposedUser): String = dbQuery {
        val newSalt = PasswordSecure.generateRandomSalt()
        Users.insert {
            it[name] = user.name
            it[login] = user.login
            it[password] = PasswordSecure.generateHash(user.password, newSalt)
            it[role] = user.role
            it[salt] = newSalt
        }[Users.id].toString()
    }

    suspend fun read(id: UUID): ExposedUser? {
        return dbQuery {
            Users.select { Users.id eq id }
                .map { ExposedUser(it[Users.name], it[Users.login], it[Users.password].decodeToString(), it[Users.role]) }
                .singleOrNull()
        }
    }

    suspend fun readAll(): List<SystemUserInfo> = dbQuery {
        var list: MutableList<SystemUserInfo> = mutableListOf()
        val query = Users.selectAll()
        query.forEach {
            list.add(
                SystemUserInfo(
                    it[Users.id].toString(),
                    it[Users.login],
                    it[Users.password],
                    it[Users.salt],
                    it[Users.role]
                )
            )
        }
        list

    }

    suspend fun update(id: UUID, user: ExposedUser) {
        dbQuery {
            val newSalt = PasswordSecure.generateRandomSalt()
            Users.update({ Users.id eq id}) {
                it[name] = user.name
                it[login] = user.login
                it[password] = PasswordSecure.generateHash(user.password, newSalt)
                it[salt] = newSalt
                it[role] = user.role
            }
        }
    }

    suspend fun delete(id: UUID) {
        dbQuery {
            Users.deleteWhere { Users.id.eq(id) }
        }
    }
}