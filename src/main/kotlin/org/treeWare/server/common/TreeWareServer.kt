package org.treeWare.server.common

import org.apache.logging.log4j.LogManager
import org.treeWare.metaModel.aux.MetaModelAuxPlugin
import org.treeWare.metaModel.getMetaName
import org.treeWare.metaModel.getRootMeta
import org.treeWare.metaModel.newMainMetaMetaModel
import org.treeWare.metaModel.validation.validate
import org.treeWare.model.core.MainModel
import org.treeWare.model.decoder.ModelDecoderResult
import org.treeWare.model.decoder.decodeJson
import org.treeWare.model.decoder.stateMachine.MultiAuxDecodingStateMachineFactory
import org.treeWare.model.encoder.EncodePasswords
import org.treeWare.model.encoder.encodeJson
import org.treeWare.model.operator.union
import java.io.InputStreamReader
import java.io.Reader
import java.io.Writer

/** Sets the model and returns a model with "error" aux if there are errors. */
typealias Setter = (mainModel: MainModel) -> MainModel?

class TreeWareServer(
    metaModelFiles: List<String>,
    logMetaModelFullNames: Boolean,
    metaModelAuxPlugins: List<MetaModelAuxPlugin>,
    private val setter: Setter
) {
    internal val rootName: String

    private val logger = LogManager.getLogger()
    private val metaModel: MainModel
    private val hasher = null // TODO(deepak-nulu): create a hasher based on server configuration.
    private val cipher = null // TODO(deepak-nulu): get a secret key from server configuration and create a cipher.

    init {
        logger.info("Meta-model files: $metaModelFiles")
        metaModel = getMetaModel(metaModelFiles, logMetaModelFullNames, metaModelAuxPlugins)
        rootName = getMetaName(getRootMeta(metaModel))
        logger.info("Meta-model root name: $rootName")
        logger.info("tree-ware server started")
    }

    fun echo(request: Reader, response: Writer) {
        // TODO(deepak-nulu): get expectedModelType value from URL query-param.
        val (model, decodeErrors) = decodeJson(request, metaModel, "data")
        // TODO(deepak-nulu): get prettyPrint value from URL query-param.
        // TODO(deepak-nulu): get encodePasswords value from URL query-param.
        // TODO(deepak-nulu): report decodeErrors once they are in aux form.
        if (model != null) encodeJson(model, response, encodePasswords = EncodePasswords.ALL, prettyPrint = true)
    }

    fun set(request: Reader, response: Writer) {
        val (model, decodeErrors) = try {
            decodeJson(request, metaModel, "data")
        } catch (exception: Exception) {
            ModelDecoderResult(null, listOf(exception.message ?: "Exception while decoding set-request"))
        }
        // TODO(deepak-nulu): report decodeErrors once they are in aux form.
        if (model == null) return
        val errors = setter(model)
        // TODO(deepak-nulu): get prettyPrint value from URL query-param.
        if (errors != null) encodeJson(errors, response, encodePasswords = EncodePasswords.ALL, prettyPrint = true)
    }

    /** Returns a validated meta-model created from the meta-model files. */
    private fun getMetaModel(
        metaModelFiles: List<String>,
        logMetaModelFullNames: Boolean,
        metaModelAuxPlugins: List<MetaModelAuxPlugin>
    ): MainModel {
        val metaMetaModel = newMainMetaMetaModel()
        val metaModelParts = metaModelFiles.map { file ->
            val reader = ClassLoader.getSystemResourceAsStream(file)?.let { InputStreamReader(it) }
                ?: throw IllegalArgumentException("Meta-model file $file not found")
            // TODO(performance): change MultiAuxDecodingStateMachineFactory() varargs to list to avoid array copies.
            val multiAuxDecodingStateMachineFactory =
                MultiAuxDecodingStateMachineFactory(*metaModelAuxPlugins.map { it.auxName to it.auxDecodingStateMachineFactory }
                    .toTypedArray())
            val (decodedMetaModel, decodeErrors) = decodeJson(
                reader,
                metaMetaModel,
                "data",
                multiAuxDecodingStateMachineFactory = multiAuxDecodingStateMachineFactory
            )
            if (decodedMetaModel == null || decodeErrors.isNotEmpty()) {
                logErrors(decodeErrors)
                throw IllegalArgumentException("Unable to decode meta-model file $file")
            }
            decodedMetaModel
        }
        val metaModel = union(metaModelParts)
        val metaModelErrors = validate(metaModel, hasher, cipher, logMetaModelFullNames)
        if (metaModelErrors.isNotEmpty()) throw IllegalArgumentException("Meta-model has validation errors")
        metaModelAuxPlugins.forEach { plugin ->
            val pluginErrors = plugin.validate(metaModel)
            if (pluginErrors.isNotEmpty()) {
                logErrors(pluginErrors)
                throw IllegalArgumentException("Meta-model has plugin validation errors")
            }
        }
        return metaModel
    }

    private fun logErrors(errors: List<String>) = errors.forEach { logger.error(it) }
}