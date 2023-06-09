import com.fmanager.plugins.schemas.FileService.Files.autoIncrement
import com.fmanager.plugins.schemas.FileService.Files.uniqueIndex
import com.fmanager.plugins.schemas.ResponseUser
import com.fmanager.plugins.schemas.SystemUserInfo
import com.fmanager.plugins.schemas.UserService
import com.fmanager.utils.PasswordSecure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedAccess(val id: Int, val name: String)

@Serializable
data class ResponseAccess(val name: String)

// TODO дописать создание 3 ролей

class AccessService(private val database: Database) {
    object Access : IntIdTable() {
        val name = varchar("name", length = 80).uniqueIndex()
    }

    init {
        transaction(database) {
            SchemaUtils.create(Access)
        }

        // Проверка количества доступов
        // Для генереации, в случае отсутсвия
        CoroutineScope(Dispatchers.IO).launch {
            if (readAll().isEmpty()){
                dbQuery {
                    Access.insert {
                        it[name] = "user"
                    }
                    Access.insert {
                        it[name] = "writer"
                    }
                    Access.insert {
                        it[name] = "admin"
                    }
                }
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(access: ResponseAccess): String = dbQuery {
        Access.insert {
            it[name] = access.name
        }[Access.name]
    }

    suspend fun read(id: Int): ExposedAccess? {
        return dbQuery {
            AccessService.Access.select { Access.id eq id }
                .map { ExposedAccess(it[Access.id].value, it[Access.name]) }
                .singleOrNull()
        }
    }

    suspend fun readAll(): List<ExposedAccess> = dbQuery {
        var list: MutableList<ExposedAccess> = mutableListOf()
        val query = Access.selectAll()
        query.forEach {
            list.add(
                ExposedAccess(
                    it[Access.id].value,
                    it[Access.name]
                )
            )
        }
        list
    }

    suspend fun update(id: Int, access: ResponseAccess) {
        dbQuery {
            Access.update({ Access.id eq id}) {
                it[name] = access.name
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Access.deleteWhere { Access.id.eq(id) }
        }
    }

}