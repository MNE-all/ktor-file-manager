package com.fmanager.utils

import com.fmanager.plugins.schemas.UserService
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    private var userService: UserService
    val UserService: UserService
        get() {
            return userService
        }

    init {
        val driverClassName = "org.h2.Driver"
        val jdbcURL = "jdbc:h2:file:./build/databaseV2"
        val database = Database.connect(jdbcURL, driverClassName)
        userService = UserService(database)
    }
}