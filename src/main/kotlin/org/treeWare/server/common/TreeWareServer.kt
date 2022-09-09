package org.treeWare.server.common

import org.lighthousegames.logging.logging
import org.treeWare.metaModel.aux.MetaModelAuxPlugin
import org.treeWare.metaModel.getMainMetaName
import org.treeWare.metaModel.newMetaModelFromJsonFiles
import org.treeWare.model.core.MainModel
import org.treeWare.model.core.MutableMainModel
import org.treeWare.model.decoder.decodeJson
import org.treeWare.model.decoder.stateMachine.MultiAuxDecodingStateMachineFactory
import org.treeWare.model.encoder.MultiAuxEncoder
import org.treeWare.model.operator.*
import org.treeWare.model.operator.get.GetResponse
import org.treeWare.model.operator.rbac.FullyPermitted
import org.treeWare.model.operator.rbac.NotPermitted
import org.treeWare.model.operator.rbac.PartiallyPermitted
import org.treeWare.model.operator.set.SetResponse
import java.io.Reader

/** Perform initialization before the server starts serving. */
typealias Initializer = (mainMeta: MainModel) -> Unit

sealed class EchoResponse(open val errorCode: ErrorCode) {
    data class Model(val model: MainModel) : EchoResponse(ErrorCode.OK)
    data class ErrorList(override val errorCode: ErrorCode, val errorList: List<String>) : EchoResponse(errorCode)
}

/** Return the RBAC model for the logged-in user. */
typealias RbacGetter = (mainMeta: MainModel) -> MainModel

/** Set the model and returns errors if any. */
typealias Setter = (mainModel: MutableMainModel) -> SetResponse

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
        if (model == null || decodeErrors.isNotEmpty()) return EchoResponse.ErrorList(
            ErrorCode.CLIENT_ERROR,
            decodeErrors
        )
        return EchoResponse.Model(model)
    }

    fun set(request: Reader): SetResponse {
        val (model, decodeErrors) = decodeJson(
            request,
            metaModel,
            multiAuxDecodingStateMachineFactory = modelMultiAuxDecodingStateMachineFactory
        )
        if (model == null || decodeErrors.isNotEmpty()) return SetResponse.ErrorList(
            ErrorCode.CLIENT_ERROR,
            decodeErrors.map { ElementModelError("", it) })
        val validationErrors = validateSet(model)
        if (validationErrors.isNotEmpty()) return SetResponse.ErrorList(ErrorCode.CLIENT_ERROR, validationErrors)
        val granularityErrors = populateSubTreeGranularityDeleteRequest(model)
        if (granularityErrors.isNotEmpty()) return SetResponse.ErrorList(ErrorCode.CLIENT_ERROR, granularityErrors)
        val rbac = rbacGetter(metaModel)
        return when (val permitResponse = permitSet(model, rbac)) {
            is FullyPermitted -> setter(permitResponse.permitted)
            // TODO(#40): return errors that indicate which parts are not permitted
            is PartiallyPermitted -> SetResponse.ErrorList(
                ErrorCode.UNAUTHORIZED,
                listOf(ElementModelError("", "Partially unauthorized"))
            )
            NotPermitted -> SetResponse.ErrorList(
                ErrorCode.UNAUTHORIZED,
                listOf(ElementModelError("", "Fully unauthorized"))
            )
        }
    }

    fun get(request: Reader): GetResponse {
        val (model, decodeErrors) = decodeJson(
            request,
            metaModel,
            multiAuxDecodingStateMachineFactory = modelMultiAuxDecodingStateMachineFactory
        )
        if (model == null || decodeErrors.isNotEmpty()) return GetResponse.ErrorList(
            ErrorCode.CLIENT_ERROR,
            decodeErrors.map { ElementModelError("", it) })
        val rbac = rbacGetter(metaModel)
        return when (val permitResponse = permitGet(model, rbac)) {
            is FullyPermitted -> getter(permitResponse.permitted)
            // TODO(#40): return errors that indicate which parts are not permitted
            is PartiallyPermitted -> GetResponse.ErrorList(
                ErrorCode.UNAUTHORIZED,
                listOf(ElementModelError("", "Partially unauthorized"))
            )
            NotPermitted -> GetResponse.ErrorList(
                ErrorCode.UNAUTHORIZED,
                listOf(ElementModelError("", "Fully unauthorized"))
            )
        }
    }
}