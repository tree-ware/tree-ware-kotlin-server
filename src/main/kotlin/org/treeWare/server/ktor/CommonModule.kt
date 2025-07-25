package org.treeWare.server.ktor

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.commonModule() {
    install(DefaultHeaders)
    install(CallLogging)

    routing {
        get("/health") {
            call.respondText("ok")
        }
    }
}