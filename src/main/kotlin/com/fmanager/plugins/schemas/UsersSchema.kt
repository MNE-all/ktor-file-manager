package com.fmanager.plugins.schemas

import AccessService
import com.fmanager.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


@Serializable
data class User(
    @Serializable(with = UUIDSerializer::class)
    var uuid: UUID,
    var name: String,
    var login: String,
    var password: ByteArray,
    var salt: ByteArray,
    var role: Int
)
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
    }
}