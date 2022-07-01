package org.treeWare.server.common

import org.lighthousegames.logging.logging
import org.treeWare.metaModel.aux.MetaModelAuxPlugin
import org.treeWare.metaModel.getMainMetaName
import org.treeWare.metaModel.newMetaModelFromJsonFiles
import org.treeWare.model.core.MainModel
import org.treeWare.model.decoder.decodeJson
import org.treeWare.model.decoder.stateMachine.MultiAuxDecodingStateMachineFactory
import org.treeWare.model.encoder.MultiAuxEncoder
import org.treeWare.model.operator.GetResponse
import org.treeWare.model.operator.permitGet
import org.treeWare.model.operator.permitSet
import org.treeWare.model.operator.validateSet
import java.io.Reader

/** Perform initialization before the server starts serving. */
typealias Initializer = (mainMeta: MainModel) -> Unit

sealed class EchoResponse {
    data class ErrorList(val errorList: List<String>) : EchoResponse()
    data class Model(val model: MainModel) : EchoResponse()
}

sealed class SetResponse {
    data class ErrorList(val errorList: List<String>) : SetResponse()

    /** A model with "error_" aux. */
    data class ErrorModel(val errorModel: MainModel) : SetResponse()
}

/** Return the RBAC model for the logged-in user. */
typealias RbacGetter = (mainMeta: MainModel) -> MainModel

/** Set the model and returns errors if any. */
typealias Setter = (mainModel: MainModel) -> SetResponse?

/** Return the requested model or errors if any. */
typealias Getter = (request: MainModel) -> GetResponse

class TreeWareServer(
    metaModelFiles: List<String>,
    logMetaModelFullNames: Boolean,
    metaModelAuxPlugins: List<MetaModelAuxPlugin>,
    modelAuxPlugins: List<MetaModelAuxPlugin>,
    initializer: Initializer,
    private val rbacGetter: RbacGetter,
    private val setter: Setter,
    private val getter: Getter
) {
    internal val mainMetaName: String

    private val logger = logging()
    private val metaModel: MainModel
    private val hasher = null // TODO(deepak-nulu): create a hasher based on server configuration.
    private val cipher = null // TODO(deepak-nulu): get a secret key from server configuration and create a cipher.

    private val modelMultiAuxDecodingStateMachineFactory: MultiAuxDecodingStateMachineFactory
    val modelMultiAuxEncoder: MultiAuxEncoder

    init {
        logger.info { "Meta-model files: $metaModelFiles" }
        metaModel = newMetaModelFromJsonFiles(
            metaModelFiles, logMetaModelFullNames, hasher, cipher, metaModelAuxPlugins, true
        ).metaModel ?: throw IllegalArgumentException("Meta-model has validation errors")

        modelMultiAuxDecodingStateMachineFactory =
            MultiAuxDecodingStateMachineFactory(*modelAuxPlugins.map { it.auxName to it.auxDecodingStateMachineFactory }
                .toTypedArray())
        modelMultiAuxEncoder =
            MultiAuxEncoder(*modelAuxPlugins.mapNotNull { plugin -> plugin.auxEncoder?.let { plugin.auxName to it } }
                .toTypedArray())


        mainMetaName = getMainMetaName(metaModel)
        logger.info { "Meta-model name: $mainMetaName" }
        logger.info { "Calling initializer" }
        initializer(metaModel)
        logger.info { "tree-ware server started" }
    }

    fun echo(request: Reader): EchoResponse {
        // TODO(deepak-nulu): get expectedModelType value from URL query-param.
        val (model, decodeErrors) = decodeJson(
            request,
            metaModel,
            multiAuxDecodingStateMachineFactory = modelMultiAuxDecodingStateMachineFactory
        )
        if (model == null || decodeErrors.isNotEmpty()) return EchoResponse.ErrorList(decodeErrors)
        return EchoResponse.Model(model)
    }

    fun set(request: Reader): SetResponse? {
        val (model, decodeErrors) = decodeJson(
            request,
            metaModel,
            multiAuxDecodingStateMachineFactory = modelMultiAuxDecodingStateMachineFactory
        )
        if (model == null || decodeErrors.isNotEmpty()) return SetResponse.ErrorList(decodeErrors)
        val validationErrors = validateSet(model)
        if (validationErrors.isNotEmpty()) return SetResponse.ErrorList(validationErrors.map { it.toString() })
        val rbac = rbacGetter(metaModel)
        // TODO(deepak-nulu): return errors that indicate which parts are not permitted
        // TODO(deepak-nulu): ErrorList (and ErrorModel) should have a type that can be mapped to an HTTP status code.
        val permitted = permitSet(model, rbac) ?: return SetResponse.ErrorList(listOf("Unauthorized"))
        return setter(permitted)
    }

    fun get(request: Reader): GetResponse {
        val (model, decodeErrors) = decodeJson(
            request,
            metaModel,
            multiAuxDecodingStateMachineFactory = modelMultiAuxDecodingStateMachineFactory
        )
        if (model == null || decodeErrors.isNotEmpty()) return GetResponse.ErrorList(decodeErrors)
        val rbac = rbacGetter(metaModel)
        // TODO(deepak-nulu): return errors that indicate which parts are not permitted
        // TODO(deepak-nulu): ErrorList (and ErrorModel) should have a type that can be mapped to an HTTP status code.
        val permitted = permitGet(model, rbac) ?: return GetResponse.ErrorList(listOf("Unauthorized"))
        return getter(permitted)
    }
}