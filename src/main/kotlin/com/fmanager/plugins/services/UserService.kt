package com.fmanager.plugins.services

import com.fmanager.dao.implementation.DAOUsersImpl
import com.fmanager.dao.interfaces.DAOUsers
import com.fmanager.plugins.schemas.ResponseUser
import com.fmanager.plugins.schemas.User
import kotlinx.coroutines.runBlocking

class UserService(val getHash: ((String, ByteArray) -> ByteArray)?) {
    private val userServiceDatabase: DAOUsers = DAOUsersImpl().apply {
        runBlocking {
            if (allUsers().isEmpty()) {
                init("admin", "admin", "root", getHash!!)
            }
        }
    }

    suspend fun login(login: String, password: String): Int? {
        val user = userServiceDatabase.user(login)
        if (user != null) {
            val hash = getHash!!(password, user.salt).decodeToString()
            val trueHase = user.password.decodeToString()

            if (trueHase == hash) {
                return user.role
            }
        }
        return null
    }

    suspend fun getCurrentUser(login: String): User? {
        return userServiceDatabase.user(login)
    }

    suspend fun editUser(login: String, user: ResponseUser): Boolean {
        if (login != "admin" || user.login == "admin") {
            userServiceDatabase.editUser(login, user.login, user.name, user.password, getHash!!)
            return true
        }
        return false
    }

    suspend fun deleteUser(login: String): HashMap<String, String> {
        if (login != "admin") {
            val startAmount = userServiceDatabase.allUsers().count()
            userServiceDatabase.deleteUser(login)
            return if (startAmount - 1 == userServiceDatabase.allUsers().count()) {
                hashMapOf("success" to "Пользователь '$login' успешно удалён!")
            } else {
                hashMapOf("error" to "Данного пользователя не существует!")
            }
        }
        return hashMapOf("error" to "Невозможно удаление данного пользователя!")
    }

    suspend fun changeAccess (role: Int, newRole: Int, login: String): Boolean {
        if (role > 2) {
            if (userServiceDatabase.user(login)?.login != "admin" || newRole > 2) {
                userServiceDatabase.changeRole(login, newRole)
                return true
            }
        }
        return false
    }

    suspend fun addUser(user: ResponseUser): String? {
        return try {
            userServiceDatabase.addNewUser(user.name, user.login, user.password, getHash!!)!!.login
        }
        catch (e: Exception) {
            null
        }
    }

    suspend fun allUsers(): List<User> {
        return userServiceDatabase.allUsers()
    }



}