package com.fmanager

import com.fmanager.plugins.DatabaseFactory
import com.fmanager.plugins.configureRouting
import com.fmanager.plugins.configureSecurity
import com.fmanager.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*

fun main() {
    embeddedServer(CIO, port = 4444, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseFactory.UserService
    DatabaseFactory
    configureSecurity()
    configureSerialization()
    configureRouting()
}
