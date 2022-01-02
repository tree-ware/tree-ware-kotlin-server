package org.treeWare.server.ktor

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.treeWare.server.common.TreeWareServer
import java.io.InputStreamReader

fun Application.treeWareModule(treeWareServer: TreeWareServer) {
    val mainMetaName = snakeCaseToKebabCase(treeWareServer.mainMetaName)

    routing {
        route("/tree-ware/api") {
            post("echo/$mainMetaName") {
                // TODO(deepak-nulu): load-test to ensure InputStream does not limit concurrency
                withContext(Dispatchers.IO) {
                    val reader = InputStreamReader(call.receiveStream())
                    call.respondTextWriter { treeWareServer.echo(reader, this) }
                }
            }

            post("set/$mainMetaName") {
                withContext(Dispatchers.IO) {
                    val reader = InputStreamReader(call.receiveStream())
                    call.respondTextWriter { treeWareServer.set(reader, this) }
                }
            }
        }
    }
}

private fun snakeCaseToKebabCase(snake: String): String = snake.split("_").joinToString("-")