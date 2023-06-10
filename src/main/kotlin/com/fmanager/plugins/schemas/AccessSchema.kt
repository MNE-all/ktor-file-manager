import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedAccess(val id: Int, val name: String)



class AccessService(database: Database) {
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

    suspend fun readAll(): List<ExposedAccess> = dbQuery {
        val list: MutableList<ExposedAccess> = mutableListOf()
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
}