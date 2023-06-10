package com.fmanager.utils

import AccessService
import com.fmanager.plugins.schemas.FileService
import com.fmanager.plugins.schemas.UserService
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    private var userService: UserService
    private var accessService: AccessService
    private var fileService: FileService
    val UserService: UserService
        get() {
            return userService
        }
    val AccessService: AccessService
        get() {
            return accessService
        }
    val FileService: FileService
        get() {
            return fileService
        }

    init {
        val driverClassName = "org.h2.Driver"
        val jdbcURL = "jdbc:h2:file:./build/file-manager-database"
        val database = Database.connect(jdbcURL, driverClassName)
        userService = UserService(database)
        accessService = AccessService(database)
        fileService = FileService(database)
    }
}