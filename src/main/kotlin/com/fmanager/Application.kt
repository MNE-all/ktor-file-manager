package com.fmanager

import com.fmanager.plugins.DatabaseFactory
import com.fmanager.plugins.configureRouting
import com.fmanager.plugins.configureSecurity
import com.fmanager.plugins.configureSerialization
import com.fmanager.plugins.services.UserService
import com.fmanager.utils.generateHash
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)


fun Application.module() {
    DatabaseFactory
    UserService(this::generateHash)
    configureSecurity()
    configureSerialization()
    configureRouting()
}
