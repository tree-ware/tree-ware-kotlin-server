package org.treeWare.server.common

import io.ktor.server.auth.*
import okio.BufferedSource
import org.lighthousegames.logging.logging
import org.treeWare.metaModel.aux.MetaModelAuxPlugin
import org.treeWare.metaModel.getMetaModelName
import org.treeWare.metaModel.newMetaModelFromJsonFiles
import org.treeWare.model.core.EntityFactory
import org.treeWare.model.core.EntityModel
import org.treeWare.model.core.MutableEntityModel
import org.treeWare.model.decoder.decodeJsonEntity
import org.treeWare.model.decoder.stateMachine.MultiAuxDecodingStateMachineFactory
import org.treeWare.model.encoder.MultiAuxEncoder
import org.treeWare.model.operator.*
import org.treeWare.model.operator.rbac.FullyPermitted
import org.treeWare.model.operator.rbac.NotPermitted
import org.treeWare.model.operator.rbac.PartiallyPermitted

/** Perform initialization before the server starts serving. */
typealias Initializer = (metaModel: EntityModel) -> Response

/** Return the RBAC model for the logged-in user. */
typealias RbacGetter = (principal: Principal?, metaModel: EntityModel) -> EntityModel?

/** Set the model and returns errors if any. */
typealias Setter = (model: MutableEntityModel) -> Response

/** Return the requested model or errors if any. */
typealias Getter = (request: EntityModel) -> Response

class TreeWareServer(
    metaModelFiles: List<String>,
    private val rootEntityFactory: EntityFactory,
    logMetaModelFullNames: Boolean,
    metaModelAuxPlugins: List<MetaModelAuxPlugin>,
    modelAuxPlugins: List<MetaModelAuxPlugin>,
    initializer: Initializer,
    private val rbacGetter: RbacGetter,
    private val setter: Setter,
    private val getter: Getter
) {
    val metaModel: EntityModel

    private val logger = logging()
    private val hasher = null // TODO(deepak-nulu): create a hasher based on server configuration.
    private val cipher = null // TODO(deepak-nulu): get a secret key from server configuration and create a cipher.

    private val modelMultiAuxDecodingStateMachineFactory: MultiAuxDecodingStateMachineFactory
    val modelMultiAuxEncoder: MultiAuxEncoder

    init {
        logger.info { "Meta-model files: $metaModelFiles" }
        metaModel = newMetaModelFromJsonFiles(
            metaModelFiles, logMetaModelFullNames, hasher, cipher, rootEntityFactory, metaModelAuxPlugins, true
        ).metaModel ?: throw IllegalArgumentException("Meta-model has validation errors")

        modelMultiAuxDecodingStateMachineFactory =
            MultiAuxDecodingStateMachineFactory(*modelAuxPlugins.map { it.auxName to it.auxDecodingStateMachineFactory }
                .toTypedArray())
        modelMultiAuxEncoder =
            MultiAuxEncoder(*modelAuxPlugins.mapNotNull { plugin -> plugin.auxEncoder?.let { plugin.auxName to it } }
                .toTypedArray())


        val metaName = getMetaModelName(metaModel)
        logger.info { "Meta-model name: $metaName" }
        logger.info { "Calling initializer" }
        val initializerResponse = initializer(metaModel)
        if (!initializerResponse.isOk()) throw IllegalStateException("Initializer failed: $initializerResponse")
    }

    fun set(principal: Principal?, request: BufferedSource): Response {
        val setRequest = rootEntityFactory(null)
        val decodeErrors = decodeJsonEntity(
            request,
            setRequest,
            multiAuxDecodingStateMachineFactory = modelMultiAuxDecodingStateMachineFactory,
        )
        if (decodeErrors.isNotEmpty()) return Response.ErrorList(
            ErrorCode.CLIENT_ERROR,
            decodeErrors.map { ElementModelError("", it) })
        if (setRequest.isEmpty()) return Response.ErrorList(
            ErrorCode.CLIENT_ERROR,
            listOf(ElementModelError("", "Empty request"))
        )
        val validationErrors = validateSet(setRequest)
        if (validationErrors.isNotEmpty()) return Response.ErrorList(ErrorCode.CLIENT_ERROR, validationErrors)
        val granularityErrors = populateSubTreeGranularityDeleteRequest(setRequest)
        if (granularityErrors.isNotEmpty()) return Response.ErrorList(ErrorCode.CLIENT_ERROR, granularityErrors)
        val rbac = rbacGetter(principal, metaModel) ?: return Response.ErrorList(
            ErrorCode.SERVER_ERROR,
            listOf(ElementModelError("/", "Unable to authorize the request"))
        )
        return when (val permittedSetRequest = permitSet(setRequest, rbac, rootEntityFactory)) {
            is FullyPermitted -> setter(permittedSetRequest.permitted)
            // TODO(#40): return errors that indicate which parts are not permitted
            is PartiallyPermitted -> Response.ErrorList(
                ErrorCode.UNAUTHORIZED,
                listOf(ElementModelError("", "Unauthorized for some parts of the request"))
            )

            NotPermitted -> Response.ErrorList(
                ErrorCode.UNAUTHORIZED,
                listOf(ElementModelError("", "Unauthorized for all parts of the request"))
            )
        }
    }

    fun get(principal: Principal?, request: BufferedSource): Response {
        val getRequest = rootEntityFactory(null)
        val decodeErrors = decodeJsonEntity(
            request,
            getRequest,
            multiAuxDecodingStateMachineFactory = modelMultiAuxDecodingStateMachineFactory
        )
        if (decodeErrors.isNotEmpty()) return Response.ErrorList(
            ErrorCode.CLIENT_ERROR,
            decodeErrors.map { ElementModelError("", it) })
        if (getRequest.isEmpty()) return Response.ErrorList(
            ErrorCode.CLIENT_ERROR,
            listOf(ElementModelError("", "Empty request"))
        )
        populateSubTreeGranularityGetRequest(getRequest)
        val rbac = rbacGetter(principal, metaModel) ?: return Response.ErrorList(
            ErrorCode.SERVER_ERROR,
            listOf(ElementModelError("/", "Unable to authorize the request"))
        )
        return when (val permittedGetRequest = permitGet(getRequest, rbac, rootEntityFactory)) {
            is FullyPermitted -> getWithPermittedRequest(permittedGetRequest.permitted, rbac)
            // TODO(#40): return errors that indicate which parts are not permitted
            is PartiallyPermitted -> Response.ErrorList(
                ErrorCode.UNAUTHORIZED,
                listOf(ElementModelError("", "Unauthorized for some parts of the request"))
            )

            NotPermitted -> Response.ErrorList(
                ErrorCode.UNAUTHORIZED,
                listOf(ElementModelError("", "Unauthorized for all parts of the request"))
            )
        }
    }

    private fun getWithPermittedRequest(permittedGetRequest: EntityModel, rbac: EntityModel): Response {
        return when (val getterResponse = getter(permittedGetRequest)) {
            is Response.Model -> when (val permittedGetResponse =
                permitGet(getterResponse.model, rbac, rootEntityFactory)) {
                is FullyPermitted -> Response.Model(permittedGetResponse.permitted)
                is PartiallyPermitted -> Response.Model(permittedGetResponse.permitted)
                NotPermitted -> Response.Model(rootEntityFactory(null))
            }

            else -> getterResponse
        }
    }
}