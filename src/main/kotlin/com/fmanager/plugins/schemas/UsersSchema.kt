package com.fmanager.plugins.schemas

import AccessService
import com.fmanager.utils.PasswordSecure
import com.fmanager.utils.UUIDSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

@Serializable
data class SystemUserInfo(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val login: String,
    val password: ByteArray,
    val salt: ByteArray,
    val role: Int)
@Serializable
data class ResponseUser(val name: String, val login: String, val password: String)

class UserService(database: Database) {

    object Users : Table() {
        val id = uuid("id").autoGenerate()
        val name = varchar("name", length = 50)
        val login = varchar("login", length = 50).uniqueIndex()
        val password = binary("password", 128)
        val salt = binary("salt", 128)
        val role = reference("access", AccessService.Access)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users)
        }

        // Проверка количества пользователей
        // Для
        CoroutineScope(Dispatchers.IO).launch {
            if (readAll().isEmpty()){
                dbQuery {
                    val newSalt = PasswordSecure.generateRandomSalt()
                    Users.insert {
                        it[name] = "admin"
                        it[login] = "admin"
                        it[password] = PasswordSecure.generateHash("root", newSalt)
                        it[role] = 3
                        it[salt] = newSalt
                    }[Users.id]
                }
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(user: ResponseUser): String = dbQuery {
        val newSalt = PasswordSecure.generateRandomSalt()
        Users.insert {
            it[name] = user.name
            it[login] = user.login
            it[password] = PasswordSecure.generateHash(user.password, newSalt)
            it[role] = 1
            it[salt] = newSalt
        }[Users.login]
    }

    suspend fun read(login: String): ResponseUser? {
        return dbQuery {
            Users.select { Users.login eq login }
                .map { ResponseUser(it[Users.name], it[Users.login], it[Users.password].decodeToString()) }
                .singleOrNull()
        }
    }

    suspend fun readAll(): List<SystemUserInfo> = dbQuery {
        val list: MutableList<SystemUserInfo> = mutableListOf()
        val query = Users.selectAll()
        query.forEach {
            list.add(
                SystemUserInfo(
                    it[Users.id],
                    it[Users.login],
                    it[Users.password],
                    it[Users.salt],
                    it[Users.role].value
                )
            )
        }
        list

    }

    suspend fun update(id: String, user: ResponseUser) {
        dbQuery {
            val newSalt = PasswordSecure.generateRandomSalt()
            Users.update({ Users.login eq id}) {
                it[name] = user.name
                it[login] = user.login
                it[password] = PasswordSecure.generateHash(user.password, newSalt)
                it[salt] = newSalt
            }
        }
    }

    suspend fun changeRole(login: String, newRole: Int) {
        dbQuery {
            Users.update({ Users.login eq login}) {
                it[role] = newRole
            }
        }
    }

    suspend fun delete(login: String) {
        dbQuery {
            Users.deleteWhere { Users.login.eq(login) }
        }
    }
}