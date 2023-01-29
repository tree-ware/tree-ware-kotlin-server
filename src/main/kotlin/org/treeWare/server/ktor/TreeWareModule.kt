package org.treeWare.server.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Sink
import okio.buffer
import okio.sink
import okio.source
import org.treeWare.metaModel.aux.SemanticVersionError
import org.treeWare.metaModel.aux.getResolvedVersionAux
import org.treeWare.model.core.MainModel
import org.treeWare.model.encoder.EncodePasswords
import org.treeWare.model.encoder.JsonWireFormatEncoder
import org.treeWare.model.encoder.encodeJson
import org.treeWare.model.operator.ElementModelError
import org.treeWare.model.operator.ErrorCode
import org.treeWare.model.operator.get.GetResponse
import org.treeWare.model.operator.set.SetResponse
import org.treeWare.server.common.TreeWareServer
import org.treeWare.util.buffered

private const val VERSION_PATH_PARAMETER_NAME = "version"
private const val VERSION_PREFIX = "v"
private const val VERSION_PREFIX_LENGTH = VERSION_PREFIX.length

fun Application.treeWareModule(treeWareServer: TreeWareServer, vararg authenticationProviderNames: String?) {
    routing {
        authenticate(*authenticationProviderNames) {
            route("/tree-ware/api") {
                setModelRoute(treeWareServer)
                getModelRoute(treeWareServer)
            }
        }
    }
}

private fun Route.setModelRoute(treeWareServer: TreeWareServer) {
    post("set/{$VERSION_PATH_PARAMETER_NAME}") {
        val versionError = validateModelVersion(call, treeWareServer.metaModel)
        if (versionError != null) respondVersionError(call, versionError, true)
        else withContext(Dispatchers.IO) {
            val principal = call.principal<Principal>()
            val source = call.receiveStream().source().buffer()
            val setResponse = treeWareServer.set(principal, source)
            val httpStatusCode = setResponse.errorCode.toHttpStatusCode()
            when (setResponse) {
                is SetResponse.Success -> call.respond(httpStatusCode, "")
                is SetResponse.ErrorList -> call.respondOutputStream(
                    ContentType.Application.Json,
                    httpStatusCode
                ) {
                    writeErrorList(this.sink(), setResponse.errorList, true)
                }
                is SetResponse.ErrorModel -> call.respondOutputStream(
                    ContentType.Application.Json,
                    httpStatusCode
                ) {
                    // TODO(deepak-nulu): get prettyPrint value from URL query-param.
                    encodeJson(
                        setResponse.errorModel,
                        this.sink(),
                        treeWareServer.modelMultiAuxEncoder,
                        EncodePasswords.ALL,
                        true
                    )
                }
            }
        }
    }
}

private fun Route.getModelRoute(treeWareServer: TreeWareServer) {
    post("get/{$VERSION_PATH_PARAMETER_NAME}") {
        val versionError = validateModelVersion(call, treeWareServer.metaModel)
        if (versionError != null) respondVersionError(call, versionError, true)
        else withContext(Dispatchers.IO) {
            val principal = call.principal<Principal>()
            val source = call.receiveStream().source().buffer()
            val getResponse = treeWareServer.get(principal, source)
            val httpStatusCode = getResponse.errorCode.toHttpStatusCode()
            when (getResponse) {
                is GetResponse.Model -> call.respondOutputStream(
                    ContentType.Application.Json,
                    httpStatusCode
                ) {
                    // TODO(deepak-nulu): get prettyPrint value from URL query-param.
                    // TODO(deepak-nulu): get encodePasswords value from URL query-param.
                    // TODO(deepak-nulu): report decodeErrors once they are in aux form.
                    encodeJson(
                        getResponse.model,
                        this.sink(),
                        treeWareServer.modelMultiAuxEncoder,
                        EncodePasswords.ALL,
                        true
                    )
                }
                is GetResponse.ErrorList -> call.respondOutputStream(
                    ContentType.Application.Json,
                    httpStatusCode
                ) {
                    // TODO(deepak-nulu): get prettyPrint value from URL query-param.
                    writeErrorList(this.sink(), getResponse.errorList, true)
                }
                is GetResponse.ErrorModel -> call.respondOutputStream(
                    ContentType.Application.Json,
                    httpStatusCode
                ) {
                    // TODO(deepak-nulu): get prettyPrint value from URL query-param.
                    encodeJson(
                        getResponse.errorModel,
                        this.sink(),
                        treeWareServer.modelMultiAuxEncoder,
                        EncodePasswords.ALL,
                        true
                    )
                }
            }
        }
    }
}

private fun validateModelVersion(call: ApplicationCall, mainMeta: MainModel): String? {
    val version = requireNotNull(call.parameters[VERSION_PATH_PARAMETER_NAME])
    if (!version.startsWith(VERSION_PREFIX)) return "Version `$version` in URL does not start with prefix `$VERSION_PREFIX`"
    val modelSemanticVersion = version.drop(VERSION_PREFIX_LENGTH)
    val metaModelVersion = getResolvedVersionAux(mainMeta)
    return when (metaModelVersion.validateModelSemanticVersion(modelSemanticVersion)) {
        SemanticVersionError.INVALID -> "Version `$version` in URL is not a valid semantic version"
        SemanticVersionError.HIGHER_THAN_SUPPORTED -> "Version `$version` in URL is higher than supported version `$VERSION_PREFIX${metaModelVersion.semantic}`"
        null -> null
    }
}

private suspend fun respondVersionError(call: ApplicationCall, versionError: String, prettyPrint: Boolean) {
    val errors = listOf(ElementModelError("", versionError))
    call.respondOutputStream(ContentType.Application.Json, HttpStatusCode.BadRequest) {
        writeErrorList(this.sink(), errors, prettyPrint)
    }
}

private fun ErrorCode.toHttpStatusCode(): HttpStatusCode = when (this) {
    ErrorCode.OK -> HttpStatusCode.OK
    ErrorCode.UNAUTHENTICATED -> HttpStatusCode.Unauthorized
    ErrorCode.UNAUTHORIZED -> HttpStatusCode.Forbidden
    ErrorCode.CLIENT_ERROR -> HttpStatusCode.BadRequest
    ErrorCode.SERVER_ERROR -> HttpStatusCode.InternalServerError
}

private fun writeErrorList(sink: Sink, errorList: List<ElementModelError>, prettyPrint: Boolean) {
    sink.buffered().use { bufferedSink ->
        val encoder = JsonWireFormatEncoder(bufferedSink, prettyPrint)
        encoder.encodeListStart(null)
        errorList.forEach { error ->
            encoder.encodeObjectStart(null)
            encoder.encodeStringField("path", error.path)
            encoder.encodeStringField("error", error.error)
            encoder.encodeObjectEnd()
        }
        encoder.encodeListEnd()
    }
}