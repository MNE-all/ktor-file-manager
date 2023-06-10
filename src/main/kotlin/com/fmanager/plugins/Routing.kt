package com.fmanager.plugins

import com.fmanager.routers.configureFileRouting
import com.fmanager.routers.configureUserRouting
import io.ktor.server.application.*

fun Application.configureRouting() {
    configureUserRouting()
    configureFileRouting()
}
