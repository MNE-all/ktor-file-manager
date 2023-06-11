package com.fmanager.dao.users

import com.fmanager.plugins.schemas.User

interface DAOUsers {
    suspend fun allUsers(): List<User>
    suspend fun user(login: String): User?
    suspend fun addNewUser(name: String, login: String, password: String): User?
    suspend fun editUser(oldLogin: String, newLogin: String = oldLogin, newName: String, newPassword: String): Boolean
    suspend fun changeRole(login: String, newRole: Int): Boolean
    suspend fun deleteUser(login: String): Boolean
}