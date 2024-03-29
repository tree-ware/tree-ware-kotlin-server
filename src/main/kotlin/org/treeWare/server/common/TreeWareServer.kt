package org.treeWare.server.common

import io.ktor.server.auth.*
import okio.BufferedSource
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

/** Perform initialization before the server starts serving. */
typealias Initializer = (mainMeta: MainModel) -> Unit

/** Return the RBAC model for the logged-in user. */
typealias RbacGetter = (principal: Principal?, mainMeta: MainModel) -> MainModel?

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
    val metaModel: MainModel

    private val logger = logging()
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


        val mainMetaName = getMainMetaName(metaModel)
        logger.info { "Meta-model name: $mainMetaName" }
        logger.info { "Calling initializer" }
        initializer(metaModel)
        logger.info { "tree-ware server started" }
    }

    fun set(principal: Principal?, request: BufferedSource): SetResponse {
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
        val rbac = rbacGetter(principal, metaModel) ?: return SetResponse.ErrorList(
            ErrorCode.SERVER_ERROR,
            listOf(ElementModelError("/", "Unable to authorize the request"))
        )
        return when (val permittedSetRequest = permitSet(model, rbac)) {
            is FullyPermitted -> setter(permittedSetRequest.permitted)
            // TODO(#40): return errors that indicate which parts are not permitted
            is PartiallyPermitted -> SetResponse.ErrorList(
                ErrorCode.UNAUTHORIZED,
                listOf(ElementModelError("", "Unauthorized for some parts of the request"))
            )
            NotPermitted -> SetResponse.ErrorList(
                ErrorCode.UNAUTHORIZED,
                listOf(ElementModelError("", "Unauthorized for all parts of the request"))
            )
        }
    }

    fun get(principal: Principal?, request: BufferedSource): GetResponse {
        val (model, decodeErrors) = decodeJson(
            request,
            metaModel,
            multiAuxDecodingStateMachineFactory = modelMultiAuxDecodingStateMachineFactory
        )
        if (model == null || decodeErrors.isNotEmpty()) return GetResponse.ErrorList(
            ErrorCode.CLIENT_ERROR,
            decodeErrors.map { ElementModelError("", it) })
        populateSubTreeGranularityGetRequest(model)
        val rbac = rbacGetter(principal, metaModel) ?: return GetResponse.ErrorList(
            ErrorCode.SERVER_ERROR,
            listOf(ElementModelError("/", "Unable to authorize the request"))
        )
        return when (val permittedGetRequest = permitGet(model, rbac)) {
            is FullyPermitted -> permitGetResponse(getter(permittedGetRequest.permitted), rbac)
            // TODO(#40): return errors that indicate which parts are not permitted
            is PartiallyPermitted -> GetResponse.ErrorList(
                ErrorCode.UNAUTHORIZED,
                listOf(ElementModelError("", "Unauthorized for some parts of the request"))
            )
            NotPermitted -> GetResponse.ErrorList(
                ErrorCode.UNAUTHORIZED,
                listOf(ElementModelError("", "Unauthorized for all parts of the request"))
            )
        }
    }

    private fun permitGetResponse(getResponse: GetResponse, rbac: MainModel): GetResponse = when (getResponse) {
        is GetResponse.Model -> when (val permittedGetResponse = permitGet(getResponse.model, rbac)) {
            is FullyPermitted -> GetResponse.Model(permittedGetResponse.permitted)
            is PartiallyPermitted -> GetResponse.Model(permittedGetResponse.permitted)
            NotPermitted -> GetResponse.Model(MutableMainModel(metaModel).also { it.getOrNewRoot() })
        }
        is GetResponse.ErrorList -> getResponse
        is GetResponse.ErrorModel -> getResponse
    }
}