package org.tree_ware.server.ktor

import com.datastax.oss.driver.api.core.CqlSession
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.tree_ware.cassandra.schema.map.MutableSchemaMap
import org.tree_ware.schema.core.MutableSchema
import org.tree_ware.server.common.TreeWareServer
import java.io.InputStreamReader

fun Application.treeWareModule(
    environment: String,
    schema: MutableSchema,
    schemaMap: MutableSchemaMap,
    cqlSession: CqlSession,
    logSchemaFullNames: Boolean
) {
    val treeWareServer = TreeWareServer(environment, schema, schemaMap, cqlSession, logSchemaFullNames)
    if (!treeWareServer.isValid) return

    val rootName = snakeCaseToKebabCase(schema.root.name)

    routing {
        route("/tree-ware/api") {
            post("echo/$rootName") {
                // TODO(deepak-nulu): load-test to ensure InputStream does not limit concurrency
                val reader = InputStreamReader(call.receiveStream())
                call.respondTextWriter { treeWareServer.echo(reader, this) }
            }
            post("set/$rootName") {
                val reader = InputStreamReader(call.receiveStream())
                call.respondTextWriter { treeWareServer.set(reader, this) }
            }
        }
    }
}

private fun snakeCaseToKebabCase(snake: String): String = snake.split("_").joinToString("-")