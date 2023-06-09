package com.fmanager

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import com.fmanager.plugins.*
import com.fmanager.plugins.routers.configureFileRouting
import com.fmanager.plugins.routers.configureRouting
import com.fmanager.plugins.routers.configureUserRouting

fun main() {
    embeddedServer(CIO, port = 4444, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSecurity()
    configureSerialization()
    configureUserRouting()
    configureFileRouting()
    configureRouting()
}
