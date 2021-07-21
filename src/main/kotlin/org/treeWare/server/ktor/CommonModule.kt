package org.treeWare.server.ktor

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing

fun Application.commonModule() {
    install(DefaultHeaders)
    install(CallLogging)

    routing {
        get("/health") {
            call.respondText("ok")
        }
    }
}
