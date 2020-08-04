package org.tree_ware.server.ktor

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.request.receiveStream
import io.ktor.response.respondTextWriter
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import org.tree_ware.cassandra.schema.map.MutableSchemaMap
import org.tree_ware.schema.core.MutableSchema
import org.tree_ware.server.common.TreeWareServer
import java.io.InputStreamReader

fun Application.treeWareModule(schema: MutableSchema, schemaMap: MutableSchemaMap, logSchemaFullNames: Boolean) {
    val treeWareServer = TreeWareServer(schema, schemaMap, logSchemaFullNames)
    if (!treeWareServer.isValid) return

    val rootName = snakeCaseToKebabCase(schema.root.name)

    routing {
        route("/tree-ware/api/$rootName") {
            post("echo") {
                // TODO(deepak-nulu): load-test to ensure InputStream does not limit concurrency
                val reader = InputStreamReader(call.receiveStream())
                call.respondTextWriter { treeWareServer.echo(reader, this) }
            }
        }
    }
}

private fun snakeCaseToKebabCase(snake: String): String = snake.split("_").joinToString("-")