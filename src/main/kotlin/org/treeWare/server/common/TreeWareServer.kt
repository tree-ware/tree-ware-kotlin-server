package org.treeWare.server.common

import org.lighthousegames.logging.logging
import org.treeWare.metaModel.aux.MetaModelAuxPlugin
import org.treeWare.metaModel.getMainMetaName
import org.treeWare.metaModel.newMetaModelFromJsonFiles
import org.treeWare.model.core.MainModel
import org.treeWare.model.decoder.decodeJson
import org.treeWare.model.decoder.stateMachine.MultiAuxDecodingStateMachineFactory
import org.treeWare.model.encoder.MultiAuxEncoder
import java.io.Reader

/** Performs initialization before the server starts serving. */
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

/** Sets the model and returns errors if any. */
typealias Setter = (mainModel: MainModel) -> SetResponse?

class TreeWareServer(
    metaModelFiles: List<String>,
    logMetaModelFullNames: Boolean,
    metaModelAuxPlugins: List<MetaModelAuxPlugin>,
    modelAuxPlugins: List<MetaModelAuxPlugin>,
    initializer: Initializer,
    private val setter: Setter
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
        if (decodeErrors.isNotEmpty() || model == null) return EchoResponse.ErrorList(decodeErrors)
        return EchoResponse.Model(model)
    }

    fun set(request: Reader): SetResponse? {
        val (model, decodeErrors) = decodeJson(
            request,
            metaModel,
            multiAuxDecodingStateMachineFactory = modelMultiAuxDecodingStateMachineFactory
        )
        if (model == null) return SetResponse.ErrorList(decodeErrors)
        return setter(model)
    }
}