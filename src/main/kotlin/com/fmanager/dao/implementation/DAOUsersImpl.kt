package com.fmanager.dao.implementation

import com.fmanager.dao.interfaces.DAOUsers
import com.fmanager.plugins.DatabaseFactory.dbQuery
import com.fmanager.plugins.schemas.User
import com.fmanager.plugins.schemas.UserService
import com.fmanager.utils.PasswordSecure
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class DAOUsersImpl: DAOUsers {
    private fun resultRowToArticle(row: ResultRow) = User(
        uuid = row[UserService.Users.id],
        name = row[UserService.Users.name],
        login = row[UserService.Users.login],
        password = row[UserService.Users.password],
        salt = row[UserService.Users.salt],
        role = row[UserService.Users.role].value
    )
    override suspend fun allUsers(): List<User> = dbQuery{
        UserService.Users.selectAll().map(::resultRowToArticle)
    }

    override suspend fun user(login: String): User? = dbQuery{
        UserService.Users
            .select { UserService.Users.login eq login }
            .map(::resultRowToArticle)
            .singleOrNull()
    }

    override suspend fun addNewUser(name: String, login: String, password: String): User? = dbQuery{
        val newSalt = PasswordSecure.generateRandomSalt()
        val insertStatement = UserService.Users.insert {
            it[UserService.Users.name] = name
            it[UserService.Users.login] = login
            it[UserService.Users.password] = PasswordSecure.generateHash(password, newSalt)
            it[role] = 1
            it[salt] = newSalt
        }
        insertStatement.resultedValues?.singleOrNull()?.let(::resultRowToArticle)
    }

    suspend fun init(name: String, login: String, password: String): User? = dbQuery{
        val newSalt = PasswordSecure.generateRandomSalt()
        val insertStatement = UserService.Users.insert {
            it[UserService.Users.name] = name
            it[UserService.Users.login] = login
            it[UserService.Users.password] = PasswordSecure.generateHash(password, newSalt)
            it[role] = 3
            it[salt] = newSalt
        }
        insertStatement.resultedValues?.singleOrNull()?.let(::resultRowToArticle)
    }


    override suspend fun editUser(oldLogin: String, newLogin: String,
                                  newName: String, newPassword: String): Boolean = dbQuery{
        val newSalt = PasswordSecure.generateRandomSalt()
        UserService.Users.update({ UserService.Users.login eq oldLogin }) {
            it[name] = newName
            it[login] = newLogin
            it[password] = PasswordSecure.generateHash(newPassword, newSalt)
            it[salt] = newSalt
        } > 0
    }

    override suspend fun changeRole(login: String, newRole: Int) = dbQuery{
        UserService.Users.update({ UserService.Users.login eq login}) {
            it[role] = newRole
        } > 0
    }

    override suspend fun deleteUser(login: String): Boolean = dbQuery{
        UserService.Users.deleteWhere { UserService.Users.login.eq(login) } > 0
    }
}