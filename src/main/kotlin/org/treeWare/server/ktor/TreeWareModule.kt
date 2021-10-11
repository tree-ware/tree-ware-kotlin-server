package org.treeWare.server.ktor

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.treeWare.server.common.TreeWareServer
import java.io.InputStreamReader

fun Application.treeWareModule(
    environment: String,
    metaModelFiles: List<String>,
    logMetaModelFullNames: Boolean
) {
    val treeWareServer = TreeWareServer(environment, metaModelFiles, logMetaModelFullNames)
    val rootName = snakeCaseToKebabCase(treeWareServer.rootName)

    routing {
        route("/tree-ware/api") {
            post("echo/$rootName") {
                // TODO(deepak-nulu): load-test to ensure InputStream does not limit concurrency
                val reader = InputStreamReader(call.receiveStream())
                call.respondTextWriter { treeWareServer.echo(reader, this) }
            }
        }
    }
}

private fun snakeCaseToKebabCase(snake: String): String = snake.split("_").joinToString("-")