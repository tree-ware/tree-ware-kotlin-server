package org.treeWare.server.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.treeWare.model.encoder.EncodePasswords
import org.treeWare.model.encoder.JsonWireFormatEncoder
import org.treeWare.model.encoder.encodeJson
import org.treeWare.model.operator.ElementModelError
import org.treeWare.model.operator.get.GetResponse
import org.treeWare.model.operator.set.SetResponse
import org.treeWare.server.common.EchoResponse
import org.treeWare.server.common.TreeWareServer
import java.io.InputStreamReader
import java.io.Writer

fun Application.treeWareModule(treeWareServer: TreeWareServer) {
    val mainMetaName = snakeCaseToKebabCase(treeWareServer.mainMetaName)

    routing {
        route("/tree-ware/api") {
            post("echo/$mainMetaName") {
                // TODO(deepak-nulu): load-test to ensure InputStream does not limit concurrency
                withContext(Dispatchers.IO) {
                    val reader = InputStreamReader(call.receiveStream())
                    when (val echoResponse = treeWareServer.echo(reader)) {
                        is EchoResponse.Model -> call.respondTextWriter(ContentType.Application.Json) {
                            // TODO(deepak-nulu): get prettyPrint value from URL query-param.
                            // TODO(deepak-nulu): get encodePasswords value from URL query-param.
                            // TODO(deepak-nulu): report decodeErrors once they are in aux form.
                            encodeJson(
                                echoResponse.model,
                                this,
                                treeWareServer.modelMultiAuxEncoder,
                                EncodePasswords.ALL,
                                true
                            )
                        }
                        is EchoResponse.ErrorList -> call.respondTextWriter(
                            ContentType.Text.Plain,
                            HttpStatusCode.BadRequest
                        ) {
                            writeStringList(this, echoResponse.errorList)
                        }
                    }
                }
            }

            post("set/$mainMetaName") {
                withContext(Dispatchers.IO) {
                    val reader = InputStreamReader(call.receiveStream())
                    when (val setResponse = treeWareServer.set(reader)) {
                        is SetResponse.Success -> call.respond(HttpStatusCode.OK, "")
                        is SetResponse.ErrorList -> call.respondTextWriter(
                            ContentType.Application.Json,
                            HttpStatusCode.BadRequest
                        ) {
                            writeErrorList(this, setResponse.errorList, true)
                        }
                        is SetResponse.ErrorModel -> call.respondTextWriter(
                            ContentType.Application.Json,
                            HttpStatusCode.BadRequest
                        ) {
                            // TODO(deepak-nulu): get prettyPrint value from URL query-param.
                            encodeJson(
                                setResponse.errorModel,
                                this,
                                treeWareServer.modelMultiAuxEncoder,
                                EncodePasswords.ALL,
                                true
                            )
                        }
                        null -> call.respondText("")
                    }
                }
            }

            post("get/$mainMetaName") {
                withContext(Dispatchers.IO) {
                    val reader = InputStreamReader(call.receiveStream())
                    when (val getResponse = treeWareServer.get(reader)) {
                        is GetResponse.Model -> call.respondTextWriter(ContentType.Application.Json) {
                            // TODO(deepak-nulu): get prettyPrint value from URL query-param.
                            // TODO(deepak-nulu): get encodePasswords value from URL query-param.
                            // TODO(deepak-nulu): report decodeErrors once they are in aux form.
                            encodeJson(
                                getResponse.model,
                                this,
                                treeWareServer.modelMultiAuxEncoder,
                                EncodePasswords.ALL,
                                true
                            )
                        }
                        is GetResponse.ErrorList -> call.respondTextWriter(
                            ContentType.Application.Json,
                            HttpStatusCode.BadRequest
                        ) {
                            // TODO(deepak-nulu): get prettyPrint value from URL query-param.
                            writeErrorList(this, getResponse.errorList, true)
                        }
                        is GetResponse.ErrorModel -> call.respondTextWriter(
                            ContentType.Application.Json,
                            HttpStatusCode.BadRequest
                        ) {
                            // TODO(deepak-nulu): get prettyPrint value from URL query-param.
                            encodeJson(
                                getResponse.errorModel,
                                this,
                                treeWareServer.modelMultiAuxEncoder,
                                EncodePasswords.ALL,
                                true
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun snakeCaseToKebabCase(snake: String): String = snake.split("_").joinToString("-")

private fun writeStringList(writer: Writer, list: List<String>) {
    list.forEachIndexed { index, error ->
        if (index != 0) writer.appendLine()
        writer.append(error)
    }
}

private fun writeErrorList(writer: Writer, errorList: List<ElementModelError>, prettyPrint: Boolean) {
    val encoder = JsonWireFormatEncoder(writer, prettyPrint)
    encoder.encodeListStart(null)
    errorList.forEach { error ->
        encoder.encodeObjectStart(null)
        encoder.encodeStringField("path", error.path)
        encoder.encodeStringField("error", error.error)
        encoder.encodeObjectEnd()
    }
    encoder.encodeListEnd()
}